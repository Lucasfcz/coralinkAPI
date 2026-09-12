package io.github.lucasfcz.coralink.modules.pipeline.dto;

public record PipelineRunResult(
        int collected,
        int screenedRelevant,
        int createdOpportunities,
        int failures
) {}
