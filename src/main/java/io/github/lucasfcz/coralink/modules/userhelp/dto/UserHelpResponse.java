package io.github.lucasfcz.coralink.modules.userhelp.dto;

import io.github.lucasfcz.coralink.modules.userhelp.model.SuggestionType;

public record UserHelpResponse(
        Long id,
        SuggestionType type,
        String suggestion,
        String userEmail
) {}
