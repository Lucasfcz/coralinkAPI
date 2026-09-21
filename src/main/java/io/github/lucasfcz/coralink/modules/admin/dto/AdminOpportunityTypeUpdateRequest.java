package io.github.lucasfcz.coralink.modules.admin.dto;

import io.github.lucasfcz.coralink.modules.opportunity.enums.OpportunityType;
import jakarta.validation.constraints.NotNull;

public record AdminOpportunityTypeUpdateRequest(
        @NotNull(message = "O tipo é obrigatório")
        OpportunityType type
) {}
