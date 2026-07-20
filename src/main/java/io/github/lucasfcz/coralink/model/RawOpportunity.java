package io.github.lucasfcz.coralink.model;

import io.github.lucasfcz.coralink.enums.OpportunityType;
import io.github.lucasfcz.coralink.enums.SourceName;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "source_name")
    private SourceName sourceName;

    // result of AI screening, null until screening occurs
    private Boolean screenedRelevant;

    @Enumerated(EnumType.STRING)
    private OpportunityType probableType;

    @Column(columnDefinition = "TEXT")
    private String screeningReasoning;

    @Column(nullable = false)
    private Boolean becameOpportunity; // became an Opportunity after phase 2?

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime foundAt;

    @Builder
    private RawOpportunity(String title, String shortSummary, String newsUrl, SourceName sourceName,
                           Boolean screenedRelevant, OpportunityType probableType,
                           String screeningReasoning, Boolean becameOpportunity) {
        this.title = title;
        this.shortSummary = shortSummary;
        this.newsUrl = newsUrl;
        this.sourceName = sourceName;
        this.screenedRelevant = screenedRelevant;
        this.probableType = probableType;
        this.screeningReasoning = screeningReasoning;
        this.becameOpportunity = becameOpportunity;
    }
}