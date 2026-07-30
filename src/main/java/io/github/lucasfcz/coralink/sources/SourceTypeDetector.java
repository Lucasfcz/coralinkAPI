package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.enums.CollectorType;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

@Component
@Slf4j
public class SourceTypeDetector {

    private static final int TIMEOUT_MILLIS = 10000;

    public CollectorType detect(String url) {
        if (hasWordPressApi(url)) {
            log.info("Detected WordPress source: {}", url);
            return CollectorType.WORDPRESS;
        }

        if (hasWordPressMarkers(url)) {
            log.info("Detected WordPress markers: {}", url);
            return CollectorType.WORDPRESS;
        }

        log.info("Detected HTML source: {}", url);
        return CollectorType.HTML;
    }


    private boolean hasWordPressApi(String url) {
        try {
            String api = url.replaceAll("/$", "") + "/wp-json/wp/v2/posts?per_page=1";
            Connection.Response response = Jsoup.connect(api)
                            .ignoreContentType(true)
                            .timeout(TIMEOUT_MILLIS)
                            .execute();

            return response.statusCode() == 200 && Objects.requireNonNull(response.contentType()).contains("json");

        } catch (IOException e) {
            return false;
        }
    }


    private boolean hasWordPressMarkers(String url) {
        try {
            String html = Jsoup.connect(url)
                            .timeout(TIMEOUT_MILLIS)
                            .get()
                            .html();

            return html.contains("wp-content")
                    || html.contains("wp-includes")
                    || html.contains("api.w.org");

        } catch (IOException e) {
            return false;
        }
    }
}