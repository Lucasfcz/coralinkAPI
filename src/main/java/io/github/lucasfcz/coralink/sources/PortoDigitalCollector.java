package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.sources.collector.HtmlCollector;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PortoDigitalCollector extends HtmlCollector {

    private static final String BASE_URL = "https://www.portodigital.org";
    private static final String NEWS_URL = BASE_URL + "/noticias";

    @Override
    protected String baseUrl() {
        return BASE_URL;
    }

    @Override
    protected String imageFallBackUrl() {
        return "https://imgs.search.brave.com/pDEX8i6TSfIuG26w8s6S8QjJllO_kqsKEUzz7MLnhd4/rs:fit:860:0:0:0/g:ce/aHR0cHM6Ly93d3cu/cG9ydG9kaWdpdGFs/Lm9yZy9fbnV4dC9p/bWcvbG9nby41NDE3/ZDljLnN2Zw";
    }

    @Override
    protected String pageUrl() {
        return NEWS_URL;
    }

    @Override
    protected List<Element> articles(Document document) {

        return document.select("a[href^='/noticias/'], a[href^='" + BASE_URL + "/noticias/']")
                .stream()
                .filter(link -> {
                    String url = link.absUrl("href");

                    return !url.equals(NEWS_URL)
                            && !url.equals(NEWS_URL + "/")
                            && !link.text().isBlank();
                })
                .toList();
    }

    @Override
    protected NewsSummary mapArticle(Element articleLink) {
        String title = articleLink.text().trim();
        String url = articleLink.absUrl("href");

        if (title.isBlank() || url.isBlank()) {
            return null;
        }

        Element container = findRelevantContainer(articleLink);

        String summary = extractSummary(container, title);

        return new NewsSummary(title, summary, url, sourceName(), LocalDateTime.now());
    }

    @Override
    public SourceName sourceName() {
        return SourceName.PORTO_DIGITAL;
    }

    private Element findRelevantContainer(Element link) {
        Element container = link.closest("article, li, .card, [class*=card], [class*=news], [class*=noticia]");

        return container != null ? container : link.parent();
    }
}