package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.exceptions.CollectException;
import io.github.lucasfcz.coralink.sources.collector.WordPressCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

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
            log.warn("Primary UPE endpoint failed; retrying fallback endpoint {}", fallbackEndpoint, httpsException);
            try {
                return fetchPosts(fallbackEndpoint);
            } catch (RuntimeException fallbackException) {
                log.warn("Fallback UPE endpoint also failed", fallbackException);
                return List.of();
            }
        }
    }

    protected String fallbackPostsEndpoint() {
        return "https://upe.br/wp-json/wp/v2/posts?per_page=20&_embed=wp:featuredmedia";
    }

    @Override
    public SourceName sourceName() {
        return SourceName.UPE;
    }
}
