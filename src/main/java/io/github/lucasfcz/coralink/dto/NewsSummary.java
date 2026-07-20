package io.github.lucasfcz.coralink.dto;

import io.github.lucasfcz.coralink.enums.SourceName;

import java.time.LocalDateTime;

public record NewsSummary(
        String title,
        String shortSummary,
        String url,
        SourceName sourceName,
        LocalDateTime foundAt
) {}