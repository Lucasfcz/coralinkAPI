package io.github.lucasfcz.coralink.modules.sources;

import io.github.lucasfcz.coralink.modules.sources.collector.WordPressCollector;
import org.springframework.stereotype.Component;

/**
 * Coletor oficial para notícias, oportunidades de estágio, capacitações e editais
 * do Instituto Euvaldo Lodi de Pernambuco (IEL-PE / Sistema FIEPE).
 * Principal articulador entre o meio acadêmico universitário e o mercado industrial de PE
 * (programas de estágio remunerado, IEL Carreiras e bolsas Inova Talentos).
 * Utiliza o Custom Post Type 'noticia' da WP REST API nativa.
 */
@Component
public class IelCollector extends WordPressCollector {

    private static final String BASE_URL = "https://ielpe.org.br";
    private static final String FALLBACK_IMAGE_URL = "https://ielpe.org.br/wp-content/themes/cartello/img/logo-iel-active.png";
    private static final String POSTS_ENDPOINT = BASE_URL + "/wp-json/wp/v2/noticia?per_page=20";

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
    public String singlePostEndpoint(String slug, String url) {
        return BASE_URL + "/wp-json/wp/v2/noticia?slug=" + slug;
    }

    @Override
    public String sourceName() {
        return "IEL";
    }
}
