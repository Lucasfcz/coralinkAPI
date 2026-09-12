package io.github.lucasfcz.coralink.modules.sources;

import io.github.lucasfcz.coralink.modules.sources.collector.WordPressCollector;
import org.springframework.stereotype.Component;

/**
 * Coletor oficial para notícias do Centro Universitário Frassinetti do Recife (UNIFAFIRE).
 */
@Component
public class UnifafireCollector extends WordPressCollector {


    private static final String BASE_URL = "https://unifafire.edu.br";

    @Override
    protected String baseUrl() {
        return BASE_URL;
    }

    @Override
    protected String imageFallBackUrl() {
        return "https://unifafire.edu.br/wp-content/uploads/2024/05/UNIFAFIREINST2024-2048x1153.webp";
    }


    @Override
    public String sourceName() {
        return "UNIFAFIRE";
    }
}
