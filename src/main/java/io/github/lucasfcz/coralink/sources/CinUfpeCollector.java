package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.sources.collector.WordPressCollector;
import org.springframework.stereotype.Component;

/**
 * Coletor oficial para notícias do Centro de Informática da UFPE (CIn-UFPE).
 * Exclui a categoria 1 (geral/não categorizado) para priorizar eventos e editais acadêmicos.
 */
@Component
public class CinUfpeCollector extends WordPressCollector {


    private static final String BASE_URL = "https://portal.cin.ufpe.br";

    @Override
    protected String baseUrl() {
        return BASE_URL;
    }

    @Override
    protected String imageFallBackUrl() {
        return "https://portal.cin.ufpe.br/wp-content/uploads/2020/07/Horizontal-Vermelho-Logotipo-CIn-UFPE.png";
    }

    @Override
    protected String postsEndpoint() {
        return BASE_URL + "/wp-json/wp/v2/posts?per_page=20&categories_exclude=1";
    }

    @Override
    public SourceName sourceName() {
        return SourceName.CIN_UFPE;
    }
}
