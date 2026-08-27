package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.enums.SourceName;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UfpeCollectorTest {

    private UfpeCollector collector;

    @BeforeEach
    void setUp() {
        collector = new UfpeCollector();
    }

    @Test
    void testArticlesSelectionAndMapping() {
        String html = """
                <html>
                <body>
                    <div class="list-full-content__item">
                        <div class="list-full-content__metadata">
                            <span class="list-full-content__date"><i class="icone-calendar"></i> 26/08/2026</span>
                            <span class="list-full-content__hour"><i class="icone-clock-o"></i> 11:24:00</span>
                        </div>
                        <div class="list-full-content__content">
                            <h3 class="list-full-content__title">
                                <a href="http://www.ufpe.br/ascom/noticias/-/asset_publisher/test/content/test-article/40615">
                                    Título da Notícia UFPE
                                </a>
                            </h3>
                            <div class="list-full-content__sumary">
                                Resumo da notícia da UFPE para testes.
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """;

        Document doc = Jsoup.parse(html, "https://www.ufpe.br/ascom/noticias");
        List<Element> articles = collector.articles(doc);
        assertEquals(1, articles.size());

        NewsSummary summary = collector.mapArticle(articles.get(0));
        assertNotNull(summary);
        assertEquals("Título da Notícia UFPE", summary.title());
        assertEquals("Resumo da notícia da UFPE para testes.", summary.shortSummary());
        assertEquals("http://www.ufpe.br/ascom/noticias/-/asset_publisher/test/content/test-article/40615", summary.url());
        assertEquals(SourceName.UFPE, summary.sourceName());
        assertEquals(LocalDateTime.of(2026, 8, 26, 0, 0, 0), summary.foundAt());
    }

    @Test
    void testSummaryFallbackToTitleWhenEmpty() {
        String html = """
                <html>
                <body>
                    <div class="list-full-content__item">
                        <div class="list-full-content__metadata">
                            <span class="list-full-content__date">26/08/2026</span>
                        </div>
                        <div class="list-full-content__content">
                            <h3 class="list-full-content__title">
                                <a href="http://www.ufpe.br/noticia-1">Título Sem Resumo</a>
                            </h3>
                            <div class="list-full-content__sumary">   </div>
                        </div>
                    </div>
                </body>
                </html>
                """;

        Document doc = Jsoup.parse(html, "https://www.ufpe.br/ascom/noticias");
        NewsSummary summary = collector.mapArticle(collector.articles(doc).get(0));
        assertNotNull(summary);
        assertEquals("Título Sem Resumo", summary.title());
        assertEquals("Título Sem Resumo", summary.shortSummary());
        assertEquals(LocalDateTime.of(2026, 8, 26, 0, 0, 0), summary.foundAt());
    }

    @Test
    void testNullDocumentReturnsEmptyList() {
        List<Element> articles = collector.articles(null);
        assertNotNull(articles);
        assertTrue(articles.isEmpty());
    }

    @Test
    void testInvalidCardReturnsNull() {
        String html = """
                <div class="list-full-content__item">
                    <div class="list-full-content__content">
                    </div>
                </div>
                """;
        Document doc = Jsoup.parse(html, "https://www.ufpe.br/ascom/noticias");
        NewsSummary summary = collector.mapArticle(collector.articles(doc).get(0));
        assertNull(summary);
    }
}
