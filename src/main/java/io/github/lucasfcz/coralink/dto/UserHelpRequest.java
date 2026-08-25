package io.github.lucasfcz.coralink.dto;

import io.github.lucasfcz.coralink.enums.SuggestionType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserHelpRequest(
        @NotNull SuggestionType type,
        @NotBlank @Size(max = 5000) String suggestion,
        @Email String userEmail
) {}
