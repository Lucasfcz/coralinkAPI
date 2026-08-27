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

class UnibraCollectorTest {

    private UnibraCollector collector;

    @BeforeEach
    void setUp() {
        collector = new UnibraCollector();
    }

    @Test
    void testArticlesSelectionAndMapping() {
        String html = """
                <html>
                <body>
                    <section class="sec-produtos">
                        <article class="single_pdt" data-inicio="2026-09-05 08:00:00">
                            <div class="infos">
                                <h1><a href="https://eventos.grupounibra.com/curso/teste-curso">Curso Teste UNIBRA</a></h1>
                                <p class="single_pdt_desc">Descrição do curso de teste.</p>
                            </div>
                        </article>
                    </section>
                </body>
                </html>
                """;

        Document doc = Jsoup.parse(html, "https://eventos.grupounibra.com/buscar/todos");
        List<Element> articles = collector.articles(doc);
        assertEquals(1, articles.size());

        NewsSummary summary = collector.mapArticle(articles.get(0));
        assertNotNull(summary);
        assertEquals("Curso Teste UNIBRA", summary.title());
        assertEquals("Descrição do curso de teste.", summary.shortSummary());
        assertEquals("https://eventos.grupounibra.com/curso/teste-curso", summary.url());
        assertEquals(SourceName.UNIBRA, summary.sourceName());
        assertNotNull(summary.foundAt());
    }

    @Test
    void testSummaryFallbackToTitleWhenNoDescription() {
        String html = """
                <article class="single_pdt">
                    <div class="infos">
                        <h1><a href="https://eventos.grupounibra.com/curso/sem-desc">Curso Sem Descrição</a></h1>
                    </div>
                </article>
                """;

        Document doc = Jsoup.parse(html, "https://eventos.grupounibra.com/buscar/todos");
        List<Element> articles = collector.articles(doc);
        NewsSummary summary = collector.mapArticle(articles.get(0));

        assertNotNull(summary);
        assertEquals("Curso Sem Descrição", summary.title());
        assertEquals("Curso Sem Descrição", summary.shortSummary());
    }

    @Test
    void testInvalidCardReturnsNull() {
        String html = "<article class=\"single_pdt\"><div class=\"infos\"></div></article>";
        Document doc = Jsoup.parse(html, "https://eventos.grupounibra.com/buscar/todos");
        List<Element> articles = collector.articles(doc);
        NewsSummary summary = collector.mapArticle(articles.get(0));
        assertNull(summary);
    }
}
