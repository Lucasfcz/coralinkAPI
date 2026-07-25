package io.github.lucasfcz.coralink.mappers;

import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.dto.ScreeningBatchResult;
import io.github.lucasfcz.coralink.dto.ScreeningResult;
import io.github.lucasfcz.coralink.model.RawOpportunity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RawOpportunityMapper {

    public RawOpportunity toEntity(NewsSummary news) {
        return RawOpportunity.builder()
                .title(news.title())
                .shortSummary(news.shortSummary())
                .newsUrl(news.url())
                .sourceName(news.sourceName())
                .screenedRelevant(null)
                .probableType(null)
                .screeningReasoning(null)
                .becameOpportunity(false)
                .build();
    }
}
