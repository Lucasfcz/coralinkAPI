package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.exceptions.CollectException;
import io.github.lucasfcz.coralink.sources.collector.WordPressCollector;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

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
    public SourceName sourceName() {
        return SourceName.UNIFAFIRE;
    }
}
