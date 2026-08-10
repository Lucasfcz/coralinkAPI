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

    @Column(name = "thematic_area")
    private String thematicArea;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "opportunity_target_audiences", joinColumns = @JoinColumn(name = "opportunity_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "target_audience")
    private Set<TargetCourseAudience> targetCourseAudiences = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private Modality modality;

    private LocalDate startDate;

    private LocalDate endDate;

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
    private Boolean isFree;

    // this variable means if opportunity is exclusive for students from the university or not, if true, it is exclusive, if false, it is open for everyone
    @Column(nullable = false)
    private Boolean isExclusive;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private Opportunity(RawOpportunity rawOpportunity, String summary, OpportunityType type,
                        String thematicArea, Set<TargetCourseAudience> targetCourseAudiences, Modality modality,
                        LocalDate startDate, LocalDate endDate, LocalDate registrationDeadline, String location, String officialUrl,
                        Double confidenceScoreAi, String imageUrl, Boolean isFree, Boolean isExclusive) {
        this.rawOpportunity = rawOpportunity;
        this.title = rawOpportunity.getTitle();
        this.summary = summary;
        this.type = type;
        this.thematicArea = thematicArea;
        this.targetCourseAudiences = targetCourseAudiences != null ? targetCourseAudiences : new HashSet<>();
        this.modality = modality;
        this.startDate = startDate;
        this.endDate = endDate;
        this.registrationDeadline = registrationDeadline;
        this.location = location;
        this.officialUrl = officialUrl;
        this.confidenceScoreAi = confidenceScoreAi;
        this.sourceName = rawOpportunity.getSourceName();
        this.imageUrl = imageUrl;
        this.isFree = isFree;
        this.isExclusive = isExclusive;
    }
}