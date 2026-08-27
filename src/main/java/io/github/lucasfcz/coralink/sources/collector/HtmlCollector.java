package io.github.lucasfcz.coralink.sources.collector;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.NewsSummary;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.List;
import java.util.Objects;

/**
 * Coletor base para páginas HTML tradicionais (scraping via Jsoup).
 * Itera sobre seletores comuns de conteúdo editorial acadêmico para extrair texto e imagem limpos.
 */
@Slf4j
public abstract class HtmlCollector extends AbstractCollector {

    private static final List<String> CONTENT_SELECTORS = List.of(
            ".asset-full-content",
            ".asset-content",
            "article .entry-content",
            ".entry-content",
            ".post-content",
            ".news-content",
            ".noticia-corpo",
            ".conteudo-noticia",
            ".event-description",
            "main article",
            "article",
            "main",
            ".journal-content-article"
    );

    protected abstract String pageUrl();

    protected abstract List<Element> articles(Document document);

    protected abstract NewsSummary mapArticle(Element article);

    @Override
    public List<NewsSummary> collect() {
        try {
            Document document = requestDocument(pageUrl());
            if (document == null) {
                return List.of();
            }

            return articles(document)
                    .stream()
                    .map(this::mapArticle)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.error("Falha ao coletar resumos de notícias da URL: {}", pageUrl(), e);
            return List.of();
        }
    }

    @Override
    public DetailedContent detailedCollect(String url) {
        try {
            Document document = requestDocument(url);
            if (document == null) {
                return null;
            }

            String text = "";

            for (String selector : CONTENT_SELECTORS) {
                Element candidate = document.selectFirst(selector);
                if (candidate != null) {
                    String extracted = extractSummary(candidate);
                    if (extracted.length() > 50) {
                        text = extracted;
                        break;
                    }
                }
            }

            if (text.isBlank() && document.body() != null) {
                text = extractSummary(document.body());
            }

            if (!text.isBlank()) {
                return new DetailedContent(text);
            }
        } catch (Exception e) {
            log.error("Falha ao coletar conteúdo detalhado para URL: {}", url, e);
        }
        return null;
    }
}