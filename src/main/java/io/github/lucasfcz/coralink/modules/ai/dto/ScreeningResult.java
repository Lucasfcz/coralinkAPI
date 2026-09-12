package io.github.lucasfcz.coralink.modules.ai.dto;

import jakarta.validation.constraints.NotNull;

public record ScreeningResult(
        @NotNull Long rawOpportunityId,
        @NotNull Boolean isRelevant
) {
}
