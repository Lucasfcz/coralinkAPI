package io.github.lucasfcz.coralink.modules.pipeline.model;

import io.github.lucasfcz.coralink.modules.pipeline.enums.PipelineStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Entidade de auditoria e observabilidade que registra cada execução do pipeline agendado ou disparado manualmente.
 */
@Entity
@Table(name = "pipeline_runs")
@Getter
@Setter
@NoArgsConstructor
public class PipelineRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PipelineStatus status;

    @Column(name = "collected_count", nullable = false)
    private int collectedCount = 0;

    @Column(name = "screening_relevant_count", nullable = false)
    private int screeningRelevantCount = 0;

    @Column(name = "extraction_relevant_count", nullable = false)
    private int extractionRelevantCount = 0;

    @Column(name = "failures_count", nullable = false)
    private int failuresCount = 0;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public PipelineRun(Instant startedAt, PipelineStatus status) {
        this.startedAt = startedAt;
        this.status = status;
        this.collectedCount = 0;
        this.screeningRelevantCount = 0;
        this.extractionRelevantCount = 0;
        this.failuresCount = 0;
    }
}
