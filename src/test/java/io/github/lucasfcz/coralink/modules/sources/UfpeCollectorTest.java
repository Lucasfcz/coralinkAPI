package io.github.lucasfcz.coralink.modules.sources;

import io.github.lucasfcz.coralink.modules.sources.dto.NewsSummary;
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
    void testProReitoriasPathsContainsAllFourKeyProReitorias() {
        List<String> paths = collector.proReitoriasPaths();
        assertNotNull(paths);
        assertEquals(4, paths.size());
        assertTrue(paths.contains("/propesqi"), "Deve incluir PROPESQI");
        assertTrue(paths.contains("/propg"), "Deve incluir PROPG");
        assertTrue(paths.contains("/proexc"), "Deve incluir PROEXC");
        assertTrue(paths.contains("/proaes"), "Deve incluir PROAES");
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
                                <a href="http://www.ufpe.br/propesqi/-/asset_publisher/test/content/edital-pibic-2026/40615">
                                    Edital PIBIC UFPE 2026/2027
                                </a>
                            </h3>
                            <div class="list-full-content__sumary">
                                Inscrições abertas para bolsas de iniciação científica PROPESQI.
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """;

        Document doc = Jsoup.parse(html, "https://www.ufpe.br/propesqi");
        List<Element> articles = collector.articles(doc);
        assertEquals(1, articles.size());

        NewsSummary summary = collector.mapArticle(articles.get(0));
        assertNotNull(summary);
        assertEquals("Edital PIBIC UFPE 2026/2027", summary.title());
        assertEquals("Inscrições abertas para bolsas de iniciação científica PROPESQI.", summary.shortSummary());
        assertEquals("https://www.ufpe.br/propesqi/-/asset_publisher/test/content/edital-pibic-2026/40615", summary.url());
        assertEquals("UFPE", summary.sourceName());
        assertEquals(LocalDateTime.of(2026, 8, 26, 0, 0, 0), summary.foundAt());
    }

    @Test
    void testFallbackSelectorsForProReitoriasArticles() {
        String html = """
                <html>
                <body>
                    <div class="asset-abstract">
                        <h2 class="asset-title">
                            <a href="https://www.ufpe.br/propg/edital-mestrado">Edital de Seleção para Mestrado 2027</a>
                        </h2>
                        <span class="asset-date">15/09/2026</span>
                        <p class="asset-summary">Vagas abertas para programas de pós-graduação stricto sensu.</p>
                    </div>
                    <div class="asset-abstract">
                        <h2 class="asset-title">
                            <a href="https://www.ufpe.br/proexc/bolsas-extensao">Programa de Extensão Cultural UFPE</a>
                        </h2>
                        <span class="asset-date">10/09/2026</span>
                        <p class="asset-summary">Seleção de bolsistas extensionistas para projetos artísticos.</p>
                    </div>
                </body>
                </html>
                """;

        Document doc = Jsoup.parse(html, "https://www.ufpe.br/propg");
        List<Element> articles = collector.articles(doc);
        assertEquals(2, articles.size());

        NewsSummary summary1 = collector.mapArticle(articles.get(0));
        assertNotNull(summary1);
        assertEquals("Edital de Seleção para Mestrado 2027", summary1.title());
        assertEquals("https://www.ufpe.br/propg/edital-mestrado", summary1.url());

        NewsSummary summary2 = collector.mapArticle(articles.get(1));
        assertNotNull(summary2);
        assertEquals("Programa de Extensão Cultural UFPE", summary2.title());
        assertEquals("https://www.ufpe.br/proexc/bolsas-extensao", summary2.url());
    }

    @Test
    void testResilientCollectAggregationAcrossProReitorias() {
        UfpeCollector testCollector = new UfpeCollector() {
            @Override
            protected Document requestDocument(String url) {
                if (url.endsWith("/propesqi")) {
                    return Jsoup.parse("""
                            <div class="list-full-content__item">
                                <h3 class="list-full-content__title"><a href="https://www.ufpe.br/propesqi/pibic">PIBIC</a></h3>
                                <div class="list-full-content__sumary">Bolsas de Pesquisa</div>
                            </div>
                            """, url);
                } else if (url.endsWith("/proaes")) {
                    return Jsoup.parse("""
                            <div class="list-full-content__item">
                                <h3 class="list-full-content__title"><a href="https://www.ufpe.br/proaes/auxilio">Auxílio Alimentação</a></h3>
                                <div class="list-full-content__sumary">Edital de permanência</div>
                            </div>
                            """, url);
                } else if (url.endsWith("/propg")) {
                    return null;
                } else {
                    throw new RuntimeException("Connection timeout");
                }
            }
        };

        List<NewsSummary> summaries = testCollector.collect();
        assertNotNull(summaries);
        assertEquals(2, summaries.size(), "Deve conter os 2 itens das pró-reitorias que responderam");
        assertEquals("PIBIC", summaries.get(0).title());
        assertEquals("Auxílio Alimentação", summaries.get(1).title());
    }

    @Test
    void testInstitutionalNoiseIsFilteredOut() {
        String html = """
                <html>
                <body>
                    <div class="list-full-content__item">
                        <div class="list-full-content__content">
                            <h3 class="list-full-content__title">
                                <a href="http://www.ufpe.br/proaes/nota-de-pesar">
                                    Nota de Pesar pelo falecimento do professor
                                </a>
                            </h3>
                            <div class="list-full-content__sumary">
                                A comunidade lamenta o falecimento.
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """;

        Document doc = Jsoup.parse(html, "https://www.ufpe.br/proaes");
        NewsSummary summary = collector.mapArticle(collector.articles(doc).get(0));
        assertNull(summary);
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
                                <a href="http://www.ufpe.br/propesqi/edital-1">Título Sem Resumo</a>
                            </h3>
                            <div class="list-full-content__sumary">   </div>
                        </div>
                    </div>
                </body>
                </html>
                """;

        Document doc = Jsoup.parse(html, "https://www.ufpe.br/propesqi");
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
        Document doc = Jsoup.parse(html, "https://www.ufpe.br/propesqi");
        NewsSummary summary = collector.mapArticle(collector.articles(doc).get(0));
        assertNull(summary);
    }
}
