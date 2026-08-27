package io.github.lucasfcz.coralink.sources;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.sources.collector.StoryblokCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

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

    @Value("${coralink.sources.porto-digital.token:Tgr62GfKdVlaQrQerwtKtgtt}")
    private String apiToken;

    public PortoDigitalCollector() {
        this.apiToken = "Tgr62GfKdVlaQrQerwtKtgtt";
    }

    public PortoDigitalCollector(String apiToken) {
        this.apiToken = apiToken;
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
    public SourceName sourceName() {
        return SourceName.PORTO_DIGITAL;
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