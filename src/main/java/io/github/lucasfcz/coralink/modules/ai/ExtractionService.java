package io.github.lucasfcz.coralink.modules.ai;

import io.github.lucasfcz.coralink.modules.sources.dto.DetailedContent;
import io.github.lucasfcz.coralink.modules.ai.dto.ExtractionBatchResult;
import io.github.lucasfcz.coralink.modules.ai.dto.ExtractionResult;
import io.github.lucasfcz.coralink.infra.exception.AiCallException;
import io.github.lucasfcz.coralink.infra.exception.BadResponseException;
import io.github.lucasfcz.coralink.modules.pipeline.model.RawOpportunity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Serviço de Extração (Fase 2 do Pipeline).
 * Envia o conteúdo detalhado de oportunidades triadas positivamente para o modelo de IA (Google Gemini)
 * para extrair campos estruturados (datas, público-alvo, modalidade, gratuidade, etc.).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractionService {

    private final AiClient aiClient;

    @org.springframework.beans.factory.annotation.Value("${coralink.ai.rate-limit-delay-ms:6000}")
    private long rateLimitDelayMs;

    private static final int MAX_RETRIES_PER_ITEM = 3;

    private static final String SYSTEM_PROMPT = """
        Você é um especialista em extração e estruturação de oportunidades acadêmicas e profissionais para estudantes universitários.
        Sua missão é extrair com rigor e fidelidade as informações de uma oportunidade com base exclusivamente no texto fornecido.

        Retorne somente dados presentes no texto. Nunca invente informações, datas, locais, requisitos, valores ou benefícios.
        Caso a oportunidade não mencione taxa, cobrança ou pagamento para participar, considere isFree = true.
        Use null quando uma informação não estiver disponível (exceto isFree, que deve ser boolean).
        confidenceScore deve estar entre 0.0 e 1.0.
        Preserve exatamente o rawOpportunityId recebido.

        ## Regras Críticas para o Summary (Resumo da Oportunidade)
        O summary deve permitir ao universitário bater o olho no card e saber de imediato se a oportunidade é relevante e vantajosa para ele, sem ter que ler páginas inteiras de editais ou notícias.
        - Escreva um parágrafo conciso, fluido e denso em informações práticas (entre 3 e 5 frases).
        - O resumo DEVE cobrir obrigatoriamente (quando as informações existirem no texto):
          1. O que é: definição clara e específica da oportunidade (ex: "Minicurso prático sobre arquitetura limpa em Java...", "Edital de Iniciação Científica em Inteligência Artificial...", "Hackathon de 48 horas focado em soluções sustentáveis...").
          2. Para quem é / Requisitos: público-alvo prioritário, cursos, períodos ou pré-requisitos necessários (ex: "Voltado para alunos de cursos de Computação a partir do 3º período que possuam base em POO...").
          3. Benefícios e Recompensas: o que o estudante ganha ao participar (ex: "Oferece bolsa mensal de R$ 700 e certificado de 80h complementares", "Premiação de R$ 10.000 para a equipe vencedora e possibilidade de contratação").
          4. Prazos e Logística: prazos limites de inscrição, forma de se inscrever, datas de realização e formato/local (ex: "Inscrições abertas até 25 de outubro via formulário online. O evento ocorrerá presencialmente no auditório da instituição de 10 a 12 de novembro.").
        - O que NÃO fazer no summary:
          - NUNCA use frases introdutórias vazias ou clichês ("Esta notícia informa sobre...", "A instituição anunciou um edital...", "Veja abaixo os detalhes...").
          - NÃO se limite a repetir apenas o título.
          - Vá direto ao ponto com linguagem objetiva, profissional e rica em dados concretos.

        ## Regras para Enums de OpportunityType
        Classifique o campo 'type' estritamente em um dos seguintes 15 valores:
        - EVENT: palestras, conferências, congressos, simpósios, meetups, feiras de carreira e encontros de networking.
        - WORKSHOP: oficinas práticas mão na massa, minicursos técnicos aplicados e treinamentos intensivos.
        - COURSE: cursos livres, bootcamps de programação, certificações técnicas e cursos de capacitação extracurricular.
        - GRADUATION: processos seletivos para cursos formais de graduação (bacharelado, licenciatura, tecnólogo), pós-graduação, mestrado ou cursos técnicos.
        - HACKATHON: hackathons, maratonas de desenvolvimento/programação e desafios de ideação/inovação.
        - COMPETITION: olimpíadas científicas ou acadêmicas, desafios de programação competitiva e competições acadêmicas.
        - INTERNSHIP: vagas e programas de estágio, programas de trainee e vagas de emprego para estudantes/júnior.
        - SCHOLARSHIP: bolsas de estudo, auxílios financeiros de permanência e editais de assistência estudantil.
        - RESEARCH: oportunidades de iniciação científica (PIBIC/PIBITI), atuação em laboratórios de pesquisa e monitoria acadêmica.
        - EXCHANGE_PROGRAM: programas de intercâmbio e mobilidade acadêmica nacional ou internacional.
        - VOLUNTEERING: projetos comunitários, voluntariado universitário e iniciativas sociais com chamada aberta.
        - EXTENSION_PROGRAM: programas e projetos de extensão universitária, ligas acadêmicas e grupos de estudo curriculares/extracurriculares.
        - INNOVATION: programas de inovação aberta, ideação, desafios tecnológicos, aceleração, pré-incubação e editais de fomento a startups/spin-offs acadêmicas (ex: Centelha, Tecnova, programas do Porto Digital e FACEPE).
        - NOTICE: comunicados oficiais, avisos regulatórios, calendários acadêmicos e informes institucionais de relevância estudantil.
        - OTHER: qualquer outra oportunidade de participação ativa que não se enquadre nas categorias acima.

        NUNCA invente novos enums de OpportunityType. Se nenhum se encaixar perfeitamente, use OTHER.

        ## Regras para Thematic Area
        Identifique a área temática principal da oportunidade baseando-se no conteúdo (ex: "Desenvolvimento Web", "Inteligência Artificial", "Cibersegurança", "Banco de Dados", "Engenharia de Software", "Ciência de Dados", "Inovação"). Se for algo amplo ou não específico, use "GERAL".

        ## Regras Críticas para TargetCourseAudience
        Classifique o público-alvo estritamente usando apenas os seguintes enums válidos, agrupados por área:
        - Tecnologia: ADS, COMPUTER_SCIENCE, SOFTWARE_ENGINEERING, INFORMATION_SYSTEMS, COMPUTER_ENGINEERING, DATA_SCIENCE, TECHNOLOGY_STUDENTS
        - Saúde: MEDICINE, NURSING, PHARMACY, PHYSICAL_THERAPY, PSYCHOLOGY, PHYSICAL_EDUCATION, DENTISTRY, BIOMEDICINE, NUTRITION, VETERINARY_MEDICINE, AESTHETICS, HEALTH_STUDENTS
        - Negócios e Gestão: BUSINESS_ADMINISTRATION, ACCOUNTING, ECONOMICS, BUSINESS_STUDENTS
        - Comunicação e Design: DESIGN, GRAPHIC_DESIGN, MARKETING, ADVERTISING, JOURNALISM
        - Engenharias (não-computação): CIVIL_ENGINEERING, ELECTRICAL_ENGINEERING, MECHANICAL_ENGINEERING, PRODUCTION_ENGINEERING, CHEMICAL_ENGINEERING, ENGINEERING_STUDENTS
        - Ciências Exatas: MATHEMATICS, STATISTICS, PHYSICS, CHEMISTRY, EXACT_SCIENCES_STUDENTS
        - Direito e Humanas: LAW, PEDAGOGY, SOCIAL_WORK, LANGUAGE_AND_LITERATURE, HUMANITIES_STUDENTS
        - Outras Aplicadas: ARCHITECTURE_AND_URBANISM, TOURISM_AND_HOSPITALITY, GASTRONOMY
        - Público Universal: UNIVERSITY_STUDENTS

        DIRETRIZES DE CLASSIFICAÇÃO PRECISA:
        1. Rigor e Isolamento de Área: O público-alvo deve ser cirúrgico e condizente com o escopo da oportunidade. Nunca misture cursos de áreas não correlatas.
           - Se a oportunidade for de Tecnologia, utilize APENAS enums do grupo de Tecnologia (ex: ADS, COMPUTER_SCIENCE, SOFTWARE_ENGINEERING, INFORMATION_SYSTEMS, COMPUTER_ENGINEERING, DATA_SCIENCE e o geral TECHNOLOGY_STUDENTS). Jamais inclua cursos de Saúde, Direito ou Negócios.
           - Se a oportunidade for da área de Saúde (medicina, enfermagem, farmácia, etc.), utilize APENAS os cursos específicos de Saúde aplicáveis e o geral HEALTH_STUDENTS. Jamais inclua enums de Tecnologia ou outras áreas.
           - Se for de Negócios/Administração, Direito, Engenharias ou Comunicação, aplique estritamente o mesmo princípio de isolamento para a sua respectiva área.
        2. Regra para UNIVERSITY_STUDENTS:
           - Inclua UNIVERSITY_STUDENTS APENAS E EXCLUSIVAMENTE se o edital/oportunidade for expressamente aberto e relevante a estudantes universitários de qualquer curso ou área de graduação, sem qualquer restrição técnica, temática ou de formação (ex: edital geral de bolsas de permanência, feira de carreiras multissetorial, seminário de boas-vindas).
           - NUNCA inclua UNIVERSITY_STUDENTS em oportunidades com foco temático específico (como um hackathon de IA, congresso de fisioterapia ou curso de programação), para garantir que os filtros dos estudantes por área exibam somente oportunidades relevantes para seu curso.

        ## Regras para Modality
        Classifique em: ONLINE, IN_PERSON ou HYBRID.

        ## Regras para isForAll
        - isForAll = true: aberta ao público geral / estudantes de qualquer faculdade.
        - isForAll = false: restrita exclusivamente a alunos matriculados na própria faculdade promotora.

        ## Regras para imageUrl
        - Extraia a URL da imagem de banner/cartaz/capa da oportunidade no conteúdo (inclusive em markdown ![...](url) ou tags html).
        - Se não houver imagem de divulgação da oportunidade, use null.

        ## Regras para Datas (Formato ISO yyyy-MM-dd)
        - startDate: data de início do evento/curso/atividade. Para eventos de 1 dia, startDate == endDate.
        - endDate: data de encerramento do evento/curso/atividade.
        - registrationDeadline: prazo final para inscrições/submissões. Se não informado, use null.

        ## Formato de Saída (JSON Estrito)
        Retorne APENAS um único objeto JSON no seguinte formato:
        {
          "rawOpportunityId": 123,
          "summary": "Minicurso prático e intensivo de desenvolvimento de microsserviços em Java e Spring Boot oferecido pelo CIn/UFPE. Destinado a estudantes de cursos de Computação a partir do 3º período que já tenham conhecimento intermediário de orientação a objetos. O minicurso é gratuito, concede certificado de 20 horas de atividades complementares e inclui desafios práticos com mentoria de especialistas do mercado. Inscrições abertas até 25 de setembro pelo formulário online da instituição, com aulas presenciais realizadas de 01 a 05 de outubro.",
          "type": "WORKSHOP",
          "thematicArea": "Backend",
          "targetCourseAudiences": ["ADS", "COMPUTER_SCIENCE", "SOFTWARE_ENGINEERING", "INFORMATION_SYSTEMS", "TECHNOLOGY_STUDENTS"],
          "modality": "IN_PERSON",
          "startDate": "2026-10-01",
          "endDate": "2026-10-05",
          "registrationDeadline": "2026-09-25",
          "location": "Centro de Informática - UFPE, Recife",
          "isFree": true,
          "isForAll": true,
          "imageUrl": "https://example.com/banner.png",
          "confidenceScore": 0.95
        }
        """;

    public ExtractionBatchResult extract(List<RawOpportunity> rawOpportunities, Map<Long, DetailedContent> contentsById) {
        if (rawOpportunities == null || rawOpportunities.isEmpty()) {
            throw new BadResponseException("É necessária pelo menos uma oportunidade bruta para extração");
        }
        if (rawOpportunities.stream().anyMatch(o -> !Boolean.TRUE.equals(o.getScreenedRelevant()))) {
            throw new BadResponseException("Apenas oportunidades triadas como relevantes podem ser extraídas");
        }

        List<ExtractionResult> results = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();

        for (RawOpportunity rawOpportunity : rawOpportunities) {
            DetailedContent content = contentsById.get(rawOpportunity.getId());

            // Não interrompe o processamento caso um conteúdo detalhado esteja ausente ou vazio; avança para os próximos itens
            if (rawOpportunity.getId() == null || content == null || content.fullContent() == null || content.fullContent().isBlank()) {
                log.warn("Ignorando a RawOpportunity {} — ID ausente ou conteúdo detalhado vazio", rawOpportunity.getId());
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
            log.error("Não foi possível extrair {} oportunidades após {} tentativas cada. IDs: {}", failedIds.size(), MAX_RETRIES_PER_ITEM, failedIds);
        }

        return new ExtractionBatchResult(results);
    }

    private ExtractionResult extractWithRetries(RawOpportunity rawOpportunity, DetailedContent content) {
        for (int attempt = 1; attempt <= MAX_RETRIES_PER_ITEM; attempt++) {
            awaitRateLimit();
            try {
                ExtractionResult result = sendExtractionRequest(rawOpportunity, content);

                if (isInvalid(result) || !Objects.equals(result.rawOpportunityId(), rawOpportunity.getId())) {
                    log.warn("Resultado de extração inválido para a oportunidade bruta {} na tentativa {}/{}", rawOpportunity.getId(), attempt, MAX_RETRIES_PER_ITEM);
                    continue;
                }

                return result;

            } catch (RuntimeException e) {
                log.warn("Falha na chamada de extração da oportunidade bruta {} na tentativa {}/{}: {}", rawOpportunity.getId(), attempt, MAX_RETRIES_PER_ITEM, e.getMessage());
            }
        }
        return null;
    }

    /**
     * Pausa preventiva para não ultrapassar o limite de requisições por minuto (RPM)
     * da cota da API do Google Gemini, prevenindo falhas de rate limit HTTP 429.
     */
    private void awaitRateLimit() {
        try {
            Thread.sleep(rateLimitDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiCallException("Thread interrompida durante espera entre requisições à IA", e);
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
            throw new AiCallException("A IA não retornou resultado de extração");
        }

        return response;
    }

    private boolean isInvalid(ExtractionResult r) {
        return r == null
                || r.rawOpportunityId() == null
                || r.summary() == null || r.summary().isBlank()
                || r.type() == null
                || r.thematicArea() == null || r.thematicArea().isBlank()
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