package io.github.lucasfcz.coralink.modules.admin;

import io.github.lucasfcz.coralink.modules.admin.dto.DashboardMetricsResponse;
import io.github.lucasfcz.coralink.modules.admin.dto.RawFunnelMetrics;
import io.github.lucasfcz.coralink.modules.opportunity.repository.OpportunityRepository;
import io.github.lucasfcz.coralink.modules.pipeline.repository.RawOpportunityRepository;
import io.github.lucasfcz.coralink.modules.userhelp.repository.UserHelpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço de regras de negócio exclusivo do painel administrativo.
 * Consolida métricas com consultas diretas no banco de dados, eliminando consultas N+1.
 */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final OpportunityRepository opportunityRepository;
    private final RawOpportunityRepository rawOpportunityRepository;
    private final UserHelpRepository userHelpRepository;
    private final io.github.lucasfcz.coralink.modules.pipeline.RawOpportunityMapper rawOpportunityMapper;

    @Transactional(readOnly = true)
    public DashboardMetricsResponse getDashboardMetrics() {
        long totalActive = opportunityRepository.countActiveOpportunities();

        // Consulta única e agregada no PostgreSQL
        RawFunnelMetrics funnel = rawOpportunityRepository.getFunnelMetrics();
        long rawCount = funnel.totalRaw();
        long screenedRelevant = funnel.screenedRelevant();
        long screenedIrrelevant = funnel.screenedIrrelevant();

        double acceptanceRate = calculateAcceptanceRate(screenedRelevant, screenedIrrelevant);
        Map<String, Long> byType = mapGroupedCount(opportunityRepository.countActiveGroupedByType());
        Map<String, Long> bySource = mapGroupedCount(opportunityRepository.countActiveGroupedBySource());
        long pendingSuggestions = userHelpRepository.count();
        long failedExtractions = rawOpportunityRepository.countByScreenedRelevantIsTrueAndBecameOpportunityIsFalseAndExtractionAttemptsGreaterThanEqual(3);

        return new DashboardMetricsResponse(
                totalActive,
                rawCount,
                screenedRelevant,
                screenedIrrelevant,
                acceptanceRate,
                byType,
                bySource,
                pendingSuggestions,
                failedExtractions
        );
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<io.github.lucasfcz.coralink.modules.pipeline.dto.RawOpportunityResponse> getFailedExtractions(org.springframework.data.domain.Pageable pageable) {
        return rawOpportunityRepository.findByScreenedRelevantIsTrueAndBecameOpportunityIsFalseAndExtractionAttemptsGreaterThanEqualOrderByFoundAtDesc(3, pageable)
                .map(rawOpportunityMapper::toResponse);
    }

    private double calculateAcceptanceRate(long screenedRelevant, long screenedIrrelevant) {
        long totalScreened = screenedRelevant + screenedIrrelevant;
        if (totalScreened == 0) {
            return 0.0;
        }
        return Math.round(((double) screenedRelevant / totalScreened) * 1000.0) / 10.0;
    }

    private Map<String, Long> mapGroupedCount(List<Object[]> rows) {
        Map<String, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            if (row != null && row.length >= 2 && row[0] != null) {
                result.put(row[0].toString(), (Long) row[1]);
            }
        }
        return result;
    }
}
