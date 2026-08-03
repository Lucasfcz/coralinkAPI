package io.github.lucasfcz.coralink.sources.collector;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.exceptions.CollectException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.List;

@Slf4j
public abstract class AbstractCollector implements Collector {

    private static final int TIMEOUT_MILLIS = 15_000;

    protected abstract String baseUrl();

    protected abstract String imageFallBackUrl();

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
            log.error("Failed to fetch URL: {}", url, e);
            return null;
        }
    }

    protected String extractSummary(Element content, String title) {
        content.select(
                "header," +
                        "footer," +
                        "nav," +
                        "aside," +
                        "script," +
                        "style," +
                        ".menu," +
                        ".navbar," +
                        ".breadcrumb," +
                        ".breadcrumbs," +
                        ".share," +
                        ".social," +
                        ".related," +
                        ".newsletter," +
                        ".comments"
        ).remove();

        return content.text();
    }

    protected String extractImage(Document document, Element content) {

        Element ogImage = document.selectFirst("meta[property=og:image]");

        if (ogImage != null) {
            return ogImage.attr("content");
        }

        if (content != null) {
            Element image = content.selectFirst("img");

            if (image != null) {
                return image.absUrl("src");
            }
        }

        return imageFallBackUrl();
    }
}