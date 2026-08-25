package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.sources.collector.WordPressCollector;
import org.springframework.stereotype.Component;

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
    public SourceName sourceName() {
        return SourceName.CESAR_SCHOOL;
    }
}
