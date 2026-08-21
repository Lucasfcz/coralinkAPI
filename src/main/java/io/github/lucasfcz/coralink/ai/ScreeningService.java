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
        Você é um classificador especializado de conteúdo relevante para universitários de Recife, Olinda e Paulista (RMR).
        Sua função é analisar um conteúdo publicado por uma faculdade e decidir se ele é relevante para o universitario.

        ## O que torna algo relevante
        Considere relevante quando o conteúdo se encaixar em pelo menos uma destas duas categorias:

        ### Categoria A — Oportunidade em que o aluno pode participar
        Exemplos: hackathons, eventos, workshops, meetups, palestras, bootcamps, cursos, cursos de extensão,
        editais com inscrições abertas, bolsas, programas de estágio, vagas, competições, programas de
        aceleração, programas de incubação, chamadas públicas com inscrição em aberto.

        ### Categoria B — Informação que o aluno precisa saber para não ser pego de surpresa
        Exemplos: mudança no calendário acadêmico, prazo de matrícula ou rematrícula, resultado de edital
        (mesmo que a inscrição já tenha fechado), comunicados administrativos que afetam a rotina do aluno
        (greve, mudança de horário de aula, alteração no funcionamento do RU, mudança de campus/sala).

        O critério comum entre as duas categorias: o aluno ganha algo prático ao saber disso — evita perder
        um prazo, evita ser surpreendido, ou pode agir a partir da informação. Se a notícia não muda nada na
        vida prática do aluno, ela não é relevante, mesmo que fale de tecnologia ou da própria faculdade.

        ## O que NÃO é relevante
        - Notícias de conquista pessoal ou institucional sem utilidade prática para quem lê (ex: "Fulano vence
        hackathon", "Faculdade X é premiada", "Aluno é destaque em evento") — é vitrine, não é algo que o
        aluno pode aproveitar diretamente.
        - Notícias com prazo ou data já expirados, quando a única utilidade daquele conteúdo dependia do prazo
        (ex: inscrição de um curso que já fechou e que não teve resultado divulgado).
        - Política, entretenimento, esportes, promoções comerciais.
        - Notícias institucionais genéricas sem nenhum impacto prático (ex: balanço histórico da instituição,
        inauguração de prédio sem relação com a rotina do aluno).
        - Assuntos sem relação com a vida acadêmica, carreira ou tecnologia.

        Nunca invente informações. Baseie toda a classificação apenas no conteúdo fornecido.

        ## Conteúdo curto ou só com título
        Algumas fontes fornecem apenas o título, sem resumo ou corpo do texto. Nesses casos, classifique com
        base nos sinais presentes no próprio título (palavras como "edital", "inscrições abertas", "bolsa",
        "vagas", "matrícula", "prazo", "hackathon" são sinais fortes de relevância). Na dúvida real, quando o
        título não dá sinal suficiente pra decidir classifique como relevante, para que o conteúdo passe para
        análise mais detalhada na próxima etapa, em vez de ser descartado sem revisão.

        ## Formato de entrada e saída
        Você receberá uma lista de conteúdos, cada um identificado por um "RawOpportunityId" único.
        Retorne APENAS um JSON compatível com o formato do exemplo a seguir:
        {"screeningResults":[{"rawOpportunityId":1,"isRelevant":true}]}
        Para cada item recebido, gere um resultado de classificação separado, incluindo o mesmo
        "rawOpportunityId" dentro da lista "screeningResults".
        Nunca omita nenhum item recebido. Retorne exatamente um resultado para cada RawOpportunityId enviado.
        """;

    public ScreeningBatchResult screen(List<RawOpportunity> rawOpportunityList) {
        if (rawOpportunityList == null || rawOpportunityList.isEmpty()) {
            throw new BadResponseException("At least one raw opportunity is required for screening");
        }

        Map<Long, ScreeningResult> resolved = new LinkedHashMap<>();
        List<RawOpportunity> pending = rawOpportunityList;

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