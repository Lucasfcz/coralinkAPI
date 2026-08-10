package io.github.lucasfcz.coralink.model;

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

    private Boolean isInvalid;

    @Column(nullable = false)
    private Boolean becameOpportunity; // became an Opportunity after phase 2?

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime foundAt;

    @Builder
    private RawOpportunity(String title, String shortSummary, String newsUrl, SourceName sourceName,
                           Boolean screenedRelevant, Boolean isInvalid, Boolean becameOpportunity) {
        this.title = title;
        this.shortSummary = shortSummary;
        this.newsUrl = newsUrl;
        this.sourceName = sourceName;
        this.screenedRelevant = screenedRelevant;
        this.isInvalid = isInvalid;
        this.becameOpportunity = becameOpportunity;
    }

    public void applyScreening(boolean relevant) {
        this.screenedRelevant = relevant;
    }

    public void markAsOpportunity() {
        this.becameOpportunity = true;
    }

    public void setIsInvalid() {
        this.isInvalid = true;
    }
}
