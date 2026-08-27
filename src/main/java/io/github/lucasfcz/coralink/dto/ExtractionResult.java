package io.github.lucasfcz.coralink.dto;

import io.github.lucasfcz.coralink.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Set;

public record ExtractionResult(
        @NotNull Long rawOpportunityId,
        @NotBlank String summary,
        @NotNull OpportunityType type,
        @NotNull String thematicArea,
        @NotNull Set<TargetCourseAudience> targetCourseAudiences,
        @NotNull Modality modality,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull LocalDate registrationDeadline,
        @NotNull String location,
        @NotNull Boolean isFree,
        @NotNull Boolean isForAll,
        String imageUrl,
        @NotNull Double confidenceScore
) {}