package io.github.lucasfcz.coralink.modules.pipeline.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Dados de uma matéria bruta coletada durante a execução da pipeline")
public record RawOpportunityResponse(
        @Schema(description = "Identificador da matéria bruta", example = "101")
        Long id,

        @Schema(description = "Título original da matéria", example = "Inscrições abertas para o Hackathon 2026")
        String title,

        @Schema(description = "URL original da notícia", example = "https://ufpe.br/noticias/hackathon-2026")
        String newsUrl,

        @Schema(description = "Fonte de origem", example = "UFPE")
        String sourceName,

        @Schema(description = "Resultado da triagem pela IA (Fase 1)", example = "true")
        Boolean screenedRelevant,

        @Schema(description = "Indica se virou oportunidade na plataforma (Fase 2)", example = "true")
        Boolean becameOpportunity,

        @Schema(description = "Data e hora em que a notícia foi capturada pelo coletor")
        LocalDateTime foundAt,

        @Schema(description = "Identificador da rodada da pipeline à qual pertence", example = "1")
        Long pipelineRunId
) {
}
