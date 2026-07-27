package io.github.lucasfcz.coralink.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ScreeningBatchResult(
        @NotEmpty List<@Valid ScreeningResult> screeningResults
) {}
