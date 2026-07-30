package io.github.lucasfcz.coralink.sources.collector;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.NewsSummary;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public abstract class HtmlCollector extends AbstractCollector {

    protected abstract String pageUrl();

    protected abstract List<Element> articles(Document document);

    protected abstract NewsSummary mapArticle(Element article);

    @Override
    public List<NewsSummary> collect() {

        Document document = requestDocument(pageUrl());

        return articles(document)
                .stream()
                .map(this::mapArticle)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public DetailedContent detailedCollect(String url) {

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

        String text = content == null ? "" : content.text();

        return new DetailedContent(text, extractImage(document, content));
    }

    protected String extractImage(Document document, Element content) {

        Element og = document.selectFirst("meta[property=og:image]");

        if (og != null) {
            return og.attr("content");
        }
        if (content != null) {
            Element image = content.selectFirst("img");

            if (image != null) {
                return image.absUrl("src");
            }
        }

        return null;
    }

    protected NewsSummary buildSummary(String title, String summary, String url) {
        return new NewsSummary(title, summary, url, sourceName(), LocalDateTime.now());
    }
}