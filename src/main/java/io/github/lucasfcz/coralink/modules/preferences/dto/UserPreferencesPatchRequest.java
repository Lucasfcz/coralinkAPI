package io.github.lucasfcz.coralink.modules.preferences.dto;

import io.github.lucasfcz.coralink.modules.opportunity.enums.OpportunityType;
import io.github.lucasfcz.coralink.modules.opportunity.enums.TargetCourseAudience;

import java.util.Set;

public record UserPreferencesPatchRequest(
        Set<OpportunityType> selectedTypes,
        Set<String> institutions,
        Set<TargetCourseAudience> targetCourses,
        Boolean notInCollege
) {}
