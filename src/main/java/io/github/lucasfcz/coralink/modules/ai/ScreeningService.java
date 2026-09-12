package io.github.lucasfcz.coralink.modules.ai;

import io.github.lucasfcz.coralink.modules.ai.dto.ScreeningBatchResult;
import io.github.lucasfcz.coralink.modules.ai.dto.ScreeningResult;
import io.github.lucasfcz.coralink.infra.exception.AiCallException;
import io.github.lucasfcz.coralink.infra.exception.BadResponseException;
import io.github.lucasfcz.coralink.modules.pipeline.model.RawOpportunity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço de Triagem (Fase 1 do Pipeline).
 * Utiliza o modelo de IA (Google Gemini) para classificar se os resumos brutos de notícias
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScreeningService {

    private final AiClient aiClient;

    private static final int MAX_RETRIES = 3;

    private static final String SYSTEM_PROMPT = """
        Você é um classificador especializado de oportunidades para universitários da Região Metropolitana do Recife (RMR).
        Sua função é analisar conteúdos publicados por faculdades, instituições de ensino e hubs de tecnologia,
        e decidir com precisão se o conteúdo representa uma OPORTUNIDADE ACIONÁVEL para o estudante universitário participar.

        ## O que é RELEVANTE (isRelevant = true)
        Considere relevante APENAS conteúdos que representem uma oportunidade real, acionável e aberta
        na qual o estudante possa se inscrever, concorrer ou participar ativamente, tais como:
        1. Eventos e Networking: palestras, conferências, congressos, simpósios, meetups, feiras de carreira.
        2. Workshops e Oficinas: minicursos práticos, oficinas mão na massa, treinamentos técnicos intensivos.
        3. Cursos e Capacitação: bootcamps, cursos livres, cursos de extensão, programas de formação em tecnologia/carreira.
        4. Formação Acadêmica: processos seletivos para cursos de graduação, pós-graduação, mestrado, cursos técnicos.
        5. Hackathons e Maratonas: desafios de inovação, maratonas de programação e ideação.
        6. Competições Acadêmicas: olimpíadas científicas, desafios de programação, competições de robótica/matemática.
        7. Carreira e Mercado: programas de estágio, vagas de trainee, vagas de emprego para estudantes/júnior.
        8. Bolsas e Fomento: editais abertos com bolsas de estudo, auxílios de permanência ou fomento a projetos.
        9. Pesquisa e Academia: chamadas abertas para iniciação científica (PIBIC/PIBITI), laboratórios de pesquisa, monitoria.
        10. Mobilidade e Extensão: intercâmbios estudantis, mobilidade acadêmica, projetos de extensão e voluntariado com inscrições abertas.

        Critério fundamental: o estudante tem uma ação concreta de participação ou inscrição disponível (Call-to-Action).

        ## O que NÃO É RELEVANTE (isRelevant = false) — REJEIÇÃO ESTRITA
        Rejeite categoricamente conteúdos puramente informativos, jornalísticos ou administrativos que não oferecem inscrição/participação ativa:
        1. Comunicados administrativos e rotina: avisos de calendário acadêmico geral, datas de matrícula ou rematrícula regular da faculdade, horários de aulas, funcionamento do Restaurante Universitário (RU), bibliotecas, mudanças de salas, greves, paralisações.
        2. Notícias institucionais e burocráticas: notas de falecimento/pesar, eleições de reitoria/colegiado/sindicato, reformas ou inauguração de prédios/espaços, balanços de gestão, portarias, novas diretrizes institucionais.
        3. Notícias acadêmicas e artigos informativos: artigos de opinião, reportagens sobre pesquisas já concluídas, coberturas de acontecimentos passados, entrevistas de professores, notícias institucionais em geral.
        4. Resultados de editais e divulgações fechadas: listas de aprovados, homologação de resultados finais, convocações de editais cujas inscrições já encerraram.
        5. Vitrine e homenagens a terceiros: notícias comemorativas como "Aluno da instituição ganha prêmio", "Faculdade celebra aniversário", "Professor é homenageado" — são apenas vitrines, sem oportunidade para quem lê.
        6. Notícias com prazos de inscrição manifestamente expirados.
        7. Política, esportes gerais, entretenimento ou promoções comerciais sem foco em carreira/aprendizado universitário.

        Nunca invente informações. Baseie a classificação estritamente no texto fornecido.

        ## Conteúdo curto ou só com título
        Quando a fonte fornecer apenas o título ou poucas palavras:
        - Palavras como "inscrições abertas", "edital de bolsa", "vaga de estágio", "hackathon", "workshop", "curso", "olimpíada", "iniciação científica", "processo seletivo" são sinais fortes de relevância (isRelevant = true).
        - Termos como "calendário", "nota de pesar", "resultado final", "comunicado", "eleição", "recesso", "posse" são sinais fortes de rejeição (isRelevant = false).
        - Em caso de dúvida real em que o título sugira uma oportunidade mas não detalhe prazos, marque como relevante (true) para análise na fase de extração.

        ## Formato de entrada e saída
        Você receberá uma lista de conteúdos, cada um identificado por um "RawOpportunityId" único.
        Retorne APENAS um JSON estritamente compatível com o formato:
        {"screeningResults":[{"rawOpportunityId":1,"isRelevant":true}]}
        Para cada item recebido, gere exatamente um resultado correspondente na lista "screeningResults".
        Nunca omita nenhum item recebido.
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