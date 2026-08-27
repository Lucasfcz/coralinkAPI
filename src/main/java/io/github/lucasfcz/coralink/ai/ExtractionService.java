package io.github.lucasfcz.coralink.ai;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.ExtractionBatchResult;
import io.github.lucasfcz.coralink.dto.ExtractionResult;
import io.github.lucasfcz.coralink.exceptions.AiCallException;
import io.github.lucasfcz.coralink.exceptions.BadResponseException;
import io.github.lucasfcz.coralink.model.RawOpportunity;
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
        
        Regras para o summary:
        - Ele deve conter todas as informacoes mais importantes para o universitario nao precisar ler um pdf enorme para entender a noticia.
        - Ele nao deve conter informacoes inuteis para o estudante.

        Regras para classificação geral da oportunidade:
        - Leia o conteudo da oportunidade sua missao é classificar a oportunidade de acordo com os itens a seguir: summary, type, thematicArea, targetCourseAudiences,
        modality, startDate, endDate, registrationDeadline, location, isFree e confidenceScore. seja preciso na classificação
        - O summary deve conter um breve resumo sobre do que a oportunidade se trata para o estudante quando ir para a pagina da oportunidade nao ter que ler 10 paginas para entende-la
        - NÃO crie campos adicionais ou campos inexistentes.
        - Caso a oportunidade nao se refira em nenhum momento a dinheiro, taxa ou pagar para ter acesso, considere que eh uma oportunidade gratuita ou seja isFree = true.

        Regras para Thematic Area:
        - Voce deve colocar qual a area tematica da noticia ou informacao com base no conteudo da noticia, exemplo nos cursos de tecnologia existem diversas areas como backend, devops, fullStack... cada curso tem sua area tematica, entao voce deve colocar a area tematica da noticia com base no conteudo da noticia, caso nao consiga identificar ou caso a noticia seja geral area tematica coloque GERAL.
        
        Regras para Enums:
        - NUNCA crie novos enums utilize apenas os que estao presentes nas classes: OpportunityType, Modality, TargetCourseAudience
        - Caso nao acredite que nenhum dos enums presentes nas classes acima se encaixe perfeitamente na oportunidade, considere como OTHER em OpportunityType e UNIVERSITY_STUDENTS em TargetCourseAudience.
        - Para o modality veja se a oportunidade acontece presencialmente, online ou hibrido e classifique de acordo com a classe Modality usando os enums: ONLINE, IN_PERSON ou HYBRID.

        Regras para o isForAll:
        - ele se diz respeito a oportunidades exclusivas para estudantes da propria faculdade, caso seja aberto ao publico geral considere isForAll = true, caso seja exclusivo para os estudantes da faculdade considere isForAll = false;

        Regras para imageUrl:
        - Extraia a URL da imagem principal da oportunidade (banner, cartaz ou capa do evento/noticia) presente no texto/conteúdo fornecido (inclusive se estiver em formato markdown ![...](url) ou links de imagem).
        - Caso o conteúdo não possua imagem ou não haja imagem condizente com a oportunidade, use null.
        
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

        Você receberá exatamente UMA oportunidade por vez. Retorne um único objeto JSON, seguindo exatamente este formato(use como exemplo/base):

        {
          "rawOpportunityId": 123,
          "summary": "Resumo da oportunidade",
          "type": "COURSE",
          "thematicArea": "Desenvolvimento Web",
          "targetCourseAudiences": ["ADS", "COMPUTER_SCIENCE", "SOFTWARE_ENGINEERING", "INFORMATION_SYSTEMS", "COMPUTER_ENGINEERING", "TECHNOLOGY_STUDENTS"],
          "modality": "ONLINE",
          "startDate": "2026-08-03",
          "endDate": "2026-08-07",
          "registrationDeadline": "2026-07-31",
          "location": "Centro do Recife",
          "isFree": true,
          "isForAll": false,
          "imageUrl": "https://example.com/banner.png",
          "confidenceScore": 0.95
        }

        Não escreva nada além do JSON.
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
                log.warn("Ignorando oportunidade bruta {} — ID ausente ou conteúdo detalhado vazio", rawOpportunity.getId());
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
     * Pausa preventiva de 20 segundos para não ultrapassar o limite de requisições por minuto (RPM)
     * da cota gratuita da API do Google Gemini (15 RPM), prevenindo falhas de rate limit HTTP 429.
     */
    private void awaitRateLimit() {
        try {
            Thread.sleep(20000);
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