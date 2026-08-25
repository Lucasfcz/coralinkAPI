package io.github.lucasfcz.coralink.sources;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;

public class DetailedDebugger {

    private static final String DEFAULT_URL =
            "https://www.portodigital.org/noticias/rec-n-play-capitulo-saude-conecta-especialistas-startups-e-lideres-para-debater-o-futuro-da-saude-no-brasil";

    private static final int TIMEOUT_MILLIS = 15000;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    public static void main(String[] args) throws Exception {
        String targetUrl = (args != null && args.length > 0) ? args[0] : DEFAULT_URL;
        debug(targetUrl);
    }

    @Test
    void testDefaultUrlDebug() throws Exception {
        debug(DEFAULT_URL);
    }

    public static void debug(String url) throws IOException {
        System.out.println("==================================================");
        System.out.println("DETAILED COLLECTOR DEBUGGER");
        System.out.println("==================================================");
        System.out.println("Target URL: " + url);
        System.out.println();

        configureResilientSsl();

        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MILLIS)
                .followRedirects(true)
                .get();

        Path folder = Path.of("debug", "detailed");
        Files.createDirectories(folder);
        Files.writeString(folder.resolve("page.html"), doc.outerHtml());

        System.out.println("HTML bruto salvo em: " + folder.resolve("page.html").toAbsolutePath());
        System.out.println();

        // 1. Metadados Gerais
        System.out.println("--- 1. METADADOS DO DOCUMENTO ---");
        System.out.println("Title: " + doc.title());
        System.out.println("meta[property=og:title]: " + getMeta(doc, "property", "og:title"));
        System.out.println("meta[property=og:description]: " + getMeta(doc, "property", "og:description"));
        System.out.println("meta[name=description]: " + getMeta(doc, "name", "description"));
        System.out.println("meta[property=og:image]: " + getMeta(doc, "property", "og:image"));
        System.out.println("meta[name=twitter:image]: " + getMeta(doc, "name", "twitter:image"));
        System.out.println();

        // 2. Análise de Seletores de Conteúdo
        System.out.println("--- 2. INSPEÇÃO DE SELETORES DE CONTEÚDO ---");
        List<String> candidateSelectors = List.of(
                ".asset-full-content",
                ".asset-content",
                "article .entry-content",
                ".entry-content",
                ".post-content",
                ".elementor-widget-theme-post-content",
                ".elementor-post__content",
                ".elementor-widget-container",
                ".news-content",
                ".noticia-corpo",
                ".conteudo-noticia",
                "main article",
                "article",
                "main",
                ".journal-content-article"
        );

        for (String sel : candidateSelectors) {
            Elements matched = doc.select(sel);
            if (!matched.isEmpty()) {
                String cleaned = cleanContent(matched.first());
                System.out.printf("Seletor '%s' -> Elementos: %d | Texto Limpo: %d caracteres%n",
                        sel, matched.size(), cleaned.length());
                if (!cleaned.isEmpty()) {
                    System.out.println("  Amostra: " + cleaned.substring(0, Math.min(150, cleaned.length())) + "...");
                }
            }
        }
        System.out.println();

        // 3. Extração Global de Parágrafos
        System.out.println("--- 3. EXTRAÇÃO DE PARÁGRAFOS (<p>) ---");
        Elements paragraphs = doc.select("p");
        System.out.println("Total de tags <p> encontradas: " + paragraphs.size());
        StringBuilder allParagraphs = new StringBuilder();
        for (Element p : paragraphs) {
            String text = p.text().trim();
            if (text.length() > 20
                    && !text.contains("Copyright")
                    && !text.contains("CNPJ")
                    && !text.contains("Todos os direitos reservados")) {
                if (!allParagraphs.isEmpty()) allParagraphs.append("\n\n");
                allParagraphs.append(text);
            }
        }
        System.out.println("Tamanho do texto consolidado de parágrafos: " + allParagraphs.length() + " caracteres");
        System.out.println();

        // 4. Imagens Identificadas
        System.out.println("--- 4. IMAGENS IDENTIFICADAS ---");
        String ogImage = getMeta(doc, "property", "og:image");
        if (ogImage.isBlank()) ogImage = getMeta(doc, "name", "twitter:image");
        System.out.println("Imagem OpenGraph / Twitter: " + (ogImage.isBlank() ? "NENHUMA" : ogImage));

        Elements imgs = doc.select("img");
        System.out.println("Total de tags <img> encontradas: " + imgs.size());
        int imgIndex = 1;
        for (Element img : imgs) {
            String src = img.attr("src");
            String dataSrc = img.attr("data-src");
            String dataLazy = img.attr("data-lazy-src");
            String absSrc = img.absUrl("src");
            if (absSrc.isBlank() && !dataSrc.isBlank()) absSrc = img.absUrl("data-src");
            if (absSrc.isBlank() && !dataLazy.isBlank()) absSrc = img.absUrl("data-lazy-src");

            if (!absSrc.isBlank() && !absSrc.startsWith("data:")) {
                System.out.printf("  [%d] src: %s | resolved: %s%n", imgIndex++, src, absSrc);
            }
        }
        System.out.println();

        // 5. Simulação do Conteúdo Final Enviado para IA
        System.out.println("--- 5. SIMULAÇÃO DO CONTEÚDO PARA IA (EXTRACTION) ---");
        String finalContent = allParagraphs.toString();
        System.out.println("Caracteres para IA: " + finalContent.length());
        Files.writeString(folder.resolve("extracted_content.txt"), finalContent);
        System.out.println("Texto completo salvo em: " + folder.resolve("extracted_content.txt").toAbsolutePath());
        System.out.println("==================================================");
    }

    private static String getMeta(Document doc, String attrKey, String attrVal) {
        Element el = doc.selectFirst("meta[" + attrKey + "=" + attrVal + "]");
        return el != null ? el.attr("content").trim() : "";
    }

    private static String cleanContent(Element element) {
        if (element == null) return "";
        Element clone = element.clone();
        clone.select("header, footer, nav, aside, script, style, .menu, .navbar, .breadcrumb, .share, .social, .related, .newsletter, .comments").remove();
        return clone.text().trim();
    }

    private static void configureResilientSsl() {
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
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception ignored) {}
    }
}
