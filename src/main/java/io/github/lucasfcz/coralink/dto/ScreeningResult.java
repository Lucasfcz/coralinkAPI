package io.github.lucasfcz.coralink.dto;

import io.github.lucasfcz.coralink.enums.OpportunityType;

public record ScreeningResult(
        boolean isRelevant,
        OpportunityType probablyType,
        String reasoning
) {
}
