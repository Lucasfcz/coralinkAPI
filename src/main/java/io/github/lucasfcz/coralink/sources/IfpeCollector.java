package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.sources.collector.WordPressCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class IfpeCollector extends WordPressCollector {

    private static final String BASE_URL = "https://portal.ifpe.edu.br";
    private static final List<String> CAMPUS = List.of("recife", "paulista", "olinda");

    @Override
    protected String baseUrl() {
        return BASE_URL;
    }

    @Override
    protected String imageFallBackUrl() {
        return "https://iconape.com/wp-content/png_logo_vector/instituto-federal-de-pernambuco-marca-horizontal-2015.png";
    }

    @Override
    public List<NewsSummary> collect() {
        List<NewsSummary> result = new ArrayList<>();

        for (String campus : campusPaths()) {
            String endpoint = endpointForCampus(campus);
            try {
                result.addAll(fetchPosts(endpoint));
            } catch (Exception exception) {
                log.warn("Failed to collect IFPE campus '{}'; skipping", campus, exception);
            }
        }
        return result;
    }

    protected List<String> campusPaths() {
        return CAMPUS;
    }

    protected String endpointForCampus(String campus) {
        return baseUrl() + "/" + campus + "/wp-json/wp/v2/posts?per_page=20&_embed=wp:featuredmedia";
    }

    @Override
    public SourceName sourceName() {
        return SourceName.IFPE;
    }
}
