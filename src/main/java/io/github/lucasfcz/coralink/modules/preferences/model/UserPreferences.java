package io.github.lucasfcz.coralink.modules.preferences.model;

import io.github.lucasfcz.coralink.modules.opportunity.enums.OpportunityType;
import io.github.lucasfcz.coralink.modules.opportunity.enums.TargetCourseAudience;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_preferences_selected_types", joinColumns = @JoinColumn(name = "user_preferences_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    @Builder.Default
    private Set<OpportunityType> selectedTypes = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_preferences_institutions", joinColumns = @JoinColumn(name = "user_preferences_id"))
    @Column(name = "institution")
    @Builder.Default
    private Set<String> institutions = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_preferences_target_courses", joinColumns = @JoinColumn(name = "user_preferences_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "target_course_audience")
    @Builder.Default
    private Set<TargetCourseAudience> targetCourses = new HashSet<>();

    @Column(name = "not_in_college", nullable = false)
    @Builder.Default
    private boolean notInCollege = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
