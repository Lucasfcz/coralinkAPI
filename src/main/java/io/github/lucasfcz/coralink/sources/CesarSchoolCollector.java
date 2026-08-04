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
        return "https://imgs.search.brave.com/lTTqgDcrGu7-0mbKJSSrmpFNGZuIY8UE8pFGBfHV5Go/rs:fit:860:0:0:0/g:ce/aHR0cHM6Ly9jZG4u/YnJhbmRmZXRjaC5p/by9pZDFBWUNsZm0w/L3RoZW1lL2Rhcmsv/bG9nby5zdmc_Yz0x/YnhpZDY0TXVwN2Fj/emV3U0FZTVgmdD0x/NzY2NTIxMTgxMjMw";
    }

    @Override
    public SourceName sourceName() {
        return SourceName.CESAR_SCHOOL;
    }
}
