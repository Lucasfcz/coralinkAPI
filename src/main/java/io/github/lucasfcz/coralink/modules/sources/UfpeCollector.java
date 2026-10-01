package io.github.lucasfcz.coralink.modules.sources;

import io.github.lucasfcz.coralink.modules.sources.collector.HtmlCollector;
import io.github.lucasfcz.coralink.modules.sources.dto.DetailedContent;
import io.github.lucasfcz.coralink.modules.sources.dto.NewsSummary;
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
 * Coletor oficial para oportunidades e notícias dos Centros Acadêmicos da Universidade Federal de Pernambuco (UFPE).
 * Coleta diretamente dos 10 Centros Acadêmicos da UFPE:
 * - CB: Centro de Biociências
 * - CAC: Centro de Artes e Comunicação
 * - CCEN: Centro de Ciências Exatas e da Natureza
 * - CCJ (FDR): Centro de Ciências Jurídicas / Faculdade de Direito do Recife
 * - CCS: Centro de Ciências da Saúde
 * - CCM: Centro de Ciências Médicas
 * - CCSA: Centro de Ciências Sociais Aplicadas
 * - CE: Centro de Educação
 * - CFCH: Centro de Filosofia e Ciências Humanas
 * - CTG: Centro de Tecnologia e Geociências
 */
@Slf4j
@Component
public class UfpeCollector extends HtmlCollector {

    public record AcademicCenter(String acronym, String path) {}

    private static final String BASE_URL = "https://www.ufpe.br";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final List<AcademicCenter> ACADEMIC_CENTERS = List.of(
            new AcademicCenter("CB", "/cb"),
            new AcademicCenter("CAC", "/cac"),
            new AcademicCenter("CCEN", "/ccen"),
            new AcademicCenter("CCJ", "/ccj"),
            new AcademicCenter("CCS", "/ccs"),
            new AcademicCenter("CCM", "/ccm"),
            new AcademicCenter("CCSA", "/ccsa"),
            new AcademicCenter("CE", "/ce"),
            new AcademicCenter("CFCH", "/cfch"),
            new AcademicCenter("CTG", "/ctg")
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
        return BASE_URL + "/cb";
    }

    protected List<AcademicCenter> academicCenters() {
        return ACADEMIC_CENTERS;
    }

    @Override
    public List<NewsSummary> collect() {
        List<NewsSummary> result = new ArrayList<>();

        for (AcademicCenter center : academicCenters()) {
            String targetUrl = baseUrl() + center.path();
            try {
                Document document = requestDocument(targetUrl);
                if (document != null) {
                    List<NewsSummary> centerNews = articles(document)
                            .stream()
                            .map(article -> mapArticleWithCenter(article, center))
                            .filter(Objects::nonNull)
                            .toList();
                    result.addAll(centerNews);
                    log.info("Coletadas {} oportunidades do centro UFPE '{}' ({})", centerNews.size(), center.acronym(), center.path());
                }
            } catch (Exception exception) {
                log.warn("Falha ao coletar oportunidades do centro UFPE '{}' ({}); ignorando", center.acronym(), center.path(), exception);
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
        Elements items = document.select("div.list-news__item");
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
        return mapArticleWithCenter(article, null);
    }

    protected NewsSummary mapArticleWithCenter(Element article, AcademicCenter center) {
        if (article == null) {
            return null;
        }

        Element titleLink = findTitleLink(article);
        if (titleLink == null) {
            return null;
        }

        String title = extractTitle(titleLink);
        String url = extractUrl(titleLink);

        if (title.isBlank() || url.isBlank() || title.length() < 4) {
            return null;
        }

        Element summaryEl = article.selectFirst("div.list-news__summary, .list-news__summary, p");
        String rawSummary = summaryEl != null ? summaryEl.text().trim() : "";
        if (rawSummary.isBlank()) {
            rawSummary = title;
        }

        if (isInstitutionalNoise(title, rawSummary)) {
            return null;
        }

        String shortSummary = rawSummary;
        if (center != null && !center.acronym().isBlank()) {
            String tag = "[" + center.acronym() + "]";
            if (!shortSummary.startsWith(tag)) {
                shortSummary = tag + " " + shortSummary;
            }
        }

        LocalDateTime publishedDate = LocalDateTime.now();
        Element dateEl = article.selectFirst("span.list-news__date, .list-news__date, span.list-full-content__date, span.asset-date");
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
                shortSummary,
                url,
                sourceName(),
                publishedDate
        );
    }

    private Element findTitleLink(Element article) {
        Element headingPublisherLink = article.selectFirst(
                ".list-news__title a[href*='asset_publisher'], h4 a[href*='asset_publisher'], h3 a[href*='asset_publisher'], h2 a[href*='asset_publisher']"
        );
        if (headingPublisherLink != null && hasValidTitle(headingPublisherLink)) {
            return headingPublisherLink;
        }

        Elements publisherLinks = article.select("a[href*='asset_publisher']");
        for (Element link : publisherLinks) {
            if (hasValidTitle(link)) {
                return link;
            }
        }

        Element headingLink = article.selectFirst(".list-news__title a[href], h4 a[href], h3 a[href], h2 a[href]");
        if (headingLink != null && hasValidTitle(headingLink)) {
            return headingLink;
        }

        Elements allLinks = article.select("a[href]");
        for (Element link : allLinks) {
            if (hasValidTitle(link)) {
                return link;
            }
        }

        return !publisherLinks.isEmpty() ? publisherLinks.first() : article.selectFirst("a[href]");
    }

    private boolean hasValidTitle(Element link) {
        if (link == null) {
            return false;
        }
        String text = extractTitle(link);
        return !text.isBlank() && text.length() >= 4;
    }

    private String extractTitle(Element link) {
        if (link == null) {
            return "";
        }
        String title = link.attr("title").trim();
        if (title.isBlank()) {
            title = link.attr("aria-label").trim();
        }
        if (title.isBlank()) {
            title = link.text().trim();
        }
        if (title.isBlank()) {
            Element img = link.selectFirst("img[alt]");
            if (img != null) {
                title = img.attr("alt").trim();
            }
        }
        return title;
    }

    private String extractUrl(Element link) {
        if (link == null) {
            return "";
        }
        String url = link.absUrl("href");
        if (url.isBlank()) {
            url = link.attr("href").trim();
            if (url.startsWith("/")) {
                url = baseUrl() + url;
            }
        }
        if (url.startsWith("http://")) {
            url = "https://" + url.substring(7);
        }
        return url;
    }

    @Override
    public DetailedContent detailedCollect(String url) {
        if (url == null || url.isBlank()) {
            return new DetailedContent("Conteúdo detalhado indisponível para esta oportunidade da UFPE.");
        }

        try {
            Document document = requestDocument(url);
            if (document == null) {
                return new DetailedContent("Conteúdo detalhado não pôde ser carregado no momento a partir da UFPE.");
            }

            // Primary selectors
            Elements candidates = document.select("div.full-content__full-content, div.full-content, .asset-full-content");
            for (Element candidate : candidates) {
                String extracted = extractSummary(candidate);
                if (extracted.length() >= 50) {
                    return new DetailedContent(extracted);
                }
            }

            // Fallback for portlet list pages where the specific target article is listed
            String contentSlug = extractContentSlug(url);
            if (!contentSlug.isBlank()) {
                Element matchingLink = document.selectFirst(
                        ".list-full-content__item a[href*='" + contentSlug + "'], a[href*='" + contentSlug + "'][href*='/content/']"
                );
                if (matchingLink != null) {
                    String subUrl = matchingLink.absUrl("href");
                    if (subUrl.isBlank()) {
                        subUrl = matchingLink.attr("href");
                    }
                    if (subUrl.startsWith("/")) {
                        subUrl = baseUrl() + subUrl;
                    }
                    if (!subUrl.isBlank() && !subUrl.equalsIgnoreCase(url)) {
                        Document subDoc = requestDocument(subUrl);
                        if (subDoc != null) {
                            Elements subCandidates = subDoc.select("div.full-content__full-content, div.full-content, .asset-full-content");
                            for (Element subCandidate : subCandidates) {
                                String extracted = extractSummary(subCandidate);
                                if (extracted.length() >= 50) {
                                    return new DetailedContent(extracted);
                                }
                            }
                        }
                    }
                }
            }

            // Secondary content selectors
            Elements fallbackContainers = document.select(
                    ".journal-content-article, .list-full-content__content, .list-full-content__item, article, main, .entry-content, .noticia-corpo"
            );
            for (Element container : fallbackContainers) {
                String extracted = extractSummary(container);
                if (extracted.length() >= 50) {
                    return new DetailedContent(extracted);
                }
            }

            // Body content fallback
            if (document.body() != null) {
                String bodyText = extractSummary(document.body());
                if (bodyText.length() >= 50) {
                    return new DetailedContent(bodyText);
                }
            }

            // Meta description fallback
            Element metaDesc = document.selectFirst("meta[name='description'], meta[property='og:description']");
            if (metaDesc != null) {
                String desc = metaDesc.attr("content").trim();
                if (desc.length() >= 50) {
                    return new DetailedContent(desc);
                }
            }

            // Guarantee non-null and non-empty content with at least 50 characters
            String pageTitle = document.title();
            String defaultText = !pageTitle.isBlank()
                    ? "Universidade Federal de Pernambuco (UFPE) - " + pageTitle + ". Consulte o portal oficial da UFPE para mais detalhes desta publicação."
                    : "Informações completas sobre esta oportunidade estão disponíveis no portal oficial da Universidade Federal de Pernambuco (UFPE).";
            return new DetailedContent(defaultText);

        } catch (Exception e) {
            log.error("Falha ao coletar conteúdo detalhado para URL: {}", url, e);
            return new DetailedContent("Falha na extração de conteúdo detalhado da UFPE. Acesse a oportunidade diretamente pela URL oficial informada.");
        }
    }

    private String extractContentSlug(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        int contentIdx = url.indexOf("/content/");
        if (contentIdx != -1) {
            String afterContent = url.substring(contentIdx + "/content/".length());
            int slashIdx = afterContent.indexOf('/');
            if (slashIdx != -1) {
                return afterContent.substring(0, slashIdx);
            }
            return afterContent;
        }
        return extractSlug(url);
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