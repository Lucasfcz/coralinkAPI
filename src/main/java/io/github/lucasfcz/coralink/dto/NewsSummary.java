package io.github.lucasfcz.coralink.dto;

import io.github.lucasfcz.coralink.enums.SourceName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

//dto responsible for first scraping and get new news
public record NewsSummary(
        @NotBlank String title,
        @NotBlank String shortSummary,
        @NotBlank String url,
        @NotNull SourceName sourceName,
        @NotNull LocalDateTime foundAt
) {}