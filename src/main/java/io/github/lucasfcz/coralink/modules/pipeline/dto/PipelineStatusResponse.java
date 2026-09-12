package io.github.lucasfcz.coralink.modules.pipeline.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Informações em tempo real sobre o agendador e status do pipeline para o painel de administração.
 */
@Schema(description = "Status em tempo real da esteira de scraping e IA")
public record PipelineStatusResponse(
        @Schema(description = "Indica se o pipeline está executando no momento", example = "false")
        boolean isRunning,

        @Schema(description = "Data e hora de início do último ciclo")
        Instant lastRunStartedAt,

        @Schema(description = "Data e hora de término do último ciclo")
        Instant lastRunFinishedAt,

        @Schema(description = "Estimativa de início do próximo ciclo agendado")
        Instant nextRunEstimatedAt,

        @Schema(description = "Segundos restantes até o próximo ciclo", example = "43200")
        Long secondsUntilNextRun,

        @Schema(description = "Contagem regressiva amigável", example = "11h 59m 50s")
        String formattedTimeRemaining,

        @Schema(description = "Dados da última execução realizada")
        PipelineRunResponse lastRun
) {
}
