package io.github.lucasfcz.coralink.modules.admin.dto;

import java.util.Map;

/**
 * Agregação de métricas do sistema para alimentar gráficos e indicadores do painel administrativo.
 */
public record DashboardMetricsResponse(
        long totalActiveOpportunities,
        long totalRawCollected,
        long totalScreenedRelevant,
        long totalScreenedIrrelevant,
        double aiScreeningAcceptanceRate,
        Map<String, Long> opportunitiesByType,
        Map<String, Long> opportunitiesBySource,
        long pendingUserSuggestions
) {
}
