package io.github.lucasfcz.coralink.model;

import io.github.lucasfcz.coralink.enums.*;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "opportunities")
public class Opportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "raw_opportunity_id", nullable = false, unique = true)
    private RawOpportunity rawOpportunity;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OpportunityType type;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "opportunity_thematic_areas", joinColumns = @JoinColumn(name = "opportunity_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "thematic_area")
    private Set<ThematicArea> thematicAreas = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "opportunity_target_audiences", joinColumns = @JoinColumn(name = "opportunity_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "target_audience")
    private Set<TargetAudience> targetAudiences = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private Modality modality;

    private LocalDate eventDate;

    private LocalDate registrationDeadline;

    private String location;

    @Column(nullable = false)
    private String officialUrl;

    @Column(nullable = false)
    private Double confidenceScoreAi;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceName sourceName;

    @Column
    private String imageUrl;

    @Column(nullable = false)
    private boolean isFree;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private Opportunity(RawOpportunity rawOpportunity, String summary, OpportunityType type,
                        Set<ThematicArea> thematicAreas, Set<TargetAudience> targetAudiences, Modality modality,
                        LocalDate eventDate, LocalDate registrationDeadline, String location, String officialUrl,
                        Double confidenceScoreAi, String imageUrl, boolean isFree) {
        this.rawOpportunity = rawOpportunity;
        this.title = rawOpportunity.getTitle();
        this.summary = summary;
        this.type = type;
        this.thematicAreas = thematicAreas != null ? thematicAreas : new HashSet<>();
        this.targetAudiences = targetAudiences != null ? targetAudiences : new HashSet<>();
        this.modality = modality;
        this.eventDate = eventDate;
        this.registrationDeadline = registrationDeadline;
        this.location = location;
        this.officialUrl = officialUrl;
        this.confidenceScoreAi = confidenceScoreAi;
        this.sourceName = rawOpportunity.getSourceName();
        this.imageUrl = imageUrl;
        this.isFree = isFree;
    }
}