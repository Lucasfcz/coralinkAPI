package io.github.lucasfcz.coralink.dto;

import io.github.lucasfcz.coralink.enums.Modality;
import io.github.lucasfcz.coralink.enums.OpportunityType;
import io.github.lucasfcz.coralink.enums.TargetAudience;
import io.github.lucasfcz.coralink.enums.ThematicArea;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Set;

public record ExtrationResult(
        @NotBlank String summary,
        @NotNull OpportunityType type,
        @NotNull Set<ThematicArea> areas,
        @NotNull Set<TargetAudience> audiences,
        @NotNull Modality modality,
        @NotNull LocalDate eventDate,
        @NotNull LocalDate registrationDeadLine,
        @NotNull String location,
        @NotNull boolean isFree,
        @NotNull Double aiConfidenceScore
) {
}
