package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.exceptions.CollectException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
public class CinUfpeCollector implements Collector {

    private static final String URL = "https://portal.cin.ufpe.br/category/noticia/";
    private static final int TIMEOUT_MILLIS = 10_000;

    @Override
    public SourceName sourceName() {
        return SourceName.CIN_UFPE;
    }

    @Override
    public List<NewsSummary> collect() {
        try {
            Document doc = request(URL);
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
            validateNewsUrl(newsUrl);
            return extractDetailedContent(request(newsUrl));

        } catch (IOException e) {
            throw new CollectException("Failed to fetch detail from CIN-UFPE: " + newsUrl, e);
        }
    }

    private Document request(String url) throws IOException {
        return Jsoup.connect(url)
                .timeout(TIMEOUT_MILLIS)
                .userAgent("CoralinkBot/1.0 (+https://github.com/lucasfcz/coralink)")
                .followRedirects(false)
                .get();
    }

    private void validateNewsUrl(String newsUrl) {
        try {
            URI uri = URI.create(newsUrl);
            if (!"https".equals(uri.getScheme()) || !"portal.cin.ufpe.br".equalsIgnoreCase(uri.getHost())) {
                throw new CollectException("Invalid CIN-UFPE news URL");
            }
        } catch (IllegalArgumentException exception) {
            throw new CollectException("Invalid CIN-UFPE news URL", exception);
        }
    }

    DetailedContent extractDetailedContent(Document doc) {
        Element contentContainer = doc.selectFirst(".colibri-post-content");
        String fullContent = contentContainer != null ? contentContainer.text() : "";
        return new DetailedContent(fullContent, extractImageUrl(doc, contentContainer));
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

    NewsSummary extractSummary(Element article) {
        Element titleLink = article.selectFirst("h4 a, h3 a, .entry-title a");
        Element summaryEl = article.selectFirst("p");

        if (titleLink == null || summaryEl == null) {
            return null;
        }

        String title = titleLink.text();
        String url = titleLink.absUrl("href");
        String summary = summaryEl.text();

        if (title.isBlank() || url.isBlank() || summary.isBlank()) {
            return null;
        }

        return new NewsSummary(title, summary, url, sourceName(), LocalDateTime.now());
    }
}
