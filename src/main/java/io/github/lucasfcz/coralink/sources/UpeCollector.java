package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.exceptions.CollectException;
import io.github.lucasfcz.coralink.sources.collector.WordPressCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Coletor oficial para notícias da Universidade de Pernambuco (UPE).
 * Suporta fallback automático para o formato de rota nativo (?rest_route=/wp/v2/posts)
 * caso o endpoint padrão com reescrita (/wp-json/) falhe.
 */
@Slf4j
@Component
public class UpeCollector extends WordPressCollector {

    private static final String BASE_URL = "https://upe.br";

    @Override
    protected String baseUrl() {
        return BASE_URL;
    }

    @Override
    protected String imageFallBackUrl() {
        return "https://upe.br/wp-content/uploads/2020/09/cropped-Marca-UPE-Horizontal-1.png";
    }

    @Override
    public List<NewsSummary> collect() {
        try {
            return fetchPosts(postsEndpoint());
        } catch (CollectException httpsException) {
            String fallbackEndpoint = fallbackPostsEndpoint();
            log.warn("Endpoint primário da UPE falhou; tentando endpoint de contingência {}", fallbackEndpoint, httpsException);
            try {
                return fetchPosts(fallbackEndpoint);
            } catch (RuntimeException fallbackException) {
                log.warn("Endpoint de contingência da UPE também falhou", fallbackException);
                return List.of();
            }
        }
    }

    protected String fallbackPostsEndpoint() {
        return "https://upe.br/?rest_route=/wp/v2/posts&per_page=20";
    }

    @Override
    public SourceName sourceName() {
        return SourceName.UPE;
    }
}

