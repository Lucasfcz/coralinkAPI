package io.github.lucasfcz.coralink.dto;

import io.github.lucasfcz.coralink.enums.SuggestionType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

public record UserHelpRequest(
        @NotNull SuggestionType type,
        @Max(5000) String suggestion,
        @Email String userEmail
) {}
