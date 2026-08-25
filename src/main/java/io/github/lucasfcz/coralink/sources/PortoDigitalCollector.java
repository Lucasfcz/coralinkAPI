package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.sources.collector.HtmlCollector;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
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
        return "https://www.portodigital.org/_nuxt/img/logo.5417d9c.svg";
    }

    @Override
    protected String pageUrl() {
        return NEWS_URL;
    }

    @Override
    protected List<Element> articles(Document document) {
        if (document == null) return List.of();

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

        // Clean up common button text inside the anchor
        title = title.replaceAll("(?i)\\b(ler mais|leia mais|veja mais)\\b", "").trim();

        Element container = findRelevantContainer(articleLink);
        String summary = extractSummary(container);

        return new NewsSummary(title, summary.isBlank() ? title : summary, url, sourceName(), LocalDateTime.now());
    }

    @Override
    public DetailedContent detailedCollect(String url) {
        try {
            Document document = requestDocument(url);
            if (document == null) return null;

            // Porto Digital renders news paragraphs inside the Nuxt body
            StringBuilder textBuilder = new StringBuilder();
            for (Element p : document.select("p")) {
                String text = p.text().trim();
                // Filter out footer / copyright / address text
                if (text.length() > 20
                        && !text.contains("Copyright")
                        && !text.contains("Cais do Apolo")
                        && !text.contains("CNPJ")
                        && !text.contains("Todos os direitos reservados")) {
                    if (!textBuilder.isEmpty()) textBuilder.append("\n\n");
                    textBuilder.append(text);
                }
            }

            String fullContent = textBuilder.toString().trim();
            if (!fullContent.isBlank()) {
                return new DetailedContent(fullContent, extractImage(document, null));
            }
        } catch (Exception e) {
            log.error("Failed to collect Porto Digital detailed content for URL: {}", url, e);
        }
        return null;
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