package io.github.lucasfcz.coralink.sources.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.NewsSummary;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class StoryblokCollector extends AbstractCollector {

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter STORYBLOK_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss]");

    protected abstract String apiToken();
    protected abstract String storiesEndpoint();
    protected abstract String singleStoryEndpointTemplate();
    protected abstract NewsSummary mapStory(JsonNode story);

    @Override
    public List<NewsSummary> collect() {
        JsonNode root = fetchJson(storiesEndpoint());
        JsonNode stories = root.path("stories");

        if (!stories.isArray()) {
            log.warn("Endpoint Storyblok não retornou array de histórias: {}", storiesEndpoint());
            return List.of();
        }

        List<NewsSummary> summaries = new ArrayList<>();
        for (JsonNode story : stories) {
            NewsSummary summary = mapStory(story);
            if (summary != null) {
                summaries.add(summary);
            }
        }
        return summaries;
    }

    @Override
    public DetailedContent detailedCollect(String url) {
        String slug = extractSlug(url);
        if (slug.isBlank()) return null;

        String endpoint = String.format(singleStoryEndpointTemplate(), slug, apiToken());
        JsonNode story = fetchJson(endpoint).path("story");

        if (story.isMissingNode() || story.isNull()) return null;

        String fullText = extractRichText(story.path("content").path("long_text")).trim();
        return fullText.isBlank() ? null : new DetailedContent(fullText);
    }

    /**
     * Centraliza a requisição HTTP e parsing JSON, evitando repetição de boilerplate.
     */
    protected JsonNode fetchJson(String url) {
        try {
            String json = Jsoup.connect(url)
                    .sslSocketFactory(RESILIENT_SSL_SOCKET_FACTORY)
                    .ignoreContentType(true)
                    .timeout(TIMEOUT_MILLIS)
                    .userAgent(USER_AGENT)
                    .execute()
                    .body();
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            log.warn("Erro ao buscar JSON em {}: {}", url, e.getMessage());
            return OBJECT_MAPPER.nullNode();
        }
    }

    /**
     * Converte a árvore AST de RichText do Storyblok em texto plano formatado.
     */
    protected String extractRichText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return "";
        if (node.isTextual()) return node.asText();
        if (node.isArray()) return processArrayNode(node);
        if (node.isObject()) return processObjectNode(node);
        return "";
    }

    private String processArrayNode(JsonNode arrayNode) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode item : arrayNode) {
            sb.append(extractRichText(item));
        }
        return sb.toString();
    }

    private String processObjectNode(JsonNode node) {
        String type = node.path("type").asText("");

        if ("text".equals(type) || node.hasNonNull("text")) {
            return node.path("text").asText("");
        }
        if ("hard_break".equals(type)) {
            return "\n";
        }

        String childText = processArrayNode(node.path("content"));
        return formatBlockText(type, childText);
    }

    private String formatBlockText(String type, String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return "";

        return switch (type) {
            case "paragraph", "heading" -> trimmed + "\n\n";
            case "list_item" -> "- " + trimmed + "\n";
            default -> text;
        };
    }

    protected String extractFirstParagraph(JsonNode node) {
        String full = extractRichText(node).trim();
        if (full.isBlank()) return "";
        int splitIndex = full.indexOf("\n\n");
        return splitIndex >= 0 ? full.substring(0, splitIndex).trim() : full;
    }

    protected LocalDateTime parseStoryblokDate(JsonNode story) {
        String postDate = story.path("content").path("post_date").asText("").trim();
        LocalDateTime parsed = parseDateTimeString(postDate);
        if (parsed != null) return parsed;

        String firstPublished = story.path("first_published_at").asText("").trim();
        parsed = parseDateTimeString(firstPublished);
        return parsed != null ? parsed : LocalDateTime.now();
    }

    private LocalDateTime parseDateTimeString(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return dateStr.contains("T")
                    ? OffsetDateTime.parse(dateStr).toLocalDateTime()
                    : LocalDateTime.parse(dateStr, STORYBLOK_DATE_FORMATTER);
        } catch (Exception ignored) {
            return null;
        }
    }
}