package io.github.lucasfcz.coralink.modules.opportunity.dto;

import io.github.lucasfcz.coralink.modules.opportunity.enums.Modality;
import io.github.lucasfcz.coralink.modules.opportunity.enums.OpportunityType;
import io.github.lucasfcz.coralink.modules.opportunity.enums.TargetCourseAudience;
import java.time.LocalDate;
import java.util.Set;

public record OpportunityResponse(
        Long id,
        String title,
        String summary,
        OpportunityType type,
        String thematicArea,
        Set<TargetCourseAudience> targetCourseAudiences,
        Modality modality,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate registrationDeadline,
        String location,
        String officialUrl,
        String sourceName,
        String imageUrl,
        Boolean isFree,
        Boolean isForAll,
        LocalDate expiresAt
) { }
