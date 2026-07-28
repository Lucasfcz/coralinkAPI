package io.github.lucasfcz.coralink.ai;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.ExtractionBatchResult;
import io.github.lucasfcz.coralink.dto.ExtractionResult;
import io.github.lucasfcz.coralink.exceptions.AiCallException;
import io.github.lucasfcz.coralink.exceptions.BadResponseException;
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

    private static final int MAX_RETRIES = 3;

    @Value("${coralink.ai.extraction.max-prompt-characters:50000}")
    private int maxPromptCharacters;

    public ExtractionBatchResult extract(List<RawOpportunity> rawOpportunities, Map<Long, DetailedContent> contentsById) {

        if (rawOpportunities == null || rawOpportunities.isEmpty()) {
            throw new BadResponseException("At least one raw opportunity is required for extraction");
        }
        if (rawOpportunities.stream().anyMatch(rawOpportunity -> rawOpportunity.getScreenedRelevant() == null)) {
            throw new AiCallException("Needs to pass throw screening first before extraction");
        }

        List<ExtractionResult> finalResults = new ArrayList<>();
        List<RawOpportunity> pending = rawOpportunities;

        for (int attempt = 0; attempt < MAX_RETRIES && !pending.isEmpty(); attempt++) {

            List<RawOpportunity> retriedItems = pending.stream()
                    .filter(item -> item.getIsRelevant() == null)
                    .toList();

            if (retriedItems.isEmpty()) {
                break;
            }

            List<ExtractionResult> attemptResults = new ArrayList<>();

            for (List<RawOpportunity> batch : partition(retriedItems, contentsById)) {

                ExtractionBatchResult response = sendExtractionRequest(batch, contentsById);

                List<ExtractionResult> invalidResults =
                        getInvalidExtractionResults(response);

                attemptResults.addAll(
                        response.extractionResults().stream()
                                .filter(r -> !invalidResults.contains(r))
                                .toList()
                );
            }

            finalResults.addAll(attemptResults);

            Set<Long> processedIds = attemptResults.stream()
                    .map(ExtractionResult::rawOpportunityId)
                    .collect(Collectors.toSet());

            pending = pending.stream()
                    .filter(item -> !processedIds.contains(item.getId()))
                    .toList();
        }

        if (finalResults.size() != rawOpportunities.size()) {
            throw new AiCallException("Unable to extract all opportunities after retries.");
        }

        return new ExtractionBatchResult(finalResults);
    }

    private ExtractionBatchResult sendExtractionRequest(
            List<RawOpportunity> batch,
            Map<Long, DetailedContent> contentsById) {

        String prompt = batch.stream()
                .map(item -> format(item, contentsById.get(item.getId())))
                .collect(Collectors.joining("\n\n"));

        ExtractionBatchResult response = groqClient.sendPrompt(
                SYSTEM_PROMPT,
                "Extraia as oportunidades abaixo:\n\n" + prompt,
                ExtractionBatchResult.class
        );

        if (response == null || response.extractionResults() == null) {
            throw new AiCallException("AI returned no extraction results");
        }

        return response;
    }

    private List<ExtractionResult> getInvalidExtractionResults(
            ExtractionBatchResult response) {

        return response.extractionResults().stream()
                .filter(r ->
                        r == null
                                || r.rawOpportunityId() == null
                                || r.summary() == null
                                || r.summary().isBlank()
                                || r.type() == null
                                || r.thematicAreas() == null
                                || r.targetAudiences() == null
                                || r.confidenceScore() == null
                                || r.confidenceScore() < 0
                                || r.confidenceScore() > 1
                )
                .toList();
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
