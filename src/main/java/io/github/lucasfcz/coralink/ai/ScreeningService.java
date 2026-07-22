package io.github.lucasfcz.coralink.ai;

import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.dto.ScreeningResult;
import io.github.lucasfcz.coralink.model.RawOpportunity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScreeningService {

    private final GroqClient groqClient;

    private final String systemPrompt = "Você é um classificador especializado de oportunidades de tecnologia para estudantes de Recife e Região Metropolitana.\n" +
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
            "## Classificação\n" +
            "\n" +
            "Sempre classifique a oportunidade em exatamente um dos seguintes tipos:\n" +
            "\n" +
            "- HACKATHON\n" +
            "- COMPETITION\n" +
            "- EDITAL\n" +
            "- COURSE\n" +
            "- WORKSHOP\n" +
            "- LECTURE\n" +
            "- BOOTCAMP\n" +
            "- CERTIFICATION\n" +
            "- INTERNSHIP_PROGRAM\n" +
            "- JOB_OPENING\n" +
            "- TRAINEE_PROGRAM\n" +
            "- SCHOLARSHIP\n" +
            "- MEETUP\n" +
            "- NETWORKING\n" +
            "- OPEN_SOURCE\n" +
            "- EVENT\n" +
            "- OTHER\n" +
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
            "- política\n" +
            "- entretenimento\n" +
            "- esportes\n" +
            "- promoções comerciais\n" +
            "- notícias gerais\n" +
            "- assuntos sem relação com tecnologia, educação ou carreira.\n" +
            "\n" +
            "Nunca invente informações. Baseie toda a classificação apenas no conteúdo fornecido." +
            "Enums para utlizados no sistema: " +
            "Enum -> ThematicArea: \n" +
            "    BACKEND,\n" +
            "    FRONTEND,\n" +
            "    DATA_SCIENCE,\n" +
            "    ARTIFICIAL_INTELLIGENCE,\n" +
            "    CLOUD_INFRA,\n" +
            "    CYBERSECURITY,\n" +
            "    UX_UI,\n" +
            "    MOBILE,\n" +
            "    CAREER_EMPLOYABILITY,\n" +
            "    ENTREPRENEURSHIP,\n" +
            "    GENERAL_TECH" +
            "Enum -> OpportunityType: \n" +
            "    EVENT,\n" +
            "    HACKATHON,\n" +
            "    EDITAL,\n" +
            "    BOOTCAMP,\n" +
            "    CERTIFICATION,\n" +
            "    COURSE,\n" +
            "    TRAINEE_PROGRAM,\n" +
            "    INTERNSHIP_PROGRAM,\n" +
            "    SCHOLARSHIP,\n" +
            "    JOB_OPENING,\n" +
            "    COMPETITION,\n" +
            "    LECTURE,\n" +
            "    NETWORKING,\n" +
            "    WORKSHOP,\n" +
            "    MEETUP,\n" +
            "    OPEN_SOURCE,\n" +
            "    OTHER" +
            "Enum -> Modality: " +
            "    IN_PERSON,\n" +
            "    ONLINE,\n" +
            "    HYBRID" +
            "Enum -> TargetAudience: " +
            "    UNDERGRAD_TECH_STUDENT,\n" +
            "    BEGINNER,\n" +
            "    INTERNSHIP_SEEKER,\n" +
            "    JOB_SEEKER,\n" +
            "    GENERAL_TECH_COMMUNITY";

    public ScreeningResult screen(NewsSummary news) {

        String userPrompt = """
                Analise a seguinte oportunidade.

                Título:
                %s

                Resumo:
                %s
                """.formatted(
                news.title(),
                news.shortSummary()
        );

        return groqClient.sendStructuredPrompt(
                systemPrompt,
                userPrompt,
                ScreeningResult.class
        );
    }
}
