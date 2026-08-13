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
public class UfpeCollector extends HtmlCollector {

    private static final String BASE_URL = "https://www.ufpe.br";
    private static final String NEWS_URL = BASE_URL + "/ascom/noticias";

    @Override
    protected String baseUrl() {
        return BASE_URL;
    }

    @Override
    protected String imageFallBackUrl() {
        return "https://imgs.search.brave.com/EIGE7DslzLVogBhwkZrF1MvSE5V4PGePlzzSx0VojcA/rs:fit:860:0:0:0/g:ce/aHR0cHM6Ly9pbWFn/ZXMuc2Vla2xvZ28u/Y29tL2xvZ28tcG5n/LzM4LzEvdW5pdmVy/c2lkYWRlLWZlZGVy/YWwtZGUtcGVybmFt/YnVjby11ZnBlLWxv/Z28tcG5nX3NlZWts/b2dvLTM4NDg2NC5w/bmc";
    }

    @Override
    protected String pageUrl() {
        return NEWS_URL;
    }

    @Override
    protected List<Element> articles(Document document) {
        return document.select("h3.list-full-content__title > a");
    }

    @Override
    protected NewsSummary mapArticle(Element articleLink) {
        String title = articleLink.text().trim();
        String url = articleLink.absUrl("href");

        if (title.isBlank() || url.isBlank()) {
            return null;
        }

        return new NewsSummary(
                title,
                title,
                url,
                sourceName(),
                LocalDateTime.now()
        );
    }

    @Override
    public SourceName sourceName() {
        return SourceName.UFPE;
    }
}