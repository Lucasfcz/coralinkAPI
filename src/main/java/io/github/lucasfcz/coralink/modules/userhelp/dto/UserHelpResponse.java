package io.github.lucasfcz.coralink.modules.userhelp.dto;

import io.github.lucasfcz.coralink.modules.userhelp.model.SuggestionType;

import java.time.LocalDateTime;

public record UserHelpResponse(
        Long id,
        SuggestionType type,
        String suggestion,
        String userEmail,
        LocalDateTime createdAt
) {
    public UserHelpResponse(Long id, SuggestionType type, String suggestion, String userEmail) {
        this(id, type, suggestion, userEmail, LocalDateTime.now());
    }
}
