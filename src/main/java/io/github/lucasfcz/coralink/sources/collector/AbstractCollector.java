package io.github.lucasfcz.coralink.sources.collector;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.NewsSummary;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Classe base para todos os coletores de notícias e oportunidades do Coralink.
 * Centraliza configurações de rede HTTP, tolerância a certificados SSL legados/autoassinados
 * (comum em portais governamentais e universitários de PE) e heurísticas de extração de texto e imagens.
 */
@Slf4j
public abstract class AbstractCollector implements Collector {

    protected static final int TIMEOUT_MILLIS = 15000;
    protected static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    // Fábrica SSL tolerante para evitar falhas de handshake em portais com cadeias de certificados incompletas (ex: UFPE)
    protected static final SSLSocketFactory RESILIENT_SSL_SOCKET_FACTORY = createResilientSslSocketFactory();

    protected abstract String baseUrl();

    protected abstract String imageFallBackUrl();

    @Override
    public String fallbackImageUrl() {
        return imageFallBackUrl();
    }

    @Override
    public abstract List<NewsSummary> collect();

    @Override
    public abstract DetailedContent detailedCollect(String url);

    /**
     * Realiza a requisição HTTP GET usando Jsoup com headers modernos de navegador e SSL resiliente.
     */
    protected Document requestDocument(String url) {
        try {
            return Jsoup.connect(url)
                    .sslSocketFactory(RESILIENT_SSL_SOCKET_FACTORY)
                    .timeout(TIMEOUT_MILLIS)
                    .userAgent(USER_AGENT)
                    .referrer(baseUrl())
                    .followRedirects(true)
                    .get();
        } catch (IOException e) {
            log.error("Falha ao requisitar URL: {}", url, e);
            return null;
        }
    }

    /**
     * Extrai texto limpo de um elemento HTML, removendo cabeçalhos, rodapés, scripts e elementos de ruído.
     */
    protected String extractSummary(Element content) {
        if (content == null) return "";
        Element working = content.clone();

        working.select(
                "header," +
                        "footer," +
                        "nav," +
                        "aside," +
                        "script," +
                        "style," +
                        ".menu," +
                        ".navbar," +
                        ".breadcrumb," +
                        ".breadcrumbs," +
                        ".share," +
                        ".social," +
                        ".related," +
                        ".newsletter," +
                        ".comments"
        ).remove();

        return working.text().trim();
    }

    /**
     * Utilitário comum para extrair o slug final de uma URL limpa (sem query params ou hash).
     */
    protected String extractSlug(String url) {
        if (url == null || url.isBlank()) return "";
        String clean = url.split("\\?")[0].split("#")[0].replaceAll("/+$", "");
        int lastSlash = clean.lastIndexOf('/');
        return lastSlash >= 0
                ? clean.substring(lastSlash + 1)
                : clean;
    }

    /**
     * Cria um SSLSocketFactory permissivo para contornar problemas de certificados SSL autoassinados
     * ou cadeias intermediárias ausentes em servidores de instituições públicas.
     */
    private static SSLSocketFactory createResilientSslSocketFactory() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            log.error("Falha ao inicializar fábrica de conexões SSL resiliente", e);
            return (SSLSocketFactory) SSLSocketFactory.getDefault();
        }
    }
}