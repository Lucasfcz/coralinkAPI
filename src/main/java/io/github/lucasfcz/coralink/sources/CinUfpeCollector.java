package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.sources.collector.WordPressCollector;
import org.springframework.stereotype.Component;

@Component
public class CinUfpeCollector extends WordPressCollector {

    private static final String BASE_URL = "https://portal.cin.ufpe.br";

    @Override
    protected String baseUrl() {
        return BASE_URL;
    }

    @Override
    protected String imageFallBackUrl() {
        return "https://imgs.search.brave.com/kubq3bDoGwCh3JGhSZYTVP6uSodLmv0-9vkZcdIp9zs/rs:fit:860:0:0:0/g:ce/aHR0cHM6Ly9wb3J0/YWwuY2luLnVmcGUu/YnIvd3AtY29udGVu/dC91cGxvYWRzLzIw/MjAvMDcvSG9yaXpv/bnRhbC1WZXJtZWxo/by1Mb2dvdGlwby1D/SW4tVUZQRS5wbmc";
    }

    @Override
    protected String postsEndpoint() {
        return BASE_URL + "/wp-json/wp/v2/posts?per_page=20&categories_exclude=1&_embed=wp:featuredmedia";
    }

    @Override
    public SourceName sourceName() {
        return SourceName.CIN_UFPE;
    }
}
