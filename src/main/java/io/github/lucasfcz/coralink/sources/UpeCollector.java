package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.sources.collector.WordPressCollector;
import org.springframework.stereotype.Component;

@Component
public class UpeCollector extends WordPressCollector {

    private static final String BASE_URL = "https://upe.br";

    @Override
    protected String baseUrl() {
        return BASE_URL;
    }

    @Override
    protected String imageFallBackUrl() {
        return "https://imgs.search.brave.com/Ae-T3SVYKJnXRfYI8MKd9MvL9SaQ_ewm00ttcMQBZKU/rs:fit:860:0:0:0/g:ce/aHR0cHM6Ly93d3cu/ZGlhcmlvZGVwZXJu/YW1idWNvLmNvbS5i/ci9fbWlkaWFzL2pw/Zy8yMDI1LzExLzE0/LzEyMDB4ODAwLzFf/MjAyNDEwMTYxNzMw/NDU3OTAzOThpXzcx/MjM3Ni03OTg3ODcu/anBlZw";
    }

    @Override
    public SourceName sourceName() {
        return SourceName.UPE;
    }
}
