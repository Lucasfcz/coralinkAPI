package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.sources.collector.HtmlCollector;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Coletor de cursos, workshops e eventos do Centro Universitário Brasileiro (UNIBRA).
 */
@Component
public class UnibraCollector extends HtmlCollector {


    private static final String BASE_URL = "https://eventos.grupounibra.com/buscar/todos";

    @Override
    protected String baseUrl() {
        return BASE_URL;
    }

    @Override
    protected String imageFallBackUrl() {
        return "https://eventos.grupounibra.com/themes/wc_eventos/images/default.jpg";
    }

    @Override
    public SourceName sourceName() {
        return SourceName.UNIBRA;
    }

    @Override
    protected String pageUrl() {
        return BASE_URL;
    }

    @Override
    protected List<Element> articles(Document document) {
        if (document == null) {
            return List.of();
        }
        return document.select("article.single_pdt");
    }

    @Override
    protected NewsSummary mapArticle(Element article) {
        Element link = article.selectFirst("h1 a");
        if (link == null) {
            return null;
        }

        String title = link.text().trim();
        String url = link.absUrl("href");

        if (title.isBlank() || url.isBlank()) {
            return null;
        }

        String summary = title;
        Element descEl = article.selectFirst(".infos p, .single_pdt_desc");
        if (descEl != null && !descEl.text().isBlank()) {
            summary = descEl.text().trim();
        }

        LocalDateTime publishedDate = LocalDateTime.now();
        String startDate = article.attr("data-inicio");
        if (!startDate.isBlank()) {
            try {
                publishedDate = LocalDateTime.parse(startDate.trim().replace(" ", "T"));
            } catch (Exception ignored) {
            }
        }

        return new NewsSummary(title, summary, url, sourceName(), publishedDate);
    }
}
