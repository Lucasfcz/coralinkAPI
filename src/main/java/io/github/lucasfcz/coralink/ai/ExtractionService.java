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

@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractionService {

    private final AiClient aiClient;

    private static final int MAX_RETRIES_PER_ITEM = 3;

    private static final String SYSTEM_PROMPT = """
        Você extrai oportunidades de tecnologia para estudantes a partir de notícias completas.

        Retorne somente dados presentes no texto.
        Nunca invente datas, local, tipos de oportunidades, area tematica, cursos, modalidade ou preço.
        Caso a oportunidade nao se refira em nenhum momento a taxa, dinheiro ou pagar para ter acesso,
        considere que eh uma oportunidade gratuita ou seja isFree = true.
        Use null quando uma informação não estiver disponível(excluindo o campo isFree).

        confidenceScore deve estar entre 0.0 e 1.0.
        Preserve exatamente o rawOpportunityId recebido.

        Regras para classificação geral da oportunidade:
        - Leia o conteudo da oportunidade sua missao é classificar a oportunidade de acordo com os itens a seguir: summary, type, thematicAreas, targetCourseAudiences,
        modality, startDate, endDate, registrationDeadline, location, isFree e confidenceScore. seja preciso na classificação
        - O summary deve conter um breve resumo sobre do que a oportunidade se trata para o estudante quando ir para a pagina da oportunidade nao ter que ler 10 paginas para entende-la
        - NÃO crie campos adicionais ou campos inexistentes.
        - Caso a oportunidade nao se refira em nenhum momento a dinheiro, taxa ou pagar para ter acesso, considere que eh uma oportunidade gratuita ou seja isFree = true.

        Regras para Enums:
        - NUNCA crie novos enums utilize apenas os que estao presentes nas classes: OpportunityType, Modality, ThematicArea, TargetCourseAudience
        - Caso nao acredite que nenhum dos enums presentes nas classes acima se encaixe perfeitamente na oportunidade, considere como OTHER em OpportunityType, GENERAL em ThematicArea e STUDENTS_IN_GENERAL em TargetCourseAudience.
        - Para o modality veja se a oportunidade acontece presencialmente, online ou hibrido e classifique de acordo com a classe Modality usando os enums: ONLINE, IN_PERSON ou HYBRID.

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

        Você receberá exatamente UMA oportunidade por vez. Retorne um único objeto JSON
        (não um array, não envolva em uma lista), seguindo exatamente este formato:

        {
          "rawOpportunityId": 123,
          "summary": "Resumo da oportunidade",
          "type": "COURSE",
          "thematicAreas": ["WEB_DEVELOPMENT", "DATA_SCIENCE"],
          "targetCourseAudiences": ["COMPUTER_SCIENCE", "STUDENTS_IN_GENERAL"],
          "modality": "ONLINE",
          "startDate": "2026-08-03",
          "endDate": "2026-08-07",
          "registrationDeadline": "2026-07-31",
          "location": "Centro do Recife",
          "isFree": true,
          "confidenceScore": 0.95
        }

        Não escreva nada além do JSON.
        """;

    private boolean firstRequestSent = false;

    public ExtractionBatchResult extract(List<RawOpportunity> rawOpportunities, Map<Long, DetailedContent> contentsById) {
        if (rawOpportunities == null || rawOpportunities.isEmpty()) {
            throw new BadResponseException("At least one raw opportunity is required for extraction");
        }
        if (rawOpportunities.stream().anyMatch(o -> !Boolean.TRUE.equals(o.getScreenedRelevant()))) {
            throw new BadResponseException("Only opportunities screened as relevant can be extracted");
        }

        List<ExtractionResult> results = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();
        firstRequestSent = false;

        for (RawOpportunity rawOpportunity : rawOpportunities) {
            DetailedContent content = contentsById.get(rawOpportunity.getId());

            if (rawOpportunity.getId() == null || content == null || content.fullContent() == null || content.fullContent().isBlank()) {
                log.warn("Skipping raw opportunity {} — missing id or detailed content", rawOpportunity.getId());
                failedIds.add(rawOpportunity.getId());
                continue;
            }

            ExtractionResult result = extractWithRetries(rawOpportunity, content);
            if (result != null) {
                results.add(result);
            } else {
                failedIds.add(rawOpportunity.getId());
            }
        }

        if (!failedIds.isEmpty()) {
            log.error("Unable to extract {} opportunities after {} attempts each. Ids: {}", failedIds.size(), MAX_RETRIES_PER_ITEM, failedIds);
            for (Long id : failedIds) {
                rawOpportunities.stream()
                        .filter(o -> Objects.equals(o.getId(), id))
                        .findFirst()
                        .ifPresent(RawOpportunity::setIsInvalid);
            }
        }

        return new ExtractionBatchResult(results);
    }

    private ExtractionResult extractWithRetries(RawOpportunity rawOpportunity, DetailedContent content) {
        for (int attempt = 1; attempt <= MAX_RETRIES_PER_ITEM; attempt++) {
            awaitRateLimit();

            try {
                ExtractionResult result = sendExtractionRequest(rawOpportunity, content);

                if (isInvalid(result) || !Objects.equals(result.rawOpportunityId(), rawOpportunity.getId())) {
                    log.warn("Invalid extraction result for raw opportunity {} on attempt {}/{}",
                            rawOpportunity.getId(), attempt, MAX_RETRIES_PER_ITEM);
                    continue;
                }

                return result;

            } catch (RuntimeException e) {
                log.warn("Extraction call failed for raw opportunity {} on attempt {}/{}",
                        rawOpportunity.getId(), attempt, MAX_RETRIES_PER_ITEM, e);
            }
        }
        return null;
    }

    private void awaitRateLimit() {
        if (!firstRequestSent) {
            firstRequestSent = true;
            return;
        }
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiCallException("Interrupted while waiting between AI requests", e);
        }
    }

    private ExtractionResult sendExtractionRequest(RawOpportunity rawOpportunity, DetailedContent content) {
        String prompt = format(rawOpportunity, content);

        ExtractionResult response = aiClient.sendPrompt(
                SYSTEM_PROMPT,
                "Extraia a oportunidade abaixo:\n\n" + prompt,
                ExtractionResult.class
        );

        if (response == null) {
            throw new AiCallException("AI returned no extraction result");
        }

        return response;
    }

    private boolean isInvalid(ExtractionResult r) {
        return r == null
                || r.rawOpportunityId() == null
                || r.summary() == null || r.summary().isBlank()
                || r.type() == null
                || r.thematicAreas() == null
                || r.targetCourseAudiences() == null
                || r.confidenceScore() == null
                || r.confidenceScore() < 0
                || r.confidenceScore() > 1;
    }

    private String format(RawOpportunity rawOpportunity, DetailedContent content) {
        String fullContent = content.fullContent();
        String boundedContent = fullContent.length() > 15000
                ? fullContent.substring(0, 15000) + "\n[conteúdo truncado por limite de contexto]"
                : fullContent;

        return """
                RawOpportunityId: %d
                Título: %s
                URL: %s
                Conteúdo: %s
                """.formatted(rawOpportunity.getId(), rawOpportunity.getTitle(), rawOpportunity.getNewsUrl(), boundedContent);
    }
}