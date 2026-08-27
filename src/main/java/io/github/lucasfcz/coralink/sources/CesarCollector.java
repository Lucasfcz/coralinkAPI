package io.github.lucasfcz.coralink.sources;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.sources.collector.WordPressCollector;
import org.springframework.stereotype.Component;

/**
 * Coletor oficial para notícias do CESAR (Centro de Estudos e Sistemas Avançados do Recife).
 * O painel WordPress do CESAR opera sob o caminho /painel, enquanto as URLs públicas são roteadas para /w/{slug}.
 */
@Component
public class CesarCollector extends WordPressCollector {


    private static final String BASE_URL = "https://www.cesar.org.br/painel";
    private static final String FALLBACK_IMAGE_URL = "https://www.cesar.org.br/painel/wp-content/uploads/2025/02/cesar-thumb-pt.jpg";
    private static final String POSTS_ENDPOINT = "https://www.cesar.org.br/painel/wp-json/wp/v2/posts?per_page=20";

    @Override
    protected String baseUrl() {
        return BASE_URL;
    }

    @Override
    protected String imageFallBackUrl() {
        return FALLBACK_IMAGE_URL;
    }

    @Override
    protected String postsEndpoint() {
        return POSTS_ENDPOINT;
    }

    @Override
    protected NewsSummary mapPost(JsonNode post) {
        NewsSummary summary = super.mapPost(post);
        if (summary == null) {
            return null;
        }

        String slug = post.path("slug").asText();
        String publicUrl = slug.isBlank() ? summary.url() : "https://www.cesar.org.br/w/" + slug;

        return new NewsSummary(
                summary.title(),
                summary.shortSummary(),
                publicUrl,
                summary.sourceName(),
                summary.foundAt()
        );
    }

    @Override
    public SourceName sourceName() {
        return SourceName.CESAR;
    }
}
