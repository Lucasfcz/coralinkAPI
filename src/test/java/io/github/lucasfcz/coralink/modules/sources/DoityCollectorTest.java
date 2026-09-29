package io.github.lucasfcz.coralink.modules.sources;

import io.github.lucasfcz.coralink.modules.sources.dto.NewsSummary;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DoityCollectorTest {

    private DoityCollector collector;

    @BeforeEach
    void setUp() {
        collector = new DoityCollector();
    }

    @Test
    void testBasicConfig() {
        assertEquals("DOITY", collector.sourceName());
        assertTrue(collector.baseUrl().contains("doity.com.br"));
    }

    @Test
    void testArticlesSelectionAndMapping() {
        String html = """
                <html>
                <body>
                    <div class="event-grid">
                        <a href="https://doity.com.br/congresso-odontologia-pe" class="card">
                            <p class="text-xs">Odontologia</p>
                            <h2>I Congresso Pernambucano de Cirurgia e Traumatologia Bucomaxilofacial</h2>
                            <p class="text-sm">15 de nov. de 2026 · Recife, PE</p>
                        </a>
                        <a href="https://doity.com.br/blog/como-organizar-evento" class="nav-link">
                            <h2>Blog - Dicas</h2>
                        </a>
                    </div>
                </body>
                </html>
                """;

        Document doc = Jsoup.parse(html, "https://doity.com.br");
        List<Element> articles = collector.articles(doc);
        assertEquals(2, articles.size());

        NewsSummary summary = collector.mapArticle(articles.get(0));
        assertNotNull(summary);
        assertEquals("I Congresso Pernambucano de Cirurgia e Traumatologia Bucomaxilofacial", summary.title());
        assertTrue(summary.shortSummary().contains("[Odontologia]"));
        assertTrue(summary.shortSummary().contains("Recife, PE"));
        assertEquals("https://doity.com.br/congresso-odontologia-pe", summary.url());
        assertEquals("DOITY", summary.sourceName());

        // Blog post should be excluded
        NewsSummary blogSummary = collector.mapArticle(articles.get(1));
        assertNull(blogSummary);
    }
}
