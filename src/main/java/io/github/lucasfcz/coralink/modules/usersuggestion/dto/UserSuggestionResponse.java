package io.github.lucasfcz.coralink.modules.usersuggestion.dto;

import io.github.lucasfcz.coralink.modules.usersuggestion.model.SuggestionType;

import java.time.LocalDateTime;

public record UserSuggestionResponse(
        Long id,
        SuggestionType type,
        String suggestion,
        String userEmail,
        LocalDateTime createdAt
) {
    public UserSuggestionResponse(Long id, SuggestionType type, String suggestion, String userEmail) {
        this(id, type, suggestion, userEmail, LocalDateTime.now());
    }
}
