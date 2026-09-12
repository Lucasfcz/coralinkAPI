package io.github.lucasfcz.coralink.modules.pipeline;

import io.github.lucasfcz.coralink.modules.sources.dto.NewsSummary;
import io.github.lucasfcz.coralink.modules.pipeline.dto.RawOpportunityResponse;
import io.github.lucasfcz.coralink.modules.pipeline.model.RawOpportunity;
import org.springframework.stereotype.Component;

@Component
public class RawOpportunityMapper {

    public RawOpportunity toEntity(NewsSummary news) {
        return new RawOpportunity(
                news.title(),
                news.shortSummary(),
                news.url(),
                news.sourceName(),
                null,
                false
        );
    }

    public RawOpportunityResponse toResponse(RawOpportunity raw) {
        return new RawOpportunityResponse(
                raw.getId(),
                raw.getTitle(),
                raw.getNewsUrl(),
                raw.getSourceName(),
                raw.getScreenedRelevant(),
                raw.getBecameOpportunity(),
                raw.getFoundAt(),
                raw.getPipelineRunId()
        );
    }
}
