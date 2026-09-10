package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.sources.collector.HtmlCollector;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Coletor oficial de editais de fomento, bolsas e inovação da FACEPE
 * (Fundação de Amparo à Ciência e Tecnologia do Estado de Pernambuco).
 */
@Slf4j
@Component
public class FacepeCollector extends HtmlCollector {

    private static final String BASE_URL = "https://www.facepe.br";
    private static final String EDITAIS_URL = BASE_URL + "/editais/";
    private static final String FALLBACK_IMAGE = "https://www.facepe.br/wp-content/uploads/2020/09/cropped-logo-facepe.png";

    private final Map<String, String> detailedContentByUrl = new ConcurrentHashMap<>();

    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{1,2})\\s+de\\s+([a-zA-ZçÇ]+)\\s+de\\s+(\\d{4})");

    private static final Map<String, String> MONTHS = Map.ofEntries(
            Map.entry("janeiro", "01"),
            Map.entry("fevereiro", "02"),
            Map.entry("março", "03"),
            Map.entry("marco", "03"),
            Map.entry("abril", "04"),
            Map.entry("maio", "05"),
            Map.entry("junho", "06"),
            Map.entry("julho", "07"),
            Map.entry("agosto", "08"),
            Map.entry("setembro", "09"),
            Map.entry("outubro", "10"),
            Map.entry("novembro", "11"),
            Map.entry("dezembro", "12")
    );

    @Override
    protected String baseUrl() {
        return BASE_URL;
    }

    @Override
    protected String imageFallBackUrl() {
        return FALLBACK_IMAGE;
    }

    @Override
    protected String pageUrl() {
        return EDITAIS_URL;
    }

    @Override
    protected List<Element> articles(Document document) {
        if (document == null) {
            return List.of();
        }
        return document.select("div.edital-conteudo:not(.sub-arquivo-edital)");
    }

    @Override
    protected NewsSummary mapArticle(Element article) {
        Element linkEl = article.selectFirst("h5 a[href]");
        if (linkEl == null) {
            return null;
        }

        String fullTitle = article.select("h5 a").text().replaceAll("\\s+", " ").trim();
        String url = linkEl.absUrl("href");

        if (fullTitle.isBlank() || url.isBlank()) {
            return null;
        }

        String rawText = article.text().replaceAll("\\s+", " ").trim();
        String summary = "Edital aberto da FACEPE: " + fullTitle + ". " + rawText;

        LocalDateTime publishedDate = parsePublishDate(rawText);

        String detailed = """
                Edital FACEPE: %s
                Fonte Oficial: Fundação de Amparo à Ciência e Tecnologia de Pernambuco (FACEPE)
                Informações: %s
                Link Oficial para Download e Consulta: %s
                """.formatted(fullTitle, rawText, url);
        detailedContentByUrl.put(url, detailed);

        return new NewsSummary(
                fullTitle,
                summary,
                url,
                sourceName(),
                publishedDate
        );
    }

    @Override
    public DetailedContent detailedCollect(String url) {
        if (url != null && detailedContentByUrl.containsKey(url)) {
            return new DetailedContent(detailedContentByUrl.get(url));
        }
        if (url != null && !url.toLowerCase().endsWith(".pdf")) {
            return super.detailedCollect(url);
        }
        return new DetailedContent("Edital oficial da FACEPE disponível para download e consulta no link: " + url);
    }

    private LocalDateTime parsePublishDate(String text) {
        Matcher matcher = DATE_PATTERN.matcher(text.toLowerCase());
        if (matcher.find()) {
            try {
                String day = String.format("%02d", Integer.parseInt(matcher.group(1)));
                String monthName = matcher.group(2);
                String month = MONTHS.getOrDefault(monthName, "01");
                String year = matcher.group(3);
                return LocalDate.parse(year + "-" + month + "-" + day).atStartOfDay();
            } catch (Exception ignored) {
            }
        }
        return LocalDateTime.now();
    }

    @Override
    public SourceName sourceName() {
        return SourceName.FACEPE;
    }
}
