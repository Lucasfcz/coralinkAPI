package io.github.lucasfcz.coralink.sources;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.NewsSummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CollectorsIntegrationTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void cinUfpeCollectorCollectsAndExtractsDetails() throws Exception {
        server = startServer();
        String baseUrl = baseUrl();
        String endpoint = "/cin/wp-json/wp/v2/posts?per_page=20&categories_exclude=1&_embed=wp:featuredmedia";
        registerJson(endpoint, """
                [{
                  "title":{"rendered":"Workshop CIN"},
                  "excerpt":{"rendered":"<p>Resumo CIN</p>"},
                  "link":"%s/cin/post-1",
                  "date":"2026-08-03T08:51:39-03:00"
                }]
                """.formatted(baseUrl));
        registerHtml("/cin/post-1", """
                <html><head><meta property="og:image" content="%s/static/cin.png"></head>
                <body><article><div class="entry-content"><p>Conteudo completo CIN</p></div></article></body></html>
                """.formatted(baseUrl));

        CinUfpeCollector collector = new CinUfpeCollector() {
            @Override protected String baseUrl() { return baseUrl; }
            @Override protected String postsEndpoint() { return baseUrl + endpoint; }
        };

        List<NewsSummary> news = collector.collect();
        assertEquals(1, news.size());
        assertEquals("Workshop CIN", news.getFirst().title());

        DetailedContent detail = collector.detailedCollect(news.getFirst().url());
        assertNotNull(detail);
        assertTrue(detail.fullContent().contains("Conteudo completo CIN"));
        assertEquals(baseUrl + "/static/cin.png", detail.imageUrl());
    }

    @Test
    void cesarSchoolCollectorCollectsAndExtractsDetails() throws Exception {
        server = startServer();
        String baseUrl = baseUrl();
        String endpoint = "/cesar/wp-json/wp/v2/posts?per_page=20&_embed=wp:featuredmedia";
        registerJson(endpoint, """
                [{
                  "title":{"rendered":"Programa CESAR"},
                  "excerpt":{"rendered":"<p>Resumo CESAR</p>"},
                  "link":"%s/cesar/post-1",
                  "date":"2026-08-03T10:00:00-03:00"
                }]
                """.formatted(baseUrl));
        registerHtml("/cesar/post-1", """
                <html><head><meta property="og:image" content="%s/static/cesar.png"></head>
                <body><article><div class="entry-content"><p>Conteudo CESAR</p></div></article></body></html>
                """.formatted(baseUrl));

        CesarSchoolCollector collector = new CesarSchoolCollector() {
            @Override protected String baseUrl() { return baseUrl + "/cesar"; }
        };

        List<NewsSummary> news = collector.collect();
        assertEquals(1, news.size());
        DetailedContent detail = collector.detailedCollect(news.getFirst().url());
        assertNotNull(detail);
        assertTrue(detail.fullContent().contains("Conteudo CESAR"));
    }

    @Test
    void ifpeCollectorCollectsFromAllCampuses() throws Exception {
        server = startServer();
        String baseUrl = baseUrl();
        registerJson("/ifpe/recife/wp-json/wp/v2/posts?per_page=20&_embed=wp:featuredmedia", postJson(baseUrl + "/ifpe/recife/post-1", "Edital Recife"));
        registerJson("/ifpe/paulista/wp-json/wp/v2/posts?per_page=20&_embed=wp:featuredmedia", postJson(baseUrl + "/ifpe/paulista/post-1", "Edital Paulista"));
        registerJson("/ifpe/olinda/wp-json/wp/v2/posts?per_page=20&_embed=wp:featuredmedia", postJson(baseUrl + "/ifpe/olinda/post-1", "Edital Olinda"));
        registerHtml("/ifpe/recife/post-1", "<html><body><article><div class='entry-content'><p>Detalhe IFPE</p></div></article></body></html>");

        IfpeCollector collector = new IfpeCollector() {
            @Override protected String baseUrl() { return baseUrl + "/ifpe"; }
        };

        List<NewsSummary> news = collector.collect();
        assertEquals(3, news.size());
        assertTrue(news.stream().anyMatch(n -> n.title().contains("Recife")));

        DetailedContent detail = collector.detailedCollect(baseUrl + "/ifpe/recife/post-1");
        assertNotNull(detail);
        assertTrue(detail.fullContent().contains("Detalhe IFPE"));
    }

    @Test
    void ufpeCollectorParsesAssetPublisherMarkup() throws Exception {
        server = startServer();
        String baseUrl = baseUrl();
        registerHtml("/ufpe/ascom/noticias", """
                <html><body>
                  <div class="asset-abstract">
                    <a href="%s/ufpe/ascom/noticias/-/asset_publisher/O3Odar12gQTr/content/edital-1/40615">Edital UFPE</a>
                    <div class="asset-summary">Resumo UFPE</div>
                  </div>
                </body></html>
                """.formatted(baseUrl));
        registerHtml("/ufpe/ascom/noticias/-/asset_publisher/O3Odar12gQTr/content/edital-1/40615",
                "<html><body><main><article><div class='entry-content'><p>Detalhe UFPE</p></div></article></main></body></html>");

        UfpeCollector collector = new UfpeCollector() {
            @Override protected String baseUrl() { return baseUrl + "/ufpe"; }
            @Override protected String pageUrl() { return baseUrl + "/ufpe/ascom/noticias"; }
        };

        List<NewsSummary> news = collector.collect();
        assertEquals(1, news.size());
        assertEquals("Edital UFPE", news.getFirst().title());
        assertEquals("Resumo UFPE", news.getFirst().shortSummary());

        DetailedContent detail = collector.detailedCollect(news.getFirst().url());
        assertNotNull(detail);
        assertTrue(detail.fullContent().contains("Detalhe UFPE"));
    }

    @Test
    void upeCollectorFallsBackWhenPrimaryEndpointFails() throws Exception {
        server = startServer();
        String baseUrl = baseUrl();
        registerJson("/upe/fallback", postJson(baseUrl + "/upe/post-1", "Edital UPE"));
        registerHtml("/upe/post-1", "<html><body><article><div class='entry-content'><p>Detalhe UPE</p></div></article></body></html>");

        UpeCollector collector = new UpeCollector() {
            @Override protected String baseUrl() { return baseUrl + "/upe"; }
            @Override protected String postsEndpoint() { return baseUrl + "/upe/primary-invalid"; }
            @Override protected String fallbackPostsEndpoint() { return baseUrl + "/upe/fallback"; }
        };

        List<NewsSummary> news = collector.collect();
        assertEquals(1, news.size());
        DetailedContent detail = collector.detailedCollect(news.getFirst().url());
        assertNotNull(detail);
        assertTrue(detail.fullContent().contains("Detalhe UPE"));
    }

    @Test
    void portoDigitalCollectorCollectsAndExtractsDetails() throws Exception {
        server = startServer();
        String baseUrl = baseUrl();
        registerHtml("/porto/noticias", """
                <html><body>
                  <div class="card"><a href="/noticias/oportunidade-1">Oportunidade Porto</a><p>Resumo Porto</p></div>
                </body></html>
                """);
        registerHtml("/noticias/oportunidade-1",
                "<html><head><meta property='og:image' content='%s/static/porto.png'></head><body><main><article><div class='entry-content'><p>Detalhe Porto</p></div></article></main></body></html>"
                        .formatted(baseUrl));

        PortoDigitalCollector collector = new PortoDigitalCollector() {
            @Override protected String baseUrl() { return baseUrl; }
            @Override protected String pageUrl() { return baseUrl + "/porto/noticias"; }
        };

        List<NewsSummary> news = collector.collect();
        assertEquals(1, news.size());
        DetailedContent detail = collector.detailedCollect(news.getFirst().url());
        assertNotNull(detail);
        assertTrue(detail.fullContent().contains("Detalhe Porto"));
        assertEquals(baseUrl + "/static/porto.png", detail.imageUrl());
    }

    private HttpServer startServer() throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.start();
        return httpServer;
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private void registerJson(String pathAndQuery, String body) {
        register(pathAndQuery, "application/json", body);
    }

    private void registerHtml(String pathAndQuery, String body) {
        register(pathAndQuery, "text/html; charset=utf-8", body);
    }

    private void register(String pathAndQuery, String contentType, String body) {
        String[] parts = pathAndQuery.split("\\?", 2);
        String path = parts[0];
        String query = parts.length == 2 ? parts[1] : null;

        server.createContext(path, exchange -> {
            if (query != null && !query.equals(exchange.getRequestURI().getRawQuery())) {
                writeResponse(exchange, 404, "text/plain", "not found");
                return;
            }
            writeResponse(exchange, 200, contentType, body);
        });
    }

    private void writeResponse(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, payload.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(payload);
        }
    }

    private String postJson(String link, String title) {
        return """
                [{
                  "title":{"rendered":"%s"},
                  "excerpt":{"rendered":"<p>Resumo %s</p>"},
                  "link":"%s",
                  "date":"2026-08-03T08:51:39-03:00"
                }]
                """.formatted(title, title, link);
    }
}
