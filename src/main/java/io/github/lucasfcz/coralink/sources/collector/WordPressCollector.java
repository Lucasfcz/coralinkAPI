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

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Coletor base para portais baseados em WordPress.
 * Realiza consumo estruturado da WP REST API (/wp-json/wp/v2/posts).
 */
@Slf4j
public abstract class WordPressCollector extends AbstractCollector {

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected String postsEndpoint() {
        return baseUrl().replaceAll("/+$", "") + "/wp-json/wp/v2/posts?per_page=20";
    }

    public String singlePostEndpoint(String slug, String url) {
        return baseUrl().replaceAll("/+$", "") + "/wp-json/wp/v2/posts?slug=" + slug;
    }

    @Override
    public List<NewsSummary> collect() {
        try {
            return fetchPosts(postsEndpoint());
        } catch (CollectException e) {
            log.warn("Falha ao coletar notícias da fonte {}: {}", sourceName(), e.getMessage());
            return List.of();
        }
    }

    protected List<NewsSummary> fetchPosts(String endpoint) {
        try {
            String json = Jsoup.connect(endpoint)
                    .sslSocketFactory(RESILIENT_SSL_SOCKET_FACTORY)
                    .ignoreContentType(true)
                    .timeout(TIMEOUT_MILLIS)
                    .userAgent(USER_AGENT)
                    .execute()
                    .body();

            JsonNode posts = OBJECT_MAPPER.readTree(json);
            if (!posts.isArray()) {
                throw new BadResponseException("Endpoint WordPress não retornou um array JSON: " + endpoint);
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
            throw new CollectException("Falha ao buscar posts do WordPress: " + endpoint, e);
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
        String slug = extractSlug(url);
        if (slug.isBlank()) {
            return null;
        }

        try {
            String endpoint = singlePostEndpoint(slug, url);
            String json = Jsoup.connect(endpoint)
                    .sslSocketFactory(RESILIENT_SSL_SOCKET_FACTORY)
                    .ignoreContentType(true)
                    .timeout(TIMEOUT_MILLIS)
                    .userAgent(USER_AGENT)
                    .execute()
                    .body();

            JsonNode posts = OBJECT_MAPPER.readTree(json);
            if (posts.isArray() && !posts.isEmpty()) {
                JsonNode post = posts.get(0);
                String renderedContent = post.path("content").path("rendered").asText();
                Document parsed = Jsoup.parse(renderedContent);
                String text = extractSummary(parsed.body());

                if (!text.isBlank()) {
                    return new DetailedContent(text);
                }
            }
        } catch (Exception e) {
            log.warn("Falha ao coletar detalhamento WordPress para a URL {}: {}", url, e.getMessage());
        }

        return null;
    }
}