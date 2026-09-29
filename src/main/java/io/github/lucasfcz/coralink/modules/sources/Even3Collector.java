package io.github.lucasfcz.coralink.modules.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lucasfcz.coralink.modules.sources.collector.AbstractCollector;
import io.github.lucasfcz.coralink.modules.sources.dto.DetailedContent;
import io.github.lucasfcz.coralink.modules.sources.dto.NewsSummary;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Coletor oficial para congressos, simpósios científicos, jornadas acadêmicas e chamadas de trabalhos
 * da Even3 (plataforma líder nacional em eventos científicos nascida no ecossistema do Recife).
 * Monitora eventos abertos para submissão científica e congressos acadêmicos em Pernambuco e online.
 */
@Slf4j
@Component
public class Even3Collector extends AbstractCollector {

    private static final String BASE_URL = "https://www.even3.com.br";
    private static final String FALLBACK_IMAGE_URL = "https://static.even3.com/assets/favicons/apple-icon-180x180.png";

    private static final String SUBMISSIONS_API_URL =
            "https://even3v2.blob.core.windows.net/json/lista-submissoes-abertas.json";
    private static final String HIGHLIGHTS_API_URL =
            "https://even3v2.blob.core.windows.net/json/home-destaque.json";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    protected String baseUrl() {
        return BASE_URL;
    }

    @Override
    protected String imageFallBackUrl() {
        return FALLBACK_IMAGE_URL;
    }

    @Override
    public String sourceName() {
        return "EVEN3";
    }

    @Override
    public List<NewsSummary> collect() {
        Map<String, NewsSummary> collected = new LinkedHashMap<>();

        collectHighlights(collected);
        collectSubmissions(collected);

        return new ArrayList<>(collected.values());
    }

    private void collectHighlights(Map<String, NewsSummary> collected) {
        try {
            String json = Jsoup.connect(HIGHLIGHTS_API_URL)
                    .sslSocketFactory(RESILIENT_SSL_SOCKET_FACTORY)
                    .ignoreContentType(true)
                    .timeout(TIMEOUT_MILLIS)
                    .userAgent(USER_AGENT)
                    .execute()
                    .body();

            JsonNode root = OBJECT_MAPPER.readTree(json);
            JsonNode items = root.path("listaEventosDestaque");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    NewsSummary summary = mapHighlightItem(item);
                    if (summary != null && !collected.containsKey(summary.url())) {
                        collected.put(summary.url(), summary);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Falha ao coletar eventos em destaque da Even3: {}", e.getMessage());
        }
    }

    private void collectSubmissions(Map<String, NewsSummary> collected) {
        try {
            String json = Jsoup.connect(SUBMISSIONS_API_URL)
                    .sslSocketFactory(RESILIENT_SSL_SOCKET_FACTORY)
                    .ignoreContentType(true)
                    .timeout(TIMEOUT_MILLIS)
                    .userAgent(USER_AGENT)
                    .execute()
                    .body();

            JsonNode root = OBJECT_MAPPER.readTree(json);
            JsonNode events = root.path("eventos");
            if (events.isArray()) {
                for (JsonNode event : events) {
                    NewsSummary summary = mapSubmissionItem(event);
                    if (summary != null && !collected.containsKey(summary.url())) {
                        collected.put(summary.url(), summary);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Falha ao coletar submissões abertas da Even3: {}", e.getMessage());
        }
    }

    public NewsSummary mapHighlightItem(JsonNode item) {
        String title = item.path("titulo").asText("").trim();
        String url = item.path("url").asText("").trim();
        if (title.isBlank() || url.isBlank()) {
            return null;
        }

        String sigla = item.path("siglaEstado").asText("").trim();
        String estado = item.path("estado").asText("").trim().toLowerCase();
        String cidade = item.path("cidade").asText("").trim().toLowerCase();
        int formato = item.path("idFormatoEvento").asInt(0);

        boolean isPernambuco = sigla.equalsIgnoreCase("PE") ||
                estado.contains("pernambuco") ||
                cidade.contains("recife") ||
                cidade.contains("olinda") ||
                cidade.contains("caruaru") ||
                cidade.contains("petrolina") ||
                cidade.contains("paulista");
        boolean isOnline = (formato == 1);

        if (!isPernambuco && !isOnline) {
            return null;
        }

        String rawSummary = item.path("resumoEvento").asText("").trim();
        String cleanSummary = rawSummary.isBlank() ? title : rawSummary;
        if (cleanSummary.length() > 300) {
            cleanSummary = cleanSummary.substring(0, 297) + "...";
        }

        LocalDateTime published = parseDateTime(item.path("data").asText());

        return new NewsSummary(
                title,
                cleanSummary,
                normalizeUrl(url),
                sourceName(),
                published
        );
    }

    public NewsSummary mapSubmissionItem(JsonNode event) {
        String title = event.path("titulo").asText("").trim();
        String url = event.path("url").asText("").trim();
        if (title.isBlank() || url.isBlank()) {
            return null;
        }

        String local = event.path("localRealizacao").asText("").trim();
        boolean isOnline = event.path("eventoOnline").asBoolean(false);
        String localLower = local.toLowerCase();

        boolean isPe = localLower.contains("pernambuco") ||
                localLower.contains("recife") ||
                localLower.contains("olinda") ||
                localLower.contains("caruaru") ||
                localLower.contains("petrolina") ||
                localLower.contains(", pe");

        if (!isPe && !isOnline) {
            return null;
        }

        String categoria = event.path("categoria").asText("Acadêmico").trim();
        String deadline = event.path("dataLimiteSubmissao").asText("").trim();
        String formattedDeadline = formatDeadline(deadline);

        StringBuilder sb = new StringBuilder();
        sb.append("[").append(categoria).append("] ");
        if (!formattedDeadline.isBlank()) {
            sb.append("Submissões abertas até ").append(formattedDeadline).append(". ");
        }
        sb.append("Formato: ").append(isOnline ? "Online" : local);

        LocalDateTime published = parseDateTime(event.path("dataInicioSubmissao").asText());

        return new NewsSummary(
                title,
                sb.toString(),
                normalizeUrl(url),
                sourceName(),
                published
        );
    }

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            try {
                return OffsetDateTime.parse(dateStr).toLocalDateTime();
            } catch (Exception ignored) {
                return LocalDateTime.now();
            }
        }
    }

    private String formatDeadline(String deadline) {
        if (deadline == null || deadline.isBlank()) {
            return "";
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(deadline, DateTimeFormatter.ISO_DATE_TIME);
            return ldt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return deadline.split("T")[0];
        }
    }

    private String normalizeUrl(String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        return BASE_URL + (url.startsWith("/") ? url : "/" + url);
    }

    @Override
    public DetailedContent detailedCollect(String url) {
        try {
            Document doc = requestDocument(url);
            if (doc == null) {
                return null;
            }

            // 1. Tentar og:description
            Element ogDesc = doc.selectFirst("meta[property=og:description]");
            if (ogDesc != null && ogDesc.hasAttr("content") && ogDesc.attr("content").trim().length() > 50) {
                return new DetailedContent(ogDesc.attr("content").trim());
            }

            // 2. Tentar meta description
            Element metaDesc = doc.selectFirst("meta[name=description]");
            if (metaDesc != null && metaDesc.hasAttr("content") && metaDesc.attr("content").trim().length() > 50) {
                return new DetailedContent(metaDesc.attr("content").trim());
            }

            // 3. Tentar seções editoriais da página
            Element content = doc.selectFirst(".event-description, #sobre, .sobre-evento, .descricao-evento, main");
            if (content != null) {
                String text = extractSummary(content);
                if (text.length() > 50) {
                    return new DetailedContent(text);
                }
            }

            if (doc.body() != null) {
                String bodyText = extractSummary(doc.body());
                if (!bodyText.isBlank()) {
                    return new DetailedContent(bodyText);
                }
            }
        } catch (Exception e) {
            log.error("Falha ao coletar detalhamento da Even3 para URL {}: {}", url, e.getMessage());
        }
        return null;
    }
}
