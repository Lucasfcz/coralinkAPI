package io.github.lucasfcz.coralink.dto;

import io.github.lucasfcz.coralink.enums.OpportunityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ScreeningResult(
        @NotNull Long rawOpportunityId,
        @NotNull boolean isRelevant,
        @NotNull OpportunityType probablyType,
        @NotBlank String reasoning
) {
}
