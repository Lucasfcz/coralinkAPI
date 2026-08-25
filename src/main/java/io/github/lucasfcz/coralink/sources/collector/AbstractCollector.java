package io.github.lucasfcz.coralink.sources.collector;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.NewsSummary;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;

@Slf4j
public abstract class AbstractCollector implements Collector {

    private static final int TIMEOUT_MILLIS = 15000;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    protected static final SSLSocketFactory RESILIENT_SSL_SOCKET_FACTORY = createResilientSslSocketFactory();

    protected abstract String baseUrl();

    protected abstract String imageFallBackUrl();

    @Override
    public abstract List<NewsSummary> collect();

    @Override
    public abstract DetailedContent detailedCollect(String url);

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
            log.error("Failed to fetch URL: {}", url, e);
            return null;
        }
    }

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

    protected String extractImage(Document document, Element content) {
        if (document != null) {
            Element ogImage = document.selectFirst("meta[property=og:image], meta[name=twitter:image]");
            if (ogImage != null) {
                String imgUrl = ogImage.hasAttr("content") ? ogImage.attr("content").trim() : "";
                if (!imgUrl.isBlank() && !imgUrl.startsWith("data:")) {
                    return ogImage.absUrl("content").isBlank() ? imgUrl : ogImage.absUrl("content");
                }
            }
        }

        if (content != null) {
            Elements images = content.select("img");
            for (Element img : images) {
                String candidate = resolveImageSource(img);
                if (candidate != null && !candidate.isBlank() && !candidate.startsWith("data:")) {
                    return candidate;
                }
            }
        }

        if (document != null) {
            Elements images = document.select("article img, main img, .post img, .noticia img");
            for (Element img : images) {
                String candidate = resolveImageSource(img);
                if (candidate != null && !candidate.isBlank() && !candidate.startsWith("data:") && !candidate.endsWith(".svg")) {
                    return candidate;
                }
            }
        }

        return imageFallBackUrl();
    }

    private String resolveImageSource(Element img) {
        if (img == null) return null;

        for (String attr : List.of("src", "data-src", "data-lazy-src", "data-original")) {
            if (img.hasAttr(attr)) {
                String val = img.attr(attr).trim();
                if (!val.isBlank() && !val.startsWith("data:")) {
                    String abs = img.absUrl(attr);
                    return abs.isBlank() ? val : abs;
                }
            }
        }

        if (img.hasAttr("srcset")) {
            String srcset = img.attr("srcset").trim();
            if (!srcset.isBlank()) {
                String firstCandidate = srcset.split(",")[0].trim().split(" ")[0].trim();
                if (!firstCandidate.isBlank() && !firstCandidate.startsWith("data:")) {
                    return firstCandidate;
                }
            }
        }

        return null;
    }

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
            log.error("Failed to initialize resilient SSL socket factory", e);
            return (SSLSocketFactory) SSLSocketFactory.getDefault();
        }
    }
}