package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.sources.collector.HtmlCollector;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Coletor de eventos, cursos e workshops no Sympla filtrados para a região de Recife-PE.
 */
@Slf4j
@Component
public class SymplaCollector extends HtmlCollector {

    private static final String BASE_URL = "https://www.sympla.com.br";
    private static final String PAGE_URL = "https://www.sympla.com.br/eventos/recife-pe/curso-workshop?category=collection";
    private static final String FALLBACK_IMAGE_URL = "https://www.sympla.com.br/images/logo-sympla-for-facebook.png";

    private static final List<String> LISTING_URLS = List.of(
            "https://www.sympla.com.br/eventos/recife-pe/curso-workshop?category=collection",
            "https://www.sympla.com.br/eventos/recife-pe/congresso-palestra?category=collection"
    );

    @Override
    protected String baseUrl() {
        return BASE_URL;
    }

    @Override
    protected String pageUrl() {
        return PAGE_URL;
    }

    @Override
    protected String imageFallBackUrl() {
        return FALLBACK_IMAGE_URL;
    }

    @Override
    public SourceName sourceName() {
        return SourceName.SYMPLA;
    }

    @Override
    protected List<Element> articles(Document document) {
        if (document == null) {
            return List.of();
        }
        return document.select("a[href*='/evento/']");
    }

    @Override
    protected NewsSummary mapArticle(Element card) {
        String rawUrl = card.absUrl("href");
        if (rawUrl.isBlank()) {
            rawUrl = card.attr("href");
        }
        if (rawUrl.isBlank()) {
            return null;
        }

        String cleanUrl = rawUrl.split("\\?")[0].split("#")[0];

        String title = card.attr("data-name").trim();
        if (title.isBlank()) {
            Element titleElement = card.selectFirst("h1, h2, h3, h4, [class*='title'], [class*='name']");
            if (titleElement != null) {
                title = titleElement.text().trim();
            }
        }
        if (title.isBlank()) {
            title = card.text().trim();
        }
        if (title.isBlank()) {
            return null;
        }

        String cardText = card.text().trim();
        String summary = !cardText.isBlank() ? cardText : title;

        return new NewsSummary(
                title,
                summary,
                cleanUrl,
                sourceName(),
                LocalDateTime.now()
        );
    }

    @Override
    public List<NewsSummary> collect() {
        Map<String, NewsSummary> summariesByUrl = new LinkedHashMap<>();

        for (String listingUrl : LISTING_URLS) {
            try {
                Document document = requestDocument(listingUrl);
                if (document == null) {
                    continue;
                }

                for (Element card : articles(document)) {
                    NewsSummary summary = mapArticle(card);
                    if (summary != null) {
                        summariesByUrl.putIfAbsent(summary.url(), summary);
                    }
                }
            } catch (Exception e) {
                log.error("Falha ao coletar eventos do Sympla na URL: {}", listingUrl, e);
            }
        }

        return new ArrayList<>(summariesByUrl.values());
    }
}

