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
public class UnibraCollector extends HtmlCollector {

    private static final String BASE_URL = "https://eventos.grupounibra.com/buscar/todos";

    @Override
    protected String baseUrl() {
        return BASE_URL;
    }

    @Override
    protected String imageFallBackUrl() {
        return "https://imgs.search.brave.com/4aqO1n174m5CNng9b0o3ftneuGkT61O0ed7tdG2yvHg/rs:fit:860:0:0:0/g:ce/aHR0cHM6Ly9jZG4u/Zm9saGFwZS5jb20u/YnIvdXBsb2FkL2Ru/X2FycXVpdm8vMjAy/Mi8wNS9kZXNpZ24t/c2VtLW5vbWUtMjcu/anBn";
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
        return document.select("article.single_pdt");
    }

    @Override
    protected NewsSummary mapArticle(Element article) {
        Element link = article.selectFirst("h1 a");
        if (link == null) {
            return null;
        }

        String title = link.text();
        String url = link.absUrl("href");

        if (title.isBlank() || url.isBlank()) {
            return null;
        }

        return new NewsSummary(title, "", url, sourceName(), LocalDateTime.now());
    }
}
