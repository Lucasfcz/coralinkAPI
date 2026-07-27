package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.NewsSummary;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CinUfpeCollectorTest {

    @Test
    void extractsContentAndImageFromDocument() {
        CinUfpeCollector collector = new CinUfpeCollector();
        DetailedContent result = collector.extractDetailedContent(Jsoup.parse("""
                <html><head><meta property='og:image' content='https://images.example/opportunity.png'></head>
                <body><div class='colibri-post-content'><p>Primeiro parágrafo.</p><p>Segundo parágrafo.</p></div></body></html>
                """));

        assertEquals("Primeiro parágrafo. Segundo parágrafo.", result.fullContent());
        assertEquals("https://images.example/opportunity.png", result.imageUrl());
    }

    @Test
    void extractsAbsoluteUrlFromArticle() {
        CinUfpeCollector collector = new CinUfpeCollector();
        NewsSummary summary = collector.extractSummary(Jsoup.parse("""
                <article><h3><a href='/news/opportunity'>Oportunidade</a></h3><p>Resumo</p></article>
                """, "https://portal.cin.ufpe.br").selectFirst("article"));

        assertNotNull(summary);
        assertEquals("https://portal.cin.ufpe.br/news/opportunity", summary.url());
    }
}
