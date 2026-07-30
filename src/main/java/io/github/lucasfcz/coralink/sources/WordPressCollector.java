package io.github.lucasfcz.coralink.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lucasfcz.coralink.dto.NewsSummary;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Slf4j
public abstract class WordPressCollector extends AbstractCollector {

    private final ObjectMapper mapper = new ObjectMapper();

    protected WordPressCollector(SourceTypeDetector detector) {
        super(detector);
    }

    protected abstract String postsEndpoint();

    @Override
    protected List<NewsSummary> collectWordPress() {
        try {
            JsonNode posts = mapper.readTree(org.jsoup.Jsoup
                                    .connect(postsEndpoint())
                                    .ignoreContentType(true)
                                    .execute()
                                    .body()
            );

            List<NewsSummary> result = new ArrayList<>();

            for(JsonNode post : posts){

                result.add(new NewsSummary(
                                post.path("title").path("rendered").asText(),
                                cleanHtml(post.path("excerpt").path("rendered").asText()),
                                post.path("link").asText(),
                                sourceName(),
                                LocalDateTime.now()
                        ));
            }

            return result;
        } catch(Exception e){
            throw new RuntimeException("WordPress collection failed", e);
        }
    }

    private String cleanHtml(String value){
        return value.replaceAll("<[^>]+>", "")
                .trim();
    }
}