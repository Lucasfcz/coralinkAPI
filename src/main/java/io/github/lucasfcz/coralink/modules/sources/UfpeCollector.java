package io.github.lucasfcz.coralink.modules.sources;

import io.github.lucasfcz.coralink.modules.sources.dto.NewsSummary;
import io.github.lucasfcz.coralink.modules.sources.collector.HtmlCollector;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Coletor oficial para oportunidades e editais da Universidade Federal de Pernambuco (UFPE).
 * Coleta diretamente das Pró-Reitorias centrais onde editais estudantis são publicados:
 * - PROPESQI: Iniciação Científica (PIBIC/PIBITI), editais de pesquisa e fomento.
 * - PROPG: Editais de pós-graduação, mestrado, doutorado e especializações.
 * - PROEXC: Editais de extensão universitária, cultura e bolsas extensionistas.
 * - PROAES: Auxílios e assistência estudantil (alimentação, moradia, transporte).
 */
@Slf4j
@Component
public class UfpeCollector extends HtmlCollector {

    private static final String BASE_URL = "https://www.ufpe.br";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final List<String> PRO_REITORIAS = List.of(
            "/propesqi",
            "/propg",
            "/proexc",
            "/proaes"
    );

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
        return BASE_URL + "/propesqi";
    }

    protected List<String> proReitoriasPaths() {
        return PRO_REITORIAS;
    }

    @Override
    public List<NewsSummary> collect() {
        List<NewsSummary> result = new ArrayList<>();

        for (String path : proReitoriasPaths()) {
            String targetUrl = baseUrl() + path;
            try {
                Document document = requestDocument(targetUrl);
                if (document != null) {
                    List<NewsSummary> proReitoriaNews = articles(document)
                            .stream()
                            .map(this::mapArticle)
                            .filter(Objects::nonNull)
                            .toList();
                    result.addAll(proReitoriaNews);
                    log.info("Coletadas {} oportunidades da pró-reitoria UFPE '{}'", proReitoriaNews.size(), path);
                }
            } catch (Exception exception) {
                log.warn("Falha ao coletar oportunidades da pró-reitoria UFPE '{}'; ignorando", path, exception);
            }

            pausePolitely();
        }

        return result;
    }

    @Override
    protected List<Element> articles(Document document) {
        if (document == null) {
            return List.of();
        }
        Elements items = document.select("div.list-full-content__item");
        if (!items.isEmpty()) {
            return items;
        }

        items = document.select(".asset-abstract, .asset-entry, div.noticia, li.asset-item");
        if (!items.isEmpty()) {
            return items;
        }

        return List.of();
    }

    @Override
    protected NewsSummary mapArticle(Element article) {
        Element titleLink = article.selectFirst("h3.list-full-content__title a, h3 a, h2 a, h4 a, .asset-title a, a.asset-link");
        if (titleLink == null) {
            return null;
        }

        String title = titleLink.text().trim();
        String url = titleLink.absUrl("href");

        if (title.isBlank() || url.isBlank()) {
            return null;
        }

        Element summaryEl = article.selectFirst("div.list-full-content__sumary, .asset-summary, p");
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
        Element dateEl = article.selectFirst("span.list-full-content__date, span.asset-date, span.metadata-entry");
        if (dateEl != null) {
            try {
                String dateText = dateEl.text().replaceAll("[^0-9/]", "").trim();
                if (!dateText.isBlank() && dateText.length() >= 10) {
                    publishedDate = LocalDate.parse(dateText.substring(0, 10), DATE_FORMATTER).atStartOfDay();
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

    private void pausePolitely() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String sourceName() {
        return "UFPE";
    }
}