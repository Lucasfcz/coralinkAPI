package io.github.lucasfcz.coralink.dto;

import java.util.List;

public record ScreeningBatchResult(
        List<ScreeningResult> screeningResults
) {}
