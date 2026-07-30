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
    public SourceName sourceName() {
        return SourceName.CESAR_SCHOOL;
    }
}
