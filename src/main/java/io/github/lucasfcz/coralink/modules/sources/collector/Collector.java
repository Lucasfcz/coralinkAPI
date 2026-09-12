package io.github.lucasfcz.coralink.modules.sources.collector;

import io.github.lucasfcz.coralink.modules.sources.dto.DetailedContent;
import io.github.lucasfcz.coralink.modules.sources.dto.NewsSummary;

import java.util.List;

public interface Collector {
    String sourceName();
    List<NewsSummary> collect();
    DetailedContent detailedCollect(String newsUrl);
    String fallbackImageUrl();
}
