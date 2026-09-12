package io.github.lucasfcz.coralink.modules.sources;

import io.github.lucasfcz.coralink.modules.sources.collector.WordPressCollector;
import org.springframework.stereotype.Component;

/**
 * Coletor oficial para notícias, workshops e cursos da Faculdade Senac Pernambuco.
 * Utiliza a API REST pública nativa do WordPress (/wp-json/wp/v2/posts).
 */
@Component
public class SenacPeCollector extends WordPressCollector {

    private static final String BASE_URL = "https://faculdadesenacpe.edu.br";
    private static final String FALLBACK_IMAGE = "https://faculdadesenacpe.edu.br/wp-content/themes/senac/images/logo.png";
    private static final String POSTS_ENDPOINT = BASE_URL + "/wp-json/wp/v2/posts?per_page=20";

    @Override
    protected String baseUrl() {
        return BASE_URL;
    }

    @Override
    protected String imageFallBackUrl() {
        return FALLBACK_IMAGE;
    }

    @Override
    protected String postsEndpoint() {
        return POSTS_ENDPOINT;
    }

    @Override
    public String sourceName() {
        return "SENAC_PE";
    }
}
