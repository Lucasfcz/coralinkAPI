package io.github.lucasfcz.coralink.modules.admin.dto;

/**
 * Record utilitário para receber a projeção direta da query de agregação condicional do funil de IA.
 */
public record RawFunnelMetrics(
        long totalRaw,
        long screenedRelevant,
        long screenedIrrelevant
) {
}
