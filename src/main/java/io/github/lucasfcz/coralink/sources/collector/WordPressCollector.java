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
                    .sslSocketFactory(RESILIENT_SSL_SOCKET_FACTORY)
                    .ignoreContentType(true)
                    .timeout(TIMEOUT_MILLIS)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
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
        // First try: Fetch full content directly from WordPress REST API using the post slug
        try {
            String slug = extractSlug(url);
            if (!slug.isBlank()) {
                String endpoint = baseUrl().replaceAll("/+$", "") + "/wp-json/wp/v2/posts?slug=" + slug + "&_embed=wp:featuredmedia";
                String json = Jsoup.connect(endpoint)
                        .sslSocketFactory(RESILIENT_SSL_SOCKET_FACTORY)
                        .ignoreContentType(true)
                        .timeout(TIMEOUT_MILLIS)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .execute()
                        .body();

                JsonNode posts = OBJECT_MAPPER.readTree(json);
                if (posts.isArray() && !posts.isEmpty()) {
                    JsonNode post = posts.get(0);
                    String renderedContent = post.path("content").path("rendered").asText();
                    String text = Jsoup.parse(renderedContent).text().trim();
                    String imageUrl = extractFeaturedImage(post);

                    if (!text.isBlank()) {
                        return new DetailedContent(text, imageUrl != null && !imageUrl.isBlank() ? imageUrl : imageFallBackUrl());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("WordPress REST API detail fetch failed for {}, falling back to HTML parsing: {}", url, e.getMessage());
        }

        // Fallback: Parse the rendered HTML page
        try {
            Document document = requestDocument(url);
            if (document != null) {
                List<String> wpSelectors = List.of(
                        "article .entry-content",
                        ".entry-content",
                        ".elementor-widget-theme-post-content",
                        ".elementor-post__content",
                        ".elementor-widget-container",
                        ".post-content",
                        ".colibri-post-content",
                        "main article",
                        "article",
                        "main"
                );

                Element content = null;
                String text = "";

                for (String selector : wpSelectors) {
                    Element candidate = document.selectFirst(selector);
                    if (candidate != null) {
                        String extracted = extractSummary(candidate);
                        if (extracted.length() > 50) {
                            content = candidate;
                            text = extracted;
                            break;
                        } else if (text.isBlank() && !extracted.isBlank()) {
                            content = candidate;
                            text = extracted;
                        }
                    }
                }

                if (text.isBlank() && document.body() != null) {
                    text = extractSummary(document.body());
                }

                if (!text.isBlank()) {
                    return new DetailedContent(text, extractImage(document, content != null ? content : document.body()));
                }
            }
        } catch (Exception e) {
            log.error("Failed to collect detailed content from HTML for URL: {}", url, e);
        }
        return null;
    }

    private String extractSlug(String url) {
        if (url == null || url.isBlank()) return "";
        String clean = url.split("\\?")[0].split("#")[0].replaceAll("/+$", "");
        int lastSlash = clean.lastIndexOf('/');
        return lastSlash >= 0 ? clean.substring(lastSlash + 1) : clean;
    }

    private String extractFeaturedImage(JsonNode post) {
        JsonNode media = post.path("_embedded").path("wp:featuredmedia");
        if (media.isArray() && !media.isEmpty()) {
            String sourceUrl = media.get(0).path("source_url").asText();
            if (!sourceUrl.isBlank()) {
                return sourceUrl;
            }
        }
        return null;
    }
}