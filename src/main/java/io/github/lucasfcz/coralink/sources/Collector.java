package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.enums.SourceName;

import java.util.List;

public interface Collector {
    SourceName sourceName();
    List<NewsSummary> collect();
    DetailedContent detailedCollect(String newsUrl);
}
