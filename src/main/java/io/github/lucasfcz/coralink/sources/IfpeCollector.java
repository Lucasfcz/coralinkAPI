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
    public List<NewsSummary> collect() {
        List<NewsSummary> result = new ArrayList<>();

        for (String campus : CAMPUS) {
            String endpoint = BASE_URL + "/" + campus + "/wp-json/wp/v2/posts?per_page=20&_embed=wp:featuredmedia";
            try {
                result.addAll(fetchPosts(endpoint));
            } catch (Exception exception) {
                log.warn("Failed to collect IFPE campus '{}'; skipping", campus, exception);
            }
        }
        return result;
    }

    @Override
    public SourceName sourceName() {
        return SourceName.IFPE;
    }
}
