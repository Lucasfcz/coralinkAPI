package io.github.lucasfcz.coralink.dto;

import java.util.List;

public record ExtractionBatchResult(
        List<ExtractionResult> extractionResults
) {}
