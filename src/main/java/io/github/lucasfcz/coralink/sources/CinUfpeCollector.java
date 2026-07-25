package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.ExtrationResult;
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

    @Override
    public DetailedContent detailedCollect(String newsUrl) {
        try {
            Document doc = Jsoup.connect(newsUrl).get();
            Element contentContainer = doc.selectFirst(".colibri-post-content");

            String fullContent = contentContainer != null
                    ? contentContainer.select("p").text()
                    : "";

            String imageUrl = extractImageUrl(doc, contentContainer);

            return new DetailedContent(fullContent, imageUrl);

        } catch (IOException e) {
            throw new CollectException("Failed to fetch detail from CIN-UFPE: " + newsUrl, e);
        }
    }

    private String extractImageUrl(Document doc, Element contentContainer) {
        Element ogImage = doc.selectFirst("meta[property=og:image]");
        if (ogImage != null) {
            return ogImage.attr("content");
        }

        if (contentContainer != null) {
            Element firstImg = contentContainer.selectFirst("img");
            if (firstImg != null) {
                return firstImg.attr("src");
            }
        }

        return null;
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
