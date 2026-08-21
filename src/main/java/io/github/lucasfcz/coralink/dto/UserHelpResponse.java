package io.github.lucasfcz.coralink.dto;

import io.github.lucasfcz.coralink.enums.SuggestionType;

public record UserHelpResponse(
        Long id,
        SuggestionType type,
        String suggestion,
        String userEmail
) {}
