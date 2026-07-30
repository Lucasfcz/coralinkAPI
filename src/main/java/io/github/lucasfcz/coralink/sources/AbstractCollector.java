package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.NewsSummary;

import io.github.lucasfcz.coralink.enums.CollectorType;
import io.github.lucasfcz.coralink.exceptions.CollectException;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public abstract class AbstractCollector implements Collector {

    private static final int TIMEOUT_MILLIS = 10000;

    protected final SourceTypeDetector detector;

    protected abstract String baseUrl();

    protected CollectorType sourceType() {
        return detector.detect(baseUrl());
    }

    @Override
    public List<NewsSummary> collect() {

        return switch (sourceType()) {

            case WORDPRESS ->
                    collectWordPress();
            case HTML ->
                    collectHtml();
        };
    }

    @Override
    public DetailedContent detailedCollect(String url) {

        return switch (sourceType()) {

            case WORDPRESS ->
                    detailedWordPress(url);
            case HTML ->
                    detailedHtml(url);
        };
    }

    protected Document requestDocument(String url) {
        try {
            return Jsoup.connect(url)
                    .timeout(TIMEOUT_MILLIS)
                    .userAgent("CoralinkBot/1.0 (+https://github.com/lucasfcz/coralink)")
                    .followRedirects(true)
                    .get();

        } catch (IOException exception) {
            throw new CollectException("Failed to fetch url: " + url, exception);
        }
    }

    protected abstract List<NewsSummary> collectWordPress();
    protected abstract List<NewsSummary> collectHtml();
    protected abstract DetailedContent detailedWordPress(String url);
    protected abstract DetailedContent detailedHtml(String url);
}