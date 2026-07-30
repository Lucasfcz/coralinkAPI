package io.github.lucasfcz.coralink.sources.collector;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.exceptions.CollectException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.List;

public abstract class AbstractCollector implements Collector {

    private static final int TIMEOUT_MILLIS = 15_000;

    protected abstract String baseUrl();

    @Override
    public abstract List<NewsSummary> collect();

    @Override
    public abstract DetailedContent detailedCollect(String url);

    protected Document requestDocument(String url) {
        try {
            return Jsoup.connect(url)
                    .timeout(TIMEOUT_MILLIS)
                    .userAgent("CoralinkBot/1.0 (+https://github.com/lucasfcz/coralink)")
                    .referrer(baseUrl())
                    .followRedirects(true)
                    .get();

        } catch (IOException e) {
            throw new CollectException("Failed to fetch URL: " + url, e);
        }
    }
}