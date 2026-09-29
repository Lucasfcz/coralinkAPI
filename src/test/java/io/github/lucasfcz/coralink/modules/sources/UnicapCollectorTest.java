package io.github.lucasfcz.coralink.modules.sources;

import io.github.lucasfcz.coralink.modules.sources.dto.NewsSummary;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UnicapCollectorTest {

    private UnicapCollector collector;

    @BeforeEach
    void setUp() {
        collector = new UnicapCollector();
    }

    @Test
    void testBasicConfig() {
        assertEquals("UNICAP", collector.sourceName());
        assertTrue(collector.baseUrl().contains("portal.unicap.br"));
    }

    @Test
    void testArticlesSelectionAndMapping() {
        String html = """
                <html>
                <body>
                    <div class="card">
                        <h3 class="text-dark">Seminário Internacional de Inteligência Artificial e Sociedade</h3>
                        <div class="card-text">
                            <p>Debate sobre ética, novas tecnologias e direitos fundamentais.</p>
                        </div>
                        <a class="stretched-link" href="https://portal.unicap.br/-/seminario-internacional-ia">Ler Mais</a>
                    </div>
                </body>
                </html>
                """;

        Document doc = Jsoup.parse(html, "https://portal.unicap.br");
        List<Element> articles = collector.articles(doc);
        assertEquals(1, articles.size());

        NewsSummary summary = collector.mapArticle(articles.get(0));
        assertNotNull(summary);
        assertEquals("Seminário Internacional de Inteligência Artificial e Sociedade", summary.title());
        assertEquals("Debate sobre ética, novas tecnologias e direitos fundamentais.", summary.shortSummary());
        assertEquals("https://portal.unicap.br/-/seminario-internacional-ia", summary.url());
        assertEquals("UNICAP", summary.sourceName());
    }

    @Test
    void testNoiseFilter() {
        String html = """
                <html>
                <body>
                    <div class="card">
                        <h3 class="text-dark">Nota de Pesar - Comunidade Acadêmica</h3>
                        <a class="stretched-link" href="https://portal.unicap.br/-/nota-de-pesar-luto">Ler Mais</a>
                    </div>
                </body>
                </html>
                """;

        Document doc = Jsoup.parse(html, "https://portal.unicap.br");
        List<Element> articles = collector.articles(doc);
        NewsSummary summary = collector.mapArticle(articles.get(0));
        assertNull(summary);
    }
}
