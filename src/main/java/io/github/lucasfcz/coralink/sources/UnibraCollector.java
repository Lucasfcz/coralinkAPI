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
        return "https://media.glassdoor.com/sqll/2699387/unibra-squarelogo-1643977289033.png";
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
