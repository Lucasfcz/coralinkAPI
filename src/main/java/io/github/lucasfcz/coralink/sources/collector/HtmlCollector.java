package io.github.lucasfcz.coralink.sources.collector;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.NewsSummary;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
public abstract class HtmlCollector extends AbstractCollector {

    protected abstract String pageUrl();

    protected abstract List<Element> articles(Document document);

    protected abstract NewsSummary mapArticle(Element article);

    @Override
    public List<NewsSummary> collect() {
        try {
            Document document = requestDocument(pageUrl());

            return articles(document)
                    .stream()
                    .map(this::mapArticle)
                    .filter(Objects::nonNull)
                    .toList();
        }
        catch (Exception e) {
            log.error("Failed to collect news summaries from URL: {}", pageUrl(), e);
            return List.of();
        }
    }

    @Override
    public DetailedContent detailedCollect(String url) {
        try {
            Document document = requestDocument(url);

            Element content = document.selectFirst(
                    ".journal-content-article," +
                            ".asset-full-content," +
                            "article .entry-content," +
                            ".content," +
                            ".post-content," +
                            "main article," +
                            "main"
            );
            if (content != null) {
                String text = extractSummary(content);
                return new DetailedContent(text, extractImage(document, content));
            }
        }
        catch (Exception e) {
            log.error("Failed to collect detailed content for URL: {}", url, e);
            return new DetailedContent("", null);
        }
        return null;
    }
}