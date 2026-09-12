package io.github.lucasfcz.coralink.modules.sources;

import io.github.lucasfcz.coralink.modules.sources.dto.NewsSummary;
import io.github.lucasfcz.coralink.modules.sources.collector.HtmlCollector;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Coletor oficial para notícias da Assessoria de Comunicação (Ascom) da UFPE.
 * O portal da UFPE utiliza o CMS Liferay com estrutura de itens div.list-full-content__item.
 */
@Component
public class UfpeCollector extends HtmlCollector {


    private static final String BASE_URL = "https://www.ufpe.br";
    private static final String NEWS_URL = BASE_URL + "/ascom/noticias";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final List<String> INSTITUTIONAL_NOISE_TERMS = List.of(
            "nota de pesar",
            "falecimento",
            "luto oficial",
            "reconduzido",
            "reconduzida",
            "completa um ano",
            "comemora 15 anos",
            "comemora 10 anos",
            "comemora 20 anos",
            "comemora 50 anos",
            "eleição para",
            "posse da",
            "posse do"
    );

    @Override
    protected String baseUrl() {
        return BASE_URL;
    }

    @Override
    protected String imageFallBackUrl() {
        return "https://www.ufpe.br/ufpe-theme/images/custom/logo-ufpe.png";
    }

    @Override
    protected String pageUrl() {
        return NEWS_URL;
    }

    @Override
    protected List<Element> articles(Document document) {
        if (document == null) {
            return List.of();
        }
        return document.select("div.list-full-content__item");
    }

    @Override
    protected NewsSummary mapArticle(Element article) {
        Element titleLink = article.selectFirst("h3.list-full-content__title a");
        if (titleLink == null) {
            return null;
        }

        String title = titleLink.text().trim();
        String url = titleLink.absUrl("href");

        if (title.isBlank() || url.isBlank()) {
            return null;
        }

        Element summaryEl = article.selectFirst("div.list-full-content__sumary");
        String summary = summaryEl != null ? summaryEl.text().trim() : "";
        if (summary.isBlank()) {
            summary = title;
        }

        if (isInstitutionalNoise(title, summary)) {
            return null;
        }

        if (url.startsWith("http://")) {
            url = "https://" + url.substring(7);
        }

        LocalDateTime publishedDate = LocalDateTime.now();
        Element dateEl = article.selectFirst("span.list-full-content__date");
        if (dateEl != null) {
            try {
                String dateText = dateEl.text().trim();
                if (!dateText.isBlank()) {
                    publishedDate = LocalDate.parse(dateText, DATE_FORMATTER).atStartOfDay();
                }
            } catch (Exception ignored) {
            }
        }

        return new NewsSummary(
                title,
                summary,
                url,
                sourceName(),
                publishedDate
        );
    }

    private boolean isInstitutionalNoise(String title, String summary) {
        String lowerTitle = title.toLowerCase();
        String lowerSummary = summary.toLowerCase();
        for (String term : INSTITUTIONAL_NOISE_TERMS) {
            if (lowerTitle.contains(term) || lowerSummary.contains(term)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String sourceName() {
        return "UFPE";
    }
}