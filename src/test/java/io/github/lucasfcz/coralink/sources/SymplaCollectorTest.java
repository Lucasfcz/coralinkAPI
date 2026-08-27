package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.enums.SourceName;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SymplaCollectorTest {

    private SymplaCollector collector;

    @BeforeEach
    void setUp() {
        collector = new SymplaCollector();
    }

    @Test
    void testSourceName() {
        assertEquals(SourceName.SYMPLA, collector.sourceName());
    }

    @Test
    void testCollectorInitialization() {
        assertNotNull(collector.baseUrl());
        assertNotNull(collector.imageFallBackUrl());
    }

    @Test
    void testArticlesSelectionAndMapping() {
        String html = """
                <html>
                <body>
                    <a href="https://www.sympla.com.br/evento/workshop-ia/12345?src=home#section1" data-name="Workshop de IA">
                        <div class="event-card">
                            <h3>Workshop de IA em Recife</h3>
                            <p>Um evento incrível sobre Inteligência Artificial.</p>
                        </div>
                    </a>
                </body>
                </html>
                """;

        Document doc = Jsoup.parse(html, "https://www.sympla.com.br");
        List<Element> cards = collector.articles(doc);
        assertEquals(1, cards.size());

        NewsSummary summary = collector.mapArticle(cards.get(0));
        assertNotNull(summary);
        assertEquals("Workshop de IA", summary.title());
        assertEquals("https://www.sympla.com.br/evento/workshop-ia/12345", summary.url());
        assertEquals(SourceName.SYMPLA, summary.sourceName());
        assertNotNull(summary.shortSummary());
        assertNotNull(summary.foundAt());
    }

    @Test
    void testCardWithoutDataNameUsesTitleElement() {
        String html = """
                <a href="/evento/meetup-tech/67890">
                    <h2 class="event-title">Meetup Tech PE</h2>
                    <p>Descrição do evento de tecnologia.</p>
                </a>
                """;

        Document doc = Jsoup.parse(html, "https://www.sympla.com.br");
        List<Element> cards = collector.articles(doc);
        assertEquals(1, cards.size());

        NewsSummary summary = collector.mapArticle(cards.get(0));
        assertNotNull(summary);
        assertEquals("Meetup Tech PE", summary.title());
        assertEquals("https://www.sympla.com.br/evento/meetup-tech/67890", summary.url());
    }

    @Test
    void testNullDocumentReturnsEmptyList() {
        List<Element> articles = collector.articles(null);
        assertNotNull(articles);
        assertTrue(articles.isEmpty());
    }

    @Test
    void testInvalidCardReturnsNull() {
        String html = "<a href=\"\"></a>";
        Document doc = Jsoup.parse(html, "https://www.sympla.com.br");
        NewsSummary summary = collector.mapArticle(doc.selectFirst("a"));
        assertNull(summary);
    }
}
