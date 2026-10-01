package io.github.lucasfcz.coralink.modules.sources;

import io.github.lucasfcz.coralink.modules.sources.collector.WordPressCollector;
import org.springframework.stereotype.Component;

/**
 * Coletor oficial para notícias, congressos, jornadas acadêmicas e projetos de extensão da
 * Faculdade Pernambucana de Saúde (FPS / complexo IMIP).
 * Referência no polo médico e de saúde do Recife (Medicina, Enfermagem, Odontologia, Farmácia, Fisioterapia, Nutrição e Psicologia).
 * Utiliza a API REST pública nativa do WordPress (/wp-json/wp/v2/posts).
 */
@Component
public class FpsCollector extends WordPressCollector {

    private static final String BASE_URL = "https://fps.edu.br";
    private static final String FALLBACK_IMAGE_URL = "https://fps.edu.br/wp-content/uploads/2023/07/Ativo-1-2.png";
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
        return "FPS";
    }
}
