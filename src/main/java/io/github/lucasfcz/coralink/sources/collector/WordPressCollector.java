package io.github.lucasfcz.coralink.sources.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.exceptions.BadResponseException;
import io.github.lucasfcz.coralink.exceptions.CollectException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class WordPressCollector extends AbstractCollector {

    private static final int TIMEOUT_MILLIS = 15_000;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected String postsEndpoint() {
        return baseUrl().replaceAll("/+$", "") + "/wp-json/wp/v2/posts?per_page=20&_embed=wp:featuredmedia";
    }

    @Override
    public List<NewsSummary> collect() {
        try {
            return fetchPosts(postsEndpoint());
        } catch (CollectException e) {
            log.warn("Failed to collect {}: {}", sourceName(), e.getMessage());
            return List.of();
        }
    }

    protected List<NewsSummary> fetchPosts(String endpoint) {
        try {
            String json = Jsoup.connect(endpoint)
                    .ignoreContentType(true)
                    .timeout(TIMEOUT_MILLIS)
                    .userAgent("CoralinkBot/1.0 (+https://github.com/lucasfcz/coralink)")
                    .execute()
                    .body();

            JsonNode posts = OBJECT_MAPPER.readTree(json);
            if (!posts.isArray()) {
                throw new BadResponseException("WordPress posts endpoint did not return a JSON array: " + endpoint);
            }

            List<NewsSummary> result = new ArrayList<>();

            for (JsonNode post : posts) {
                NewsSummary summary = mapPost(post);

                if (summary != null) {
                    result.add(summary);
                }
            }

            return result;

        } catch (IOException e) {
            throw new CollectException("Failed to fetch WordPress posts: " + endpoint, e);
        }
    }

    protected NewsSummary mapPost(JsonNode post) {
        String title = Jsoup.parse(post.path("title").path("rendered").asText()).text();

        String summary = Jsoup.parse(post.path("excerpt").path("rendered").asText()).text();

        String url = post.path("link").asText();

        if (title.isBlank() || url.isBlank()) {
            return null;
        }

        LocalDateTime published;

        try {
            published = OffsetDateTime.parse(post.path("date").asText()).toLocalDateTime();
        } catch (Exception ignored) {
            published = LocalDateTime.now();
        }

        return new NewsSummary(
                title,
                summary.isBlank() ? title : summary,
                url,
                sourceName(),
                published
        );
    }

    @Override
    public DetailedContent detailedCollect(String url) {
        try {
        Document document = requestDocument(url);

        Element content = document.selectFirst(
                "article .entry-content," +
                        ".post-content," +
                        ".colibri-post-content," +
                        "main article," +
                        "main"
        );
        if(content != null) {
            String text = extractSummary(content);

            return new DetailedContent(text, extractImage(document, content));
        }
        } catch (Exception e) {
            throw new CollectException("Failed to collect detailed content for URL: " + url, e);
        }
        return null;
    }
}