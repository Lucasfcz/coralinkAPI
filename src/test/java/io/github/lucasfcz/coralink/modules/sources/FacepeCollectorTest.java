package io.github.lucasfcz.coralink.modules.sources;

import io.github.lucasfcz.coralink.modules.sources.dto.DetailedContent;
import io.github.lucasfcz.coralink.modules.sources.dto.NewsSummary;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FacepeCollectorTest {

    private FacepeCollector collector;

    @BeforeEach
    void setUp() {
        collector = new FacepeCollector();
    }

    @Test
    void testArticlesSelectionAndMapping() {
        String html = """
                <html>
                <body>
                    <div class="edital-conteudo">
                        <h5>
                            <a href="https://www.facepe.br/wp-content/uploads/2026/09/Edital_30.pdf"><span>30/2026 - </span></a>
                            <a href="https://www.facepe.br/wp-content/uploads/2026/09/Edital_30.pdf">Lançamento Edital nº 30/2026 PIBIC</a>
                        </h5>
                        <hr/>
                        Publicação: 9 de setembro de 2026
                    </div>
                    <div class="edital-conteudo sub-arquivo-edital">
                        <h5>
                            <a href="https://www.facepe.br/wp-content/uploads/2026/09/Errata.pdf">Errata 01</a>
                        </h5>
                    </div>
                </body>
                </html>
                """;

        Document doc = Jsoup.parse(html, "https://www.facepe.br/editais/");
        List<Element> articles = collector.articles(doc);
        assertEquals(1, articles.size());

        NewsSummary summary = collector.mapArticle(articles.get(0));
        assertNotNull(summary);
        assertTrue(summary.title().contains("30/2026"));
        assertTrue(summary.title().contains("PIBIC"));
        assertEquals("https://www.facepe.br/wp-content/uploads/2026/09/Edital_30.pdf", summary.url());
        assertEquals("FACEPE", summary.sourceName());
        assertEquals(LocalDateTime.of(2026, 9, 9, 0, 0, 0), summary.foundAt());

        DetailedContent detailed = collector.detailedCollect(summary.url());
        assertNotNull(detailed);
        assertTrue(detailed.fullContent().contains("Edital FACEPE"));
        assertTrue(detailed.fullContent().contains("PIBIC"));
    }

    @Test
    void testSubArquivosIgnored() {
        String html = """
                <html>
                <body>
                    <div class="edital-conteudo sub-arquivo-edital">
                        <h5>
                            <a href="https://www.facepe.br/errata.pdf">Errata</a>
                        </h5>
                    </div>
                </body>
                </html>
                """;

        Document doc = Jsoup.parse(html, "https://www.facepe.br/editais/");
        List<Element> articles = collector.articles(doc);
        assertTrue(articles.isEmpty());
    }

    @Test
    void testNullDocumentReturnsEmpty() {
        assertTrue(collector.articles(null).isEmpty());
    }
}
