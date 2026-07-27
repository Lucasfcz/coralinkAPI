package io.github.lucasfcz.coralink.ai;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.ExtractionBatchResult;
import io.github.lucasfcz.coralink.dto.ExtractionResult;
import io.github.lucasfcz.coralink.exceptions.AiCallException;
import io.github.lucasfcz.coralink.model.RawOpportunity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExtractionService {

    private static final String SYSTEM_PROMPT = """
            Você extrai oportunidades de tecnologia para estudantes a partir de notícias completas.
            Retorne somente dados presentes no texto. Não invente datas, local, modalidade ou preço: use null para campos desconhecidos.
            confidenceScore deve estar entre 0.0 e 1.0. Para cada item, preserve exatamente o rawOpportunityId recebido.
            Retorne JSON com extractionResults, contendo rawOpportunityId, summary, type, thematicAreas, targetAudiences,
            modality, eventDate, registrationDeadline, location, isFree e confidenceScore. Nunca omita um item.
            """;

    private final GroqClient groqClient;

    @Value("${coralink.ai.extraction.max-prompt-characters:50000}")
    private int maxPromptCharacters;

    public ExtractionBatchResult extract(List<RawOpportunity> items, Map<Long, DetailedContent> contentsById) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("At least one raw opportunity is required for extraction");
        }
        List<ExtractionResult> results = new ArrayList<>();
        for (List<RawOpportunity> batch : partition(items, contentsById)) {
            String prompt = batch.stream().map(item -> format(item, contentsById.get(item.getId())))
                    .collect(Collectors.joining("\n\n"));
            ExtractionBatchResult response = groqClient.sendPrompt(
                    SYSTEM_PROMPT, "Extraia as oportunidades abaixo:\n\n" + prompt, ExtractionBatchResult.class);
            validate(batch, response);
            results.addAll(response.extractionResults());
        }
        return new ExtractionBatchResult(results);
    }

    private List<List<RawOpportunity>> partition(List<RawOpportunity> items, Map<Long, DetailedContent> contentsById) {
        List<List<RawOpportunity>> batches = new ArrayList<>();
        List<RawOpportunity> current = new ArrayList<>();
        int currentSize = 0;
        for (RawOpportunity item : items) {
            DetailedContent content = contentsById.get(item.getId());
            if (item.getId() == null || content == null || content.fullContent() == null || content.fullContent().isBlank()) {
                throw new IllegalArgumentException("Every item must have a persisted id and non-empty detailed content");
            }
            int itemSize = format(item, content).length();
            if (!current.isEmpty() && currentSize + itemSize > maxPromptCharacters) {
                batches.add(List.copyOf(current));
                current.clear();
                currentSize = 0;
            }
            current.add(item);
            currentSize += itemSize;
        }
        if (!current.isEmpty()) {
            batches.add(List.copyOf(current));
        }
        return batches;
    }

    private void validate(List<RawOpportunity> batch, ExtractionBatchResult response) {
        if (response == null || response.extractionResults() == null) {
            throw new AiCallException("AI returned no extraction results");
        }

        List<ExtractionResult> results = response.extractionResults();

        if (results.stream().anyMatch(Objects::isNull)) {
            throw new AiCallException("AI returned a null extraction result within the batch");
        }

        if (results.stream().anyMatch(this::hasInvalidFields)) {
            throw new AiCallException("AI returned an extraction result with missing or invalid fields");
        }

        Set<Long> expectedIds = batch.stream().map(RawOpportunity::getId).collect(Collectors.toSet());
        Set<Long> returnedIds = results.stream().map(ExtractionResult::rawOpportunityId).collect(Collectors.toSet());

        if (results.size() != batch.size() || !expectedIds.equals(returnedIds)) {
            throw new AiCallException("AI returned a different set of items than the batch sent");
        }
    }

    private boolean hasInvalidFields(ExtractionResult result) {
        return result.rawOpportunityId() == null
                || result.summary() == null || result.summary().isBlank()
                || result.type() == null
                || result.thematicAreas() == null
                || result.targetAudiences() == null
                || result.confidenceScore() == null
                || result.confidenceScore() < 0
                || result.confidenceScore() > 1;
    }

    private String format(RawOpportunity item, DetailedContent content) {
        int contentLimit = Math.max(1_000, maxPromptCharacters - 1_000);
        String fullContent = content.fullContent();
        String boundedContent = fullContent.length() > contentLimit
                ? fullContent.substring(0, contentLimit) + "\n[conteúdo truncado por limite de contexto]"
                : fullContent;

        return """
                RawOpportunityId: %d
                Título: %s
                URL: %s
                Conteúdo: %s
                """
                .formatted(item.getId(), item.getTitle(), item.getNewsUrl(), boundedContent);
    }
}
