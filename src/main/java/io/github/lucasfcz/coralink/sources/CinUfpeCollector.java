package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.exceptions.CollectException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
public class CinUfpeCollector implements Collector {

    private static final String URL = "https://portal.cin.ufpe.br/category/noticia/";

    @Override
    public List<NewsSummary> collect() {
        try {
            Document doc = Jsoup.connect(URL).get();
            Elements articles = doc.select("article");

            return articles.stream()
                    .map(this::extractSummary)
                    .filter(Objects::nonNull)
                    .toList();

        } catch (IOException e) {
            throw new CollectException("Failed to collect data from CIN-UFPE", e);
        }
    }

    private NewsSummary extractSummary(Element article) {
        Element titleLink = article.selectFirst("h4 a, h3 a, .entry-title a");
        Element summaryEl = article.selectFirst("p");

        if (titleLink == null || summaryEl == null) {
            return null;
        }

        String title = titleLink.text();
        String url = titleLink.attr("href");
        String summary = summaryEl.text();

        return new NewsSummary(title, summary, url, SourceName.CIN_UFPE, LocalDateTime.now());
    }
}