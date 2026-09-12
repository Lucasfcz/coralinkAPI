package io.github.lucasfcz.coralink.modules.ai.dto;

import java.util.List;

public record ExtractionBatchResult(
        List<ExtractionResult> extractionResults
) {}
