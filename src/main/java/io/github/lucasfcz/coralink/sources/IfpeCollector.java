package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.sources.collector.WordPressCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Coletor oficial para notícias do Instituto Federal de Pernambuco (IFPE).
 * O portal do IFPE é particionado em multisite por campus (Recife, Olinda, Paulista).
 */
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
        return "https://cdn.direcaoconcursos.com.br/uploads/2025/08/ifpe.jpg";
    }

    @Override
    public List<NewsSummary> collect() {
        List<NewsSummary> result = new ArrayList<>();

        for (String campus : campusPaths()) {
            String endpoint = endpointForCampus(campus);
            try {
                result.addAll(fetchPosts(endpoint));
            } catch (Exception exception) {
                log.warn("Falha ao coletar notícias do campus IFPE '{}'; ignorando", campus, exception);
            }
        }
        return result;
    }


    protected List<String> campusPaths() {
        return CAMPUS;
    }

    protected String endpointForCampus(String campus) {
        return baseUrl() + "/" + campus + "/wp-json/wp/v2/posts?per_page=20";
    }

    @Override
    public String singlePostEndpoint(String slug, String url) {
        if (url != null) {
            for (String campus : campusPaths()) {
                if (url.contains("/" + campus + "/")) {
                    return baseUrl() + "/" + campus + "/wp-json/wp/v2/posts?slug=" + slug;
                }
            }
        }
        return super.singlePostEndpoint(slug, url);
    }

    @Override
    public SourceName sourceName() {
        return SourceName.IFPE;
    }
}
