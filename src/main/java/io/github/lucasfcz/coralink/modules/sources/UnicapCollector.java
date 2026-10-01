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
 * Coletor oficial para notícias, eventos, seminários e editais da Universidade Católica de Pernambuco (UNICAP).
 * Coleta das seções centrais de eventos e publicações acadêmicas da UNICAP.
 */
@Slf4j
@Component
public class UnicapCollector extends HtmlCollector {

    private static final String BASE_URL = "https://portal.unicap.br";
    private static final String FALLBACK_IMAGE_URL = "https://images.seeklogo.com/logo-png/21/1/unicap-logo-png_seeklogo-218833.png";

    private static final List<String> SECTIONS = List.of(
            "/acontece-na-unicap",
            "/noticias"
    );

    private static final List<String> NOISE_TERMS = List.of(
            "nota de pesar",
            "falecimento",
            "luto oficial",
            "missa de sétimo dia",
            "recesso administrativo"
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
        return BASE_URL + "/acontece-na-unicap";
    }

    @Override
    public String sourceName() {
        return "UNICAP";
    }

    @Override
    public List<NewsSummary> collect() {
        Map<String, NewsSummary> summariesByUrl = new LinkedHashMap<>();

        for (String sectionPath : SECTIONS) {
            String targetUrl = baseUrl() + sectionPath;
            try {
                Document document = requestDocument(targetUrl);
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
                log.warn("Falha ao coletar seção '{}' da UNICAP: {}", sectionPath, e.getMessage());
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
        // Seleciona cards ou blocos que contenham o link canônico no padrão Liferay da Unicap (/-/)
        return document.select("a.stretched-link[href*='/-/'], a[href*='portal.unicap.br/-/']");
    }

    @Override
    protected NewsSummary mapArticle(Element linkEl) {
        if (linkEl == null) {
            return null;
        }

        String url = linkEl.absUrl("href");
        if (url.isBlank()) {
            url = linkEl.attr("href");
        }

        if (url.isBlank() || !url.contains("/-/")) {
            return null;
        }

        // Tenta capturar o título a partir do card pai
        String title = "";
        String summary = "";

        Element card = linkEl.closest(".card, .change-class, div:has(h3)");
        if (card != null) {
            Element titleEl = card.selectFirst("h3, h2, h4, .card-title");
            if (titleEl != null) {
                title = titleEl.text().trim();
            }

            Element summaryEl = card.selectFirst(".card-text, p");
            if (summaryEl != null) {
                summary = summaryEl.text().trim();
            }
        }

        if (title.isBlank()) {
            title = linkEl.text().trim();
        }

        // Se o link era apenas "Ler Mais", tenta deduzir do slug da URL
        if (title.isBlank() || title.equalsIgnoreCase("Ler Mais") || title.equalsIgnoreCase("Saiba Mais")) {
            String slug = url.substring(url.lastIndexOf("/-/") + 3).replace("-", " ");
            title = capitalizeWords(slug);
        }

        if (isInstitutionalNoise(title)) {
            return null;
        }

        if (summary.isBlank()) {
            summary = title;
        }

        return new NewsSummary(
                title,
                summary,
                url,
                sourceName(),
                LocalDateTime.now()
        );
    }

    private String capitalizeWords(String text) {
        if (text == null || text.isBlank()) return text;
        String[] words = text.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private boolean isInstitutionalNoise(String title) {
        String lower = title.toLowerCase();
        for (String term : NOISE_TERMS) {
            if (lower.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private void pausePolitely() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
