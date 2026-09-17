package io.github.lucasfcz.coralink.modules.pipeline.repository;

import io.github.lucasfcz.coralink.modules.admin.dto.RawFunnelMetrics;
import io.github.lucasfcz.coralink.modules.pipeline.model.RawOpportunity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface RawOpportunityRepository extends JpaRepository<RawOpportunity, Long> {

    List<RawOpportunity> findAllByNewsUrlIn(Collection<String> newsUrls);

    List<RawOpportunity> findByScreenedRelevantIsTrueAndBecameOpportunityIsFalse();

    List<RawOpportunity> findByScreenedRelevantIsTrueAndBecameOpportunityIsFalseAndExtractionAttemptsLessThan(int maxAttempts);

    Page<RawOpportunity> findByScreenedRelevantIsTrueAndBecameOpportunityIsFalseAndExtractionAttemptsGreaterThanEqualOrderByFoundAtDesc(int minAttempts, Pageable pageable);

    long countByScreenedRelevantIsTrueAndBecameOpportunityIsFalseAndExtractionAttemptsGreaterThanEqual(int minAttempts);

    List<RawOpportunity> findByScreenedRelevantIsNull();

    Page<RawOpportunity> findByPipelineRunId(Long pipelineRunId, Pageable pageable);

    Page<RawOpportunity> findAllByOrderByFoundAtDesc(Pageable pageable);

    List<RawOpportunity> findByPipelineRunIdIsNull();

    /**
     * Consulta única de agregação condicional no PostgreSQL para calcular todas as métricas do funil de IA
     * em um único roundtrip ao banco, eliminando consultas N+1 e múltiplas viagens de rede.
     */
    @Query("""
        SELECT new io.github.lucasfcz.coralink.modules.admin.dto.RawFunnelMetrics(
            COUNT(r),
            COALESCE(SUM(CASE WHEN r.screenedRelevant = true THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN r.screenedRelevant = false THEN 1L ELSE 0L END), 0L)
        )
        FROM RawOpportunity r
    """)
    RawFunnelMetrics getFunnelMetrics();
}
