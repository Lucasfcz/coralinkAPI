package io.github.lucasfcz.coralink.modules.sources;

import io.github.lucasfcz.coralink.modules.sources.collector.HtmlCollector;
import io.github.lucasfcz.coralink.modules.sources.dto.NewsSummary;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Coletor de congressos, simpósios, conferências e eventos científicos e acadêmicos na plataforma Doity,
 * filtrados especificamente para a região de Recife e Pernambuco.
 */
@Slf4j
@Component
public class DoityCollector extends HtmlCollector {

    private static final String BASE_URL = "https://doity.com.br";
    private static final String FALLBACK_IMAGE_URL = "https://doity.com.br/blog/app/uploads/2021/06/logo-doity.svg";

    private static final List<String> LISTING_URLS = List.of(
            "https://doity.com.br/eventos/recife-pe",
            "https://doity.com.br/eventos/congressos-seminarios/recife-pe",
            "https://doity.com.br/eventos/pernambuco"
    );

    private static final List<String> EXCLUDED_PATHS = List.of(
            "/blog",
            "/eventos/",
            "/login",
            "/cadastre-se",
            "/termos",
            "/politica-de-privacidade"
    );

    @Override
    protected String baseUrl() {
        return BASE_URL;
    }

    @Override
    protected String imageFallBackUrl() {
        return FALLBACK_IMAGE_URL;
    }

    @Override
    protected String pageUrl() {
        return "https://doity.com.br/eventos/recife-pe";
    }

    @Override
    public String sourceName() {
        return "DOITY";
    }

    @Override
    public List<NewsSummary> collect() {
        Map<String, NewsSummary> summariesByUrl = new LinkedHashMap<>();

        for (String listingUrl : LISTING_URLS) {
            try {
                Document document = requestDocument(listingUrl);
                if (document == null) {
                    continue;
                }

                for (Element item : articles(document)) {
                    NewsSummary summary = mapArticle(item);
                    if (summary != null) {
                        summariesByUrl.putIfAbsent(summary.url(), summary);
                    }
                }
            } catch (Exception e) {
                log.warn("Falha ao coletar eventos do Doity na URL {}: {}", listingUrl, e.getMessage());
            }

            pausePolitely();
        }

        return new ArrayList<>(summariesByUrl.values());
    }

    @Override
    protected List<Element> articles(Document document) {
        if (document == null) {
            return List.of();
        }
        return document.select("a[href*='doity.com.br/']");
    }

    @Override
    protected NewsSummary mapArticle(Element linkEl) {
        if (linkEl == null) {
            return null;
        }

        String rawUrl = linkEl.absUrl("href");
        if (rawUrl.isBlank()) {
            rawUrl = linkEl.attr("href");
        }

        if (rawUrl.isBlank()) {
            return null;
        }

        String cleanUrl = rawUrl.split("\\?")[0].split("#")[0];

        for (String excluded : EXCLUDED_PATHS) {
            if (cleanUrl.contains(excluded)) {
                return null;
            }
        }

        Element titleEl = linkEl.selectFirst("h2, h3, h1, [class*='title']");
        if (titleEl == null) {
            return null;
        }

        String title = titleEl.text().trim();
        if (title.isBlank()) {
            return null;
        }

        // Tenta capturar área temática/categoria (ex: Direito, Odontologia) e data/local
        String summary = title;
        Element categoryEl = linkEl.selectFirst("p.text-xs, [class*='category']");
        Element infoEl = linkEl.selectFirst("p.text-sm, [class*='date']");

        if (categoryEl != null && !categoryEl.text().isBlank()) {
            summary = "[" + categoryEl.text().trim() + "] " + title;
        }
        if (infoEl != null && !infoEl.text().isBlank()) {
            summary = summary + " — " + infoEl.text().trim();
        }

        return new NewsSummary(
                title,
                summary,
                cleanUrl,
                sourceName(),
                LocalDateTime.now()
        );
    }

    private void pausePolitely() {
        try {
            Thread.sleep(250);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
