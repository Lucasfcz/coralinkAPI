package io.github.lucasfcz.coralink.modules.sources;

import io.github.lucasfcz.coralink.modules.sources.collector.WordPressCollector;
import org.springframework.stereotype.Component;

/**
 * Coletor oficial para notícias, programações, workshops e chamadas do REC'n'Play.
 * O maior festival gratuito de tecnologia, inovação, cultura e criatividade do Brasil,
 * sediado no Porto Digital (Recife Antigo), com forte presença universitária (Arena Universitária e Hackathons).
 * Utiliza a API REST pública nativa do WordPress (/wp-json/wp/v2/posts).
 */
@Component
public class RecnplayCollector extends WordPressCollector {

    private static final String BASE_URL = "https://recnplay.pe";
    private static final String FALLBACK_IMAGE_URL = "https://recnplay.pe/wp-content/themes/recnplay/assets/images/logo.png";
    private static final String POSTS_ENDPOINT = BASE_URL + "/wp-json/wp/v2/posts?per_page=20";

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
    public String sourceName() {
        return "RECNPLAY";
    }
}
