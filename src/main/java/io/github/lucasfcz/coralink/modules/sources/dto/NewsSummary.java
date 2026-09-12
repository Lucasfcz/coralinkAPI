package io.github.lucasfcz.coralink.modules.sources.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * DTO que representa o resumo preliminar de uma notícia/evento coletado na primeira etapa de scraping.
 */
public record NewsSummary(

        @NotBlank String title,
        @NotBlank String shortSummary,
        @NotBlank String url,
        @NotBlank String sourceName,
        @NotNull LocalDateTime foundAt
) {}