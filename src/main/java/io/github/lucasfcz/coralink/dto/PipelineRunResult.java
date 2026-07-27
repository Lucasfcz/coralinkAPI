package io.github.lucasfcz.coralink.dto;

public record PipelineRunResult(
        int collected,
        int screenedRelevant,
        int createdOpportunities,
        int failures
) {}
