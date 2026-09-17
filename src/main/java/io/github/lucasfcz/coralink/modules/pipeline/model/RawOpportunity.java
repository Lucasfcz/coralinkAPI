package io.github.lucasfcz.coralink.modules.pipeline.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "raw_opportunities")
public class RawOpportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String shortSummary;

    @Column(name = "news_url", nullable = false, unique = true)
    private String newsUrl;

    @Column(nullable = false, name = "source_name")
    private String sourceName;

    // Resultado da triagem por IA (Fase 1). Permanece null até a triagem ser executada.
    private Boolean screenedRelevant;

    // Indica se a notícia bruta avançou na Fase 2 e foi promovida a uma Oportunidade oficial.
    @Column(nullable = false)
    private Boolean becameOpportunity;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime foundAt;

    /**
     * Referência por ID à execução do pipeline correspondente.
     * Evita acoplamento e consultas N+1 via JPA.
     */
    @Column(name = "pipeline_run_id")
    private Long pipelineRunId;

    @Column(name = "extraction_attempts", nullable = false)
    private int extractionAttempts = 0;

    @Column(name = "last_extraction_error")
    private String lastExtractionError;

    public RawOpportunity(String title, String shortSummary, String newsUrl, String sourceName,
                          Boolean screenedRelevant, Boolean becameOpportunity) {
        this.title = title;
        this.shortSummary = shortSummary;
        this.newsUrl = newsUrl;
        this.sourceName = sourceName;
        this.screenedRelevant = screenedRelevant;
        this.becameOpportunity = becameOpportunity != null ? becameOpportunity : false;
    }

    public void applyScreening(boolean relevant) {
        this.screenedRelevant = relevant;
    }

    public void markAsOpportunity() {
        this.becameOpportunity = true;
    }

    public void assignPipelineRun(Long pipelineRunId) {
        this.pipelineRunId = pipelineRunId;
    }

    public void recordExtractionFailure(String error) {
        this.extractionAttempts++;
        this.lastExtractionError = error;
    }
}
