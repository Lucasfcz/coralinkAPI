package io.github.lucasfcz.coralink.ai;

import io.github.lucasfcz.coralink.dto.ScreeningBatchResult;
import io.github.lucasfcz.coralink.dto.ScreeningResult;
import io.github.lucasfcz.coralink.enums.OpportunityType;
import io.github.lucasfcz.coralink.exceptions.AiCallException;
import io.github.lucasfcz.coralink.exceptions.BadResponseException;
import io.github.lucasfcz.coralink.model.RawOpportunity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


// this class is responsible for AI first call to know if the opportunity is relevant for universitaries/students
// and what is your probably type, for least the AI sends her justify for results
@Service
@RequiredArgsConstructor
public class ScreeningService {

    private final GroqClient groqClient;

    private static final int MAX_RETRIES = 3;

    // prompt for first screening of opportunities, to classify them as relevant or not and to determine their probable type
    private static final String SYSTEM_PROMPT = """
            Você é um classificador especializado de oportunidades de tecnologia para estudantes de Recife e Região Metropolitana.
            
            Sua função é analisar um conteúdo e decidir se ele representa uma oportunidade relevante para estudantes de tecnologia.
            
            ## Público-alvo
            
            Considere prioritariamente estudantes de:
            
            - ADS
            - Ciência da Computação
            - Sistemas de Informação
            - Engenharia de Software
            - Engenharia da Computação
            - áreas correlatas
            - iniciantes em programação
            
            ## Critérios de relevância
            
            Considere relevante quando o conteúdo envolver educação, carreira ou tecnologia, como:
            
            - programação
            - desenvolvimento web
            - desenvolvimento mobile
            - backend
            - frontend
            - IA
            - ciência de dados
            - banco de dados
            - cloud
            - DevOps
            - segurança
            - UX/UI
            - software livre
            - engenharia de software
            - inovação
            - empreendedorismo em tecnologia
            
            Escolha o tipo mais específico possível.
            
            Exemplos:
            
            - "Workshop de Spring Boot" → WORKSHOP
            - "Hackathon Porto Digital" → HACKATHON
            - "Programa de estágio da Accenture" → INTERNSHIP_PROGRAM
            - "Vaga para Desenvolvedor Java" → JOB_OPENING
            - "Bootcamp Santander + DIO" → BOOTCAMP
            - "Google Summer of Code" → OPEN_SOURCE
            - "AWS Cloud Practitioner gratuito" → CERTIFICATION
            - "Meetup do GDG Recife" → MEETUP
            - "Campus Party" → EVENT
            - "Edital FACEPE" → EDITAL
            
            Utilize EVENT apenas quando houver um evento tecnológico relevante que não possa ser classificado em uma categoria mais específica.
            
            Utilize OTHER somente quando o conteúdo for relevante para estudantes de tecnologia, mas não se encaixar em nenhuma das categorias acima.
            
            ## Critérios negativos
            
            Classifique como não relevante quando o conteúdo tratar apenas de:
            
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
            
            Retorne um JSON compatível com o seguinte formato: {"screeningResults":[{"rawOpportunityId":1,"isRelevant":true,"probableType":"WORKSHOP","reasoning":"..."}]}.
            Para cada oportunidade recebida, gere um resultado de classificação separado, incluindo o mesmo "rawOpportunityId" dentro da lista "screeningResults".
            
            Nunca omita nenhuma oportunidade recebida. Retorne exatamente um resultado para cada RawOpportunityId enviado.""";

    public ScreeningBatchResult screen(List<RawOpportunity> rawOpportunityList) {

        if (rawOpportunityList == null || rawOpportunityList.isEmpty()) {
            throw new BadResponseException("At least one raw opportunity is required for screening");
        }

        List<ScreeningResult> finalResults = new ArrayList<>();
        //this variable will hold the opportunities that need to be retried in case of invalid results
        List<RawOpportunity> rawOpportunities = rawOpportunityList;

        for (int attempt = 0; attempt < MAX_RETRIES && !rawOpportunities.isEmpty(); attempt++) {

            ScreeningBatchResult response = sendScreeningRequest(rawOpportunities);
            List<ScreeningResult> invalidResults = getScreenInvalidResults(response);

            finalResults.addAll(
                    response.screeningResults().stream()
                            .filter(r -> !invalidResults.contains(r))
                            .toList()
            );

            if (invalidResults.isEmpty()) {
                break;
            }

            Set<Long> invalidIds = invalidResults.stream()
                    .map(ScreeningResult::rawOpportunityId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // Filter out the opportunities that have invalid results
            rawOpportunities = rawOpportunities.stream()
                    .filter(o -> invalidIds.contains(o.getId()))
                    .toList();
        }

        if (finalResults.size() != rawOpportunityList.size()) {
            throw new AiCallException("Unable to obtain valid screening for all opportunities after retries.");
        }

        return new ScreeningBatchResult(finalResults);
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

        ScreeningBatchResult result = groqClient.sendPrompt(
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

        Set<OpportunityType> validTypes = EnumSet.allOf(OpportunityType.class);

        return result.screeningResults().stream()
                .filter(r -> r == null
                        || r.rawOpportunityId() == null
                        || r.isRelevant() == null
                        || r.probableType() == null
                        || r.reasoning() == null
                        || r.reasoning().isBlank()
                        || !validTypes.contains(r.probableType())
                        || result.screeningResults().stream().anyMatch(other -> other != r && Objects.equals(other.rawOpportunityId(), r.rawOpportunityId()))
                )
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
