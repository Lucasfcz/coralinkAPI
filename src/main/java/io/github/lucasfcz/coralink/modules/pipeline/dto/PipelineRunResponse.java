package io.github.lucasfcz.coralink.modules.pipeline.dto;

import io.github.lucasfcz.coralink.modules.pipeline.enums.PipelineStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Dados resumidos de uma execução do pipeline de coleta e extração")
public record PipelineRunResponse(
        @Schema(description = "Identificador da execução", example = "1")
        Long id,

        @Schema(description = "Data e hora de início", example = "2026-09-10T12:00:00Z")
        Instant startedAt,

        @Schema(description = "Data e hora de término", example = "2026-09-10T12:05:00Z")
        Instant finishedAt,

        @Schema(description = "Status final da execução", example = "SUCCESS")
        PipelineStatus status,

        @Schema(description = "Total de matérias brutas coletadas", example = "45")
        int collectedCount,

        @Schema(description = "Total de matérias aprovadas na triagem de IA", example = "20")
        int screeningRelevantCount,

        @Schema(description = "Total de oportunidades extraídas e publicadas com sucesso", example = "18")
        int extractionRelevantCount,

        @Schema(description = "Total de falhas/erros durante a execução", example = "2")
        int failuresCount,

        @Schema(description = "Duração total em milissegundos", example = "300000")
        Long durationMs,

        @Schema(description = "Mensagem de erro (em caso de status FAILED)")
        String errorMessage
) {
}
