package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.enums.SourceName;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
public class CinUfpeCollector extends WordPressCollector {

    private static final String BASE_URL = "https://portal.cin.ufpe.br";
    private static final String NEWS_URL = BASE_URL + "/category/noticia/";

    public CinUfpeCollector(SourceTypeDetector detector) {
        super(detector);
    }

    @Override
    protected String baseUrl() {
        return BASE_URL;
    }

    @Override
    protected String postsEndpoint() {
        return BASE_URL + "/wp-json/wp/v2/posts?per_page=20";
    }

    @Override
    protected List<NewsSummary> collectHtml() {

        try {
            Document doc = requestDocument(NEWS_URL);

            return doc.select("article")
                    .stream()
                    .map(this::extractHtmlSummary)
                    .filter(Objects::nonNull)
                    .toList();

        } catch (Exception exception) {
            throw new RuntimeException(
                    "Failed HTML collection from CIN UFPE",
                    exception
            );
        }
    }

    @Override
    protected DetailedContent detailedWordPress(String url) {

        try {
            Document doc = requestDocument(url);

            Element content = doc.selectFirst(".colibri-post-content");

            String text = content != null
                    ? content.text()
                    : "";

            return new DetailedContent(
                    text,
                    extractImageUrl(doc, content)
            );

        } catch (Exception exception) {
            throw new RuntimeException(
                    "Failed wordpress detail extraction from CIN UFPE",
                    exception
            );
        }
    }

    @Override
    protected DetailedContent detailedHtml(String url) {

        try {
            Document doc = requestDocument(url);

            Element content = doc.selectFirst(".colibri-post-content");

            if (content == null) {
                content = doc.selectFirst("main");
            }

            String text = content != null
                    ? content.text()
                    : "";

            return new DetailedContent(
                    text,
                    extractImageUrl(doc, content)
            );

        } catch (Exception exception) {
            throw new RuntimeException("Failed html detail extraction from CIN UFPE", exception);
        }
    }

    private NewsSummary extractHtmlSummary(Element article) {

        Element titleLink = article.selectFirst("h4 a, h3 a, .entry-title a");
        Element summaryElement = article.selectFirst("p");

        if (titleLink == null) {
            return null;
        }

        String title = titleLink.text();
        String url = titleLink.absUrl("href");

        String summary = summaryElement != null
                ? summaryElement.text()
                : title;

        if (title.isBlank() || url.isBlank()) {
            return null;
        }

        return new NewsSummary(title, summary, url, sourceName(), LocalDateTime.now()
        );
    }

    private String extractImageUrl(Document doc, Element content) {

        Element ogImage = doc.selectFirst("meta[property=og:image]");

        if (ogImage != null) {
            return ogImage.attr("content");
        }

        if (content != null) {
            Element image = content.selectFirst("img");

            if (image != null) {
                return image.absUrl("src");
            }
        }

        return null;
    }

    @Override
    public SourceName sourceName() {
        return SourceName.CIN_UFPE;
    }
}