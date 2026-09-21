package io.github.lucasfcz.coralink.modules.opportunity.model;

import io.github.lucasfcz.coralink.modules.opportunity.enums.*;
import io.github.lucasfcz.coralink.modules.pipeline.model.RawOpportunity;
import jakarta.persistence.*;
import lombok.AccessLevel;
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

    @OneToOne(fetch = FetchType.LAZY)
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

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "opportunity_target_audiences", joinColumns = @JoinColumn(name = "opportunity_id"))
    @org.hibernate.annotations.BatchSize(size = 25)
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

    @Column(name = "source_name", nullable = false)
    private String sourceName;

    @Column
    private String imageUrl;

    @Column(nullable = false)
    private Boolean isFree;

    // Define se a oportunidade é aberta ao público externo e a qualquer estudante (true)
    // ou se é restrita estritamente a alunos matriculados na instituição de origem (false).
    @Column(nullable = false)
    private Boolean isForAll;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDate expiresAt;

    public Opportunity(RawOpportunity rawOpportunity, String summary, OpportunityType type,
                       String thematicArea, Set<TargetCourseAudience> targetCourseAudiences, Modality modality,
                       LocalDate startDate, LocalDate endDate, LocalDate registrationDeadline, String location, String officialUrl,
                       Double confidenceScoreAi, String imageUrl, Boolean isFree, Boolean isForAll) {
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
        this.isForAll = isForAll;
        this.expiresAt = calculateExpiresAt(LocalDate.now());
    }

    private LocalDate calculateExpiresAt(LocalDate referenceDate) {
        // 1. registrationDeadline + 3 dias
        if (this.registrationDeadline != null) {
            return this.registrationDeadline.plusDays(3);
        }
        // 2. endDate
        if (this.endDate != null) {
            return this.endDate;
        }
        // 3. startDate
        if (this.startDate != null) {
            return this.startDate;
        }
        // 4. Default de 30 dias a partir da criação (caso de notícias, artigos...)
        LocalDate base = (this.createdAt != null) ? this.createdAt.toLocalDate() : referenceDate;
        return base.plusDays(30);
    }

    /**
     * Permite que o administrador edite os campos gerados pela IA ou corrija inconsistências.
     */
    public void updateAdminFields(
            String title,
            String summary,
            OpportunityType type,
            String thematicArea,
            Modality modality,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate registrationDeadline,
            String location,
            String officialUrl,
            String imageUrl,
            Boolean isFree,
            Boolean isForAll,
            Set<TargetCourseAudience> targetCourseAudiences,
            LocalDate expiresAt
    ) {
        if (title != null && !title.isBlank()) this.title = title;
        if (summary != null && !summary.isBlank()) this.summary = summary;
        if (type != null) this.type = type;
        this.thematicArea = thematicArea;
        this.modality = modality;
        this.startDate = startDate;
        this.endDate = endDate;
        this.registrationDeadline = registrationDeadline;
        this.location = location;
        if (officialUrl != null && !officialUrl.isBlank()) this.officialUrl = officialUrl;
        this.imageUrl = imageUrl;
        if (isFree != null) this.isFree = isFree;
        if (isForAll != null) this.isForAll = isForAll;
        if (targetCourseAudiences != null) {
            this.targetCourseAudiences = new HashSet<>(targetCourseAudiences);
        }
        if (expiresAt != null) {
            this.expiresAt = expiresAt;
        }
    }

    public void updateType(OpportunityType type) {
        if (type != null) {
            this.type = type;
        }
    }

    public void makeOpportunityExpirates() {
        this.expiresAt = LocalDate.now().minusDays(1);
    }
}