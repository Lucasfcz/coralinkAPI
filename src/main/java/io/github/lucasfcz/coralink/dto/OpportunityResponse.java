package io.github.lucasfcz.coralink.dto;

import io.github.lucasfcz.coralink.enums.*;
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
        SourceName sourceName,
        String imageUrl,
        Boolean isFree,
        Boolean isForAll
) { }
