package io.github.lucasfcz.coralink.ai;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.ExtractionBatchResult;
import io.github.lucasfcz.coralink.dto.ExtractionResult;
import io.github.lucasfcz.coralink.exceptions.AiCallException;
import io.github.lucasfcz.coralink.exceptions.BadResponseException;
import io.github.lucasfcz.coralink.model.RawOpportunity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractionService {

    private static final int MAX_RETRIES = 3;

    private static final String SYSTEM_PROMPT = """
        Você extrai oportunidades de tecnologia para estudantes a partir de notícias completas.

        Retorne somente dados presentes no texto.
        Nunca invente datas, local, modalidade ou preço.
        Caso a oportunidade nao se refira em nenhum momento a taxa, dinheiro ou pagar para ter acesso,
        considere que eh uma oportunidade gratuita ou seja isFree = true.
        Use null quando uma informação não estiver disponível(excluindo o campo isFree).

        confidenceScore deve estar entre 0.0 e 1.0.
        Preserve exatamente o rawOpportunityId recebido.
        
        Retorne JSON contendo extractionResults com:
        rawOpportunityId, summary, type, thematicAreas, targetAudiences,
        modality, startDate, endDate, registrationDeadline,
        location, isFree e confidenceScore.

        Regras para datas:
        - Use o formato ISO yyyy-MM-dd.
        - Para eventos de um único dia, startDate e endDate devem ser iguais.
        - Para eventos com duração ou período, startDate deve ser o primeiro dia
          e endDate deve ser o último dia.
        - Nunca retorne intervalos em uma única string.
        
        Exemplo:
        "Curso acontece de 03/08/2026 até 07/08/2026"
        deve retornar:
        {
          "startDate": "2026-08-03",
          "endDate": "2026-08-07"
        }

        Nunca omita itens recebidos.
        """;
    private final AiClient aiClient;

    @Value("${coralink.ai.extraction.max-prompt-characters:50000}")
    private int maxPromptCharacters;

    public ExtractionBatchResult extract(List<RawOpportunity> rawOpportunities, Map<Long, DetailedContent> contentsById) {
        if (rawOpportunities == null || rawOpportunities.isEmpty()) {
            throw new BadResponseException("At least one raw opportunity is required for extraction");
        }
        if (rawOpportunities.stream().anyMatch(o -> !Boolean.TRUE.equals(o.getScreenedRelevant()))) {
            throw new BadResponseException("Only opportunities screened as relevant can be extracted");
        }

        Map<Long, ExtractionResult> resolved = new LinkedHashMap<>();
        List<RawOpportunity> pending = rawOpportunities;

        for (int attempt = 1; attempt <= MAX_RETRIES && !pending.isEmpty(); attempt++) {
            for (List<RawOpportunity> batch : partition(pending, contentsById)) {
                ExtractionBatchResult response = sendExtractionRequest(batch, contentsById);
                List<ExtractionResult> invalid = getInvalidExtractionResults(response);

                response.extractionResults().stream()
                        .filter(r -> r != null && r.rawOpportunityId() != null && !invalid.contains(r))
                        .forEach(r -> resolved.put(r.rawOpportunityId(), r));
            }

            pending = pending.stream()
                    .filter(o -> !resolved.containsKey(o.getId()))
                    .toList();
        }

        if (!pending.isEmpty()) {
            List<Long> failedIds = pending.stream().map(RawOpportunity::getId).toList();
            throw new AiCallException("Unable to extract all opportunities after " + MAX_RETRIES
                    + " attempts for raw opportunity ids: " + failedIds);
        }

        List<ExtractionResult> finalResults = rawOpportunities.stream()
                .map(o -> resolved.get(o.getId()))
                .toList();

        return new ExtractionBatchResult(finalResults);
    }

    private ExtractionBatchResult sendExtractionRequest(List<RawOpportunity> batch, Map<Long, DetailedContent> contentsById) {
        String prompt = batch.stream()
                .map(item -> format(item, contentsById.get(item.getId())))
                .collect(Collectors.joining("\n\n"));

        ExtractionBatchResult response = aiClient.sendPrompt(
                SYSTEM_PROMPT,
                "Extraia as oportunidades abaixo:\n\n" + prompt,
                ExtractionBatchResult.class
        );

        if (response == null || response.extractionResults() == null) {
            throw new AiCallException("AI returned no extraction results");
        }

        return response;
    }

    private List<ExtractionResult> getInvalidExtractionResults(ExtractionBatchResult response) {
        List<ExtractionResult> results = response.extractionResults();

        return results.stream()
                .filter(r -> r == null
                        || r.rawOpportunityId() == null
                        || r.summary() == null || r.summary().isBlank()
                        || r.type() == null
                        || r.thematicAreas() == null
                        || r.targetAudiences() == null
                        || r.confidenceScore() == null
                        || r.confidenceScore() < 0
                        || r.confidenceScore() > 1
                        || results.stream().anyMatch(other -> other != r
                        && Objects.equals(other.rawOpportunityId(), r.rawOpportunityId())))
                .toList();
    }

    private List<List<RawOpportunity>> partition(List<RawOpportunity> rawOpportunities, Map<Long, DetailedContent> contentsById) {
        List<List<RawOpportunity>> batches = new ArrayList<>();
        List<RawOpportunity> current = new ArrayList<>();
        int currentSize = 0;

        for (RawOpportunity rawOpportunity : rawOpportunities) {
            DetailedContent content = contentsById.get(rawOpportunity.getId());

            // caso seja invalido, pula para outra oportunidade
            if (rawOpportunity.getId() == null || content == null || content.fullContent() == null || content.fullContent().isBlank()) {
                log.warn("Skipping raw opportunity {} — missing id or detailed content", rawOpportunity.getId());
                continue;
            }

            int rawOpportunitySize = format(rawOpportunity, content).length();
            if (!current.isEmpty() && currentSize + rawOpportunitySize > maxPromptCharacters) {
                batches.add(List.copyOf(current));
                current.clear();
                currentSize = 0;
            }
            current.add(rawOpportunity);
            currentSize += rawOpportunitySize;
        }
        if (!current.isEmpty()) {
            batches.add(List.copyOf(current));
        }
        return batches;
    }

    private String format(RawOpportunity rawOpportunity, DetailedContent content) {
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
                """.formatted(rawOpportunity.getId(), rawOpportunity.getTitle(), rawOpportunity.getNewsUrl(), boundedContent);
    }
}