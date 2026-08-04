package io.github.lucasfcz.coralink.ai;

import io.github.lucasfcz.coralink.dto.ScreeningBatchResult;
import io.github.lucasfcz.coralink.dto.ScreeningResult;
import io.github.lucasfcz.coralink.exceptions.AiCallException;
import io.github.lucasfcz.coralink.exceptions.BadResponseException;
import io.github.lucasfcz.coralink.model.RawOpportunity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreeningService {

    private final AiClient aiClient;

    private static final int MAX_RETRIES = 3;

    private static final String SYSTEM_PROMPT = """
            Você é um classificador especializado de oportunidades de tecnologia para estudantes de Recife e Região Metropolitana.

            Sua função é analisar um conteúdo e decidir se ele representa uma oportunidade relevante para estudantes de tecnologia.

            ## Critérios de relevância

            Considere relevante APENAS quando o conteúdo divulgar uma oportunidade na qual um estudante possa participar.

            ## Exemplos

            - hackathons
            - eventos
            - workshops
            - meetups
            - palestras
            - bootcamps
            - cursos
            - minicursos
            - editais
            - bolsas
            - programas de estágio
            - vagas
            - competições
            - programas de aceleração
            - programas de incubação
            - inscrições abertas
            - chamadas públicas
            
            ## Critérios negativos

            Classifique como não relevante quando o conteúdo tratar apenas de:
            
            - sao noticias que o universitario nao possa participar
            - assuntos que não são considerados oportunidades para o universitario em geral
            - assuntos que nao podem ser considerados oportunidades para universitarios, exemplo: Henrique Foncerca vence hackthoon..., essa noticia não pode ser considerada uma oportunidade pois não é algo que o universita pode se beneficiar diretamente.
            - noticias com datas passadas
            - política
            - entretenimento
            - esportes
            - promoções comerciais
            - notícias gerais
            - assuntos sem relação com tecnologia, educação ou carreira.

            Nunca invente informações. Baseie toda a classificação apenas no conteúdo fornecido.
            ## Formato de entrada e saída

            Você receberá uma lista de oportunidades, cada uma identificada por um "RawOpportunityId" único.

            Retorne APENAS um JSON compatível com o formato do exemplo a seguir: {"screeningResults":[{"rawOpportunityId":1,"isRelevant":true"}]}.
            Para cada oportunidade recebida, gere um resultado de classificação separado, incluindo o mesmo "rawOpportunityId" dentro da lista "screeningResults".

            Nunca omita nenhuma oportunidade recebida. Retorne exatamente um resultado para cada RawOpportunityId enviado.""";

    private boolean firstRequestSent = false;

    public ScreeningBatchResult screen(List<RawOpportunity> rawOpportunityList) {
        if (rawOpportunityList == null || rawOpportunityList.isEmpty()) {
            throw new BadResponseException("At least one raw opportunity is required for screening");
        }

        Map<Long, ScreeningResult> resolved = new LinkedHashMap<>();
        List<RawOpportunity> pending = rawOpportunityList;
        firstRequestSent = false;

        for (int attempt = 1; attempt <= MAX_RETRIES && !pending.isEmpty(); attempt++) {
            for (List<RawOpportunity> batch : partition(pending)) {
                awaitRateLimit();

                ScreeningBatchResult response = sendScreeningRequest(batch);
                List<ScreeningResult> invalid = getScreenInvalidResults(response);

                response.screeningResults().stream()
                        .filter(r -> r != null && r.rawOpportunityId() != null && !invalid.contains(r))
                        .forEach(r -> resolved.put(r.rawOpportunityId(), r));
            }

            pending = pending.stream()
                    .filter(o -> !resolved.containsKey(o.getId()))
                    .toList();
        }

        if (!pending.isEmpty()) {
            List<Long> failedIds = pending.stream().map(RawOpportunity::getId).toList();
            log.error("Unable to obtain valid screening after " + MAX_RETRIES + " attempts for raw opportunity ids: {}", failedIds);
            for (Long id : failedIds) {
                pending.stream()
                        .filter(o -> Objects.equals(o.getId(), id))
                        .findFirst()
                        .ifPresent(RawOpportunity::setIsInvalid);
            }
        }

        List<ScreeningResult> finalResults = rawOpportunityList.stream()
                .map(o -> resolved.get(o.getId()))
                .toList();

        return new ScreeningBatchResult(finalResults);
    }

    private List<List<RawOpportunity>> partition(List<RawOpportunity> rawOpportunities) {
        List<List<RawOpportunity>> batches = new ArrayList<>();
        for (int i = 0; i < rawOpportunities.size(); i += 10) {
            batches.add(rawOpportunities.subList(i, Math.min(i + 10, rawOpportunities.size())));
        }
        return batches;
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
            throw new AiCallException("Interrupted while waiting between AI batch requests", e);
        }
    }

    private ScreeningBatchResult sendScreeningRequest(List<RawOpportunity> rawOpportunities) {
        String opportunitiesBlock = rawOpportunities.stream()
                .map(this::formatRawOpportunity)
                .collect(Collectors.joining("\n\n"));

        String userPrompt = """
                Analise as seguintes oportunidades
                e classifique cada uma delas.

                %s
                """.formatted(opportunitiesBlock);

        log.info("Prompt chars: {}", userPrompt.length());
        log.info("quantity of opportunities: {}", rawOpportunities.size());

        ScreeningBatchResult result = aiClient.sendPrompt(
                SYSTEM_PROMPT,
                userPrompt,
                ScreeningBatchResult.class
        );

        if (result == null || result.screeningResults() == null) {
            throw new AiCallException("AI returned no screening results");
        }

        return result;
    }

    private List<ScreeningResult> getScreenInvalidResults(ScreeningBatchResult result) {
        List<ScreeningResult> results = result.screeningResults();

        return results.stream()
                .filter(r -> r == null
                        || r.rawOpportunityId() == null
                        || r.isRelevant() == null
                        || results.stream().anyMatch(other -> other != r
                        && Objects.equals(other.rawOpportunityId(), r.rawOpportunityId())))
                .toList();
    }

    private String formatRawOpportunity(RawOpportunity rawOpportunity) {
        return """
                RawOpportunityId: %d
                Título: %s
                Resumo: %s
                """.formatted(
                rawOpportunity.getId(),
                rawOpportunity.getTitle(),
                rawOpportunity.getShortSummary()
        );
    }
}