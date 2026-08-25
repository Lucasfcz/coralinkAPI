package io.github.lucasfcz.coralink.sources.collector;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.NewsSummary;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

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

    private static final List<String> CONTENT_SELECTORS = List.of(
            ".asset-full-content",
            ".asset-content",
            "article .entry-content",
            ".entry-content",
            ".post-content",
            ".news-content",
            ".noticia-corpo",
            ".conteudo-noticia",
            "main article",
            "article",
            "main",
            ".journal-content-article"
    );

    @Override
    public DetailedContent detailedCollect(String url) {
        try {
            Document document = requestDocument(url);
            if (document == null) {
                return null;
            }

            Element content = null;
            String text = "";

            for (String selector : CONTENT_SELECTORS) {
                Element candidate = document.selectFirst(selector);
                if (candidate != null) {
                    String extracted = extractSummary(candidate);
                    if (extracted.length() > 50) {
                        content = candidate;
                        text = extracted;
                        break;
                    } else if (text.isBlank() && !extracted.isBlank()) {
                        content = candidate;
                        text = extracted;
                    }
                }
            }

            if (text.isBlank() && document.body() != null) {
                text = extractSummary(document.body());
            }

            if (!text.isBlank()) {
                return new DetailedContent(text, extractImage(document, content != null ? content : document.body()));
            }
        } catch (Exception e) {
            log.error("Failed to collect detailed content for URL: {}", url, e);
        }
        return null;
    }
}