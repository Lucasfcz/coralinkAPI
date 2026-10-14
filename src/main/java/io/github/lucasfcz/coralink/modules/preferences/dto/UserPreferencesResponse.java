package io.github.lucasfcz.coralink.modules.preferences.dto;

import io.github.lucasfcz.coralink.modules.opportunity.enums.OpportunityType;
import io.github.lucasfcz.coralink.modules.opportunity.enums.TargetCourseAudience;

import java.time.LocalDateTime;
import java.util.Set;

public record UserPreferencesResponse(
        Long id,
        Long userId,
        Set<OpportunityType> selectedTypes,
        Set<String> institutions,
        Set<TargetCourseAudience> targetCourses,
        boolean notInCollege,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
