package io.github.lucasfcz.coralink.ai;

import io.github.lucasfcz.coralink.dto.ScreeningBatchResult;
import io.github.lucasfcz.coralink.dto.ScreeningResult;
import io.github.lucasfcz.coralink.exceptions.AiCallException;
import io.github.lucasfcz.coralink.model.RawOpportunity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


// this class is responsible for AI first call to know if the opportunity is relevant for universitaries/students
// and what is your probably type, for least the AI sends her justify for results
@Service
@RequiredArgsConstructor
public class ScreeningService {

    private final GroqClient groqClient;

    // prompt for first screening of opportunities, to classify them as relevant or not and to determine their probably type
    private static final String SYSTEM_PROMPT = "Você é um classificador especializado de oportunidades de tecnologia para estudantes de Recife e Região Metropolitana.\n" +
            "\n" +
            "Sua função é analisar um conteúdo e decidir se ele representa uma oportunidade relevante para estudantes de tecnologia.\n" +
            "\n" +
            "## Público-alvo\n" +
            "\n" +
            "Considere prioritariamente estudantes de:\n" +
            "\n" +
            "- ADS\n" +
            "- Ciência da Computação\n" +
            "- Sistemas de Informação\n" +
            "- Engenharia de Software\n" +
            "- Engenharia da Computação\n" +
            "- áreas correlatas\n" +
            "- iniciantes em programação\n" +
            "\n" +
            "## Critérios de relevância\n" +
            "\n" +
            "Considere relevante quando o conteúdo envolver educação, carreira ou tecnologia, como:\n" +
            "\n" +
            "- programação\n" +
            "- desenvolvimento web\n" +
            "- desenvolvimento mobile\n" +
            "- backend\n" +
            "- frontend\n" +
            "- IA\n" +
            "- ciência de dados\n" +
            "- banco de dados\n" +
            "- cloud\n" +
            "- DevOps\n" +
            "- segurança\n" +
            "- UX/UI\n" +
            "- software livre\n" +
            "- engenharia de software\n" +
            "- inovação\n" +
            "- empreendedorismo em tecnologia\n" +
            "\n" +
            "Escolha o tipo mais específico possível.\n" +
            "\n" +
            "Exemplos:\n" +
            "\n" +
            "- \"Workshop de Spring Boot\" → WORKSHOP\n" +
            "- \"Hackathon Porto Digital\" → HACKATHON\n" +
            "- \"Programa de estágio da Accenture\" → INTERNSHIP_PROGRAM\n" +
            "- \"Vaga para Desenvolvedor Java\" → JOB_OPENING\n" +
            "- \"Bootcamp Santander + DIO\" → BOOTCAMP\n" +
            "- \"Google Summer of Code\" → OPEN_SOURCE\n" +
            "- \"AWS Cloud Practitioner gratuito\" → CERTIFICATION\n" +
            "- \"Meetup do GDG Recife\" → MEETUP\n" +
            "- \"Campus Party\" → EVENT\n" +
            "- \"Edital FACEPE\" → EDITAL\n" +
            "\n" +
            "Utilize EVENT apenas quando houver um evento tecnológico relevante que não possa ser classificado em uma categoria mais específica.\n" +
            "\n" +
            "Utilize OTHER somente quando o conteúdo for relevante para estudantes de tecnologia, mas não se encaixar em nenhuma das categorias acima.\n" +
            "\n" +
            "## Critérios negativos\n" +
            "\n" +
            "Classifique como não relevante quando o conteúdo tratar apenas de:\n" +
            "\n" +
            "- assuntos que não são considerados oportunidades para o universitario em geral\n" +
            "- assuntos que nao podem ser considerados oportunidades para universitarios, exemplo: Henrique Foncerca vence hackthoon..., essa noticia não pode ser considerada uma oportunidade pois não é algo que o universita pode se beneficiar diretamente.\n" +
            "- noticias com datas passadas\n" +
            "- política\n" +
            "- entretenimento\n" +
            "- esportes\n" +
            "- promoções comerciais\n" +
            "- notícias gerais\n" +
            "- assuntos sem relação com tecnologia, educação ou carreira.\n" +
            "\n" +
            "Nunca invente informações. Baseie toda a classificação apenas no conteúdo fornecido.\n"
            +
            "## Formato de entrada e saída\n" +
            "\n" +
            "Você receberá uma lista de oportunidades, cada uma identificada por um \"RawOpportunityId\" único.\n" +
            "\n" +
            "Retorne um JSON compatível com o seguinte formato: {\"screeningResults\":[{\"rawOpportunityId\":1,\"isRelevant\":true,\"probablyType\":\"WORKSHOP\",\"reasoning\":\"...\"}]}.\n" +
            "Para cada oportunidade recebida, gere um resultado de classificação separado, incluindo o mesmo \"rawOpportunityId\" dentro da lista \"screeningResults\".\n" +
            "\n" +
            "Nunca omita nenhuma oportunidade recebida. Retorne exatamente um resultado para cada RawOpportunityId enviado.";

    public ScreeningBatchResult screen(List<RawOpportunity> rawOpportunities) {
        if (rawOpportunities == null || rawOpportunities.isEmpty()) {
            throw new IllegalArgumentException("At least one raw opportunity is required for screening");
        }

        String opportunitiesBlock = rawOpportunities.stream()
                .map(this::formatRawOpportunity)
                .collect(Collectors.joining("\n\n"));

        String userPrompt = """
                Analise as seguintes oportunidades
                e classifique cada uma delas. %s
           """.formatted(opportunitiesBlock);

        ScreeningBatchResult result = groqClient.sendPrompt(
                SYSTEM_PROMPT,
                userPrompt,
                ScreeningBatchResult.class);
        validateResponse(rawOpportunities, result);
        return result;
    }

    private void validateResponse(List<RawOpportunity> rawOpportunities, ScreeningBatchResult result) {
        if (rawOpportunities.stream().anyMatch(opportunity -> opportunity.getId() == null)) {
            throw new IllegalArgumentException("Raw opportunities must be persisted before screening");
        }
        if (result == null || result.screeningResults() == null) {
            throw new AiCallException("AI returned no screening results");
        }

        Set<Long> expectedIds = rawOpportunities.stream().map(RawOpportunity::getId).collect(Collectors.toSet());
        Set<Long> returnedIds = result.screeningResults().stream()
                .map(ScreeningResult::rawOpportunityId)
                .collect(Collectors.toSet());

        boolean containsNull = result.screeningResults().stream().anyMatch(item -> item == null
                || item.rawOpportunityId() == null
                || item.probablyType() == null
                || item.reasoning() == null
                || item.reasoning().isBlank());
        if (containsNull || result.screeningResults().size() != rawOpportunities.size() || !returnedIds.equals(expectedIds)) {
            throw new AiCallException("AI returned an invalid or incomplete screening result");
        }
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
