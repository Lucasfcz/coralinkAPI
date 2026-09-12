package io.github.lucasfcz.coralink.modules.sources;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.lucasfcz.coralink.modules.sources.dto.DetailedContent;
import io.github.lucasfcz.coralink.modules.sources.dto.NewsSummary;
import io.github.lucasfcz.coralink.modules.sources.collector.StoryblokCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Coletor oficial para notícias e eventos do ecossistema do Porto Digital (Recife Antigo).
 * O portal do Porto Digital utiliza Nuxt.js/Vue (SPA) com Storyblok Headless CMS;
 * a coleta é realizada diretamente na CDN da API do Storyblok com token público de leitura.
 */
@Slf4j
@Component
public class PortoDigitalCollector extends StoryblokCollector {

    private static final String BASE_URL = "https://www.portodigital.org";
    private static final String FALLBACK_IMAGE_URL = "https://www.portodigital.org/_nuxt/img/logo.5417d9c.svg";

    private final String apiToken;

    public PortoDigitalCollector() {
        this(System.getenv("PORTO_DIGITAL_STORYBLOK_TOKEN"));
    }

    public PortoDigitalCollector(@Value("${coralink.sources.porto-digital.token:}") String apiToken) {
        if (apiToken == null || apiToken.isBlank()) {
            apiToken = System.getenv("PORTO_DIGITAL_STORYBLOK_TOKEN");
        }
        this.apiToken = (apiToken != null && !apiToken.isBlank()) ? apiToken.trim() : null;
    }

    @Override
    public List<NewsSummary> collect() {
        if (apiToken == null || apiToken.isBlank()) {
            log.warn("Token da CDN do Storyblok para Porto Digital ausente (defina 'PORTO_DIGITAL_STORYBLOK_TOKEN' no .env). Coleta do Porto Digital ignorada.");
            return List.of();
        }
        return super.collect();
    }

    @Override
    protected String baseUrl() {
        return BASE_URL;
    }

    @Override
    protected String imageFallBackUrl() {
        return FALLBACK_IMAGE_URL;
    }

    @Override
    protected String apiToken() {
        return this.apiToken;
    }

    @Override
    protected String storiesEndpoint() {
        return "https://api.storyblok.com/v1/cdn/stories?token=" + apiToken()
                + "&version=published&starts_with=noticias&sort_by=content.post_date:desc&per_page=20";
    }

    @Override
    protected String singleStoryEndpointTemplate() {
        return "https://api.storyblok.com/v1/cdn/stories/noticias/%s?token=%s&version=published";
    }

    @Override
    public String sourceName() {
        return "PORTO_DIGITAL";
    }

    @Override
    public DetailedContent detailedCollect(String url) {
        if (apiToken == null || apiToken.isBlank()) {
            return null;
        }
        return super.detailedCollect(url);
    }

    @Override
    protected NewsSummary mapStory(JsonNode story) {
        String slug = story.path("slug").asText("").trim();
        String fullSlug = story.path("full_slug").asText("").trim();

        String canonicalUrl;
        if (!slug.isBlank()) {
            canonicalUrl = BASE_URL + "/noticias/" + slug.replaceAll("^/+", "");
        } else if (!fullSlug.isBlank()) {
            canonicalUrl = BASE_URL + "/" + fullSlug.replaceAll("^/+", "");
        } else {
            return null;
        }

        JsonNode content = story.path("content");
        String title = content.path("title").asText("").trim();
        if (title.isBlank()) {
            title = story.path("name").asText("").trim();
        }
        if (title.isBlank()) {
            return null;
        }

        String summary = content.path("lead").asText("").trim();
        if (summary.isBlank()) {
            summary = content.path("summary").asText("").trim();
        }
        if (summary.isBlank()) {
            summary = extractFirstParagraph(content.path("long_text"));
        }
        if (summary.isBlank()) {
            summary = title;
        }

        LocalDateTime publishedDate = parseStoryblokDate(story);

        return new NewsSummary(title, summary, canonicalUrl, sourceName(), publishedDate);
    }
}