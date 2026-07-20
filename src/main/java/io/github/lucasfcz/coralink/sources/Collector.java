package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.dto.NewsSummary;

import java.util.List;

public interface Collector {
    List<NewsSummary> collect();
}