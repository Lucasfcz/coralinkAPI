package io.github.lucasfcz.coralink.modules.sources;

import io.github.lucasfcz.coralink.modules.sources.collector.WordPressCollector;
import org.springframework.stereotype.Component;

/**
 * Coletor oficial para notícias da faculdade CESAR School (graduação e pós-graduação).
 */
@Component
public class CesarSchoolCollector extends WordPressCollector {


    private static final String BASE_URL = "https://www.cesar.school";

    @Override
    protected String baseUrl() {
        return BASE_URL;
    }

    @Override
    protected String imageFallBackUrl() {
        return "https://www.cesar.school/wp-content/themes/cesar/assets/images/logo.svg";
    }

    @Override
    public String sourceName() {
        return "CESAR_SCHOOL";
    }
}
