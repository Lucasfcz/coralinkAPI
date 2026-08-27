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

/**
 * Serviço de Triagem (Fase 1 do Pipeline).
 * Utiliza o modelo de IA (Google Gemini) para classificar se os resumos brutos de notícias
 * coletados possuem pertinência prática (Categoria A: oportunidades ativas / Categoria B: avisos acadêmicos).
 */
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
            throw new BadResponseException("É necessária pelo menos uma oportunidade bruta para triagem");
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
            log.error("Não foi possível obter triagem válida após {} tentativas para os IDs brutos: {}", MAX_RETRIES, failedIds);
        }

        // Mapeia os resultados garantindo que elementos não classificados não gerem NullPointerException
        List<ScreeningResult> finalResults = rawOpportunityList.stream()
                .map(o -> resolved.get(o.getId()))
                .filter(Objects::nonNull)
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

    /**
     * Pausa preventiva de 20 segundos para não ultrapassar o limite de requisições por minuto (RPM)
     * da cota gratuita da API do Google Gemini (15 RPM), prevenindo falhas de rate limit HTTP 429.
     */
    private void awaitRateLimit() {
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiCallException("Thread interrompida durante espera entre requisições em lote à IA", e);
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

        log.info("Caracteres do prompt: {}", userPrompt.length());
        log.info("Quantidade de oportunidades no lote: {}", rawOpportunities.size());

        ScreeningBatchResult result = aiClient.sendPrompt(
                SYSTEM_PROMPT,
                userPrompt,
                ScreeningBatchResult.class
        );

        if (result == null || result.screeningResults() == null) {
            throw new AiCallException("A IA não retornou resultados de triagem");
        }

        return result;
    }

    private List<ScreeningResult> getScreenInvalidResults(ScreeningBatchResult result) {
        List<ScreeningResult> results = result.screeningResults();
        if (results == null) return List.of();

        Set<Long> seenIds = new HashSet<>();
        Set<Long> duplicateIds = new HashSet<>();
        for (ScreeningResult r : results) {
            if (r != null && r.rawOpportunityId() != null) {
                if (!seenIds.add(r.rawOpportunityId())) {
                    duplicateIds.add(r.rawOpportunityId());
                }
            }
        }

        return results.stream()
                .filter(r -> r == null
                        || r.rawOpportunityId() == null
                        || r.isRelevant() == null
                        || duplicateIds.contains(r.rawOpportunityId()))
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