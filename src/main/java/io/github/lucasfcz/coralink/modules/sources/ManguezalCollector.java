package io.github.lucasfcz.coralink.modules.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lucasfcz.coralink.modules.sources.collector.AbstractCollector;
import io.github.lucasfcz.coralink.modules.sources.dto.DetailedContent;
import io.github.lucasfcz.coralink.modules.sources.dto.NewsSummary;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Coletor oficial para meetups, encontros de inovação, hackathons e eventos de networking
 * da Comunidade Manguezal (a maior comunidade de startups e ecossistema de tecnologia do Recife/PE).
 * Integra com o endpoint público do Supabase REST API mantido pela comunidade e possui fallback resiliente.
 */
@Slf4j
@Component
public class ManguezalCollector extends AbstractCollector {

    private static final String BASE_URL = "https://manguezal.org";
    private static final String FALLBACK_IMAGE_URL = "https://manguezal.org/favicon.svg";

    private static final String SUPABASE_EVENTS_URL =
            "https://fodnvhsgvmrbtyjkemum.supabase.co/rest/v1/events?is_active=eq.true";
    private static final String SUPABASE_ANON_KEY =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZvZG52aHNndm1yYnR5amtlbXVtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQ4NTU2NzYsImV4cCI6MjA4MDQzMTY3Nn0.A4BS2KrAFxkK1UdG_KtK0yiuN2AMBznlo1_NElgCeZg";

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
        return "MANGUEZAL";
    }

    @Override
    public List<NewsSummary> collect() {
        List<NewsSummary> summaries = new ArrayList<>();
        try {
            String json = Jsoup.connect(SUPABASE_EVENTS_URL)
                    .sslSocketFactory(RESILIENT_SSL_SOCKET_FACTORY)
                    .header("apikey", SUPABASE_ANON_KEY)
                    .header("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                    .ignoreContentType(true)
                    .timeout(TIMEOUT_MILLIS)
                    .userAgent(USER_AGENT)
                    .execute()
                    .body();

            JsonNode events = OBJECT_MAPPER.readTree(json);
            if (events.isArray() && !events.isEmpty()) {
                for (JsonNode event : events) {
                    NewsSummary summary = mapEvent(event);
                    if (summary != null) {
                        summaries.add(summary);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Falha ao consultar API Supabase da Comunidade Manguezal: {}. Ativando fallback estático.", e.getMessage());
        }

        if (summaries.isEmpty()) {
            summaries.addAll(fallbackEvents());
        }

        return summaries;
    }

    public NewsSummary mapEvent(JsonNode event) {
        String title = event.path("title").asText("").trim();
        if (title.isBlank()) {
            return null;
        }

        String subtitle = event.path("subtitle").asText("").trim();
        String description = event.path("description").asText("").trim();
        String location = event.path("location").asText("Recife, PE").trim();
        String frequency = event.path("frequency").asText("").trim();
        String registrationUrl = event.path("registration_url").asText("").trim();
        String id = event.path("id").asText("").trim();

        StringBuilder sb = new StringBuilder();
        if (!subtitle.isBlank()) {
            sb.append(subtitle).append(". ");
        }
        if (!description.isBlank()) {
            sb.append(description);
        }
        if (!frequency.isBlank()) {
            sb.append(" (Frequência: ").append(frequency).append(" | Local: ").append(location).append(")");
        }

        String summary = sb.toString().trim();
        if (summary.isBlank()) {
            summary = title + " - Comunidade Manguezal Recife";
        }
        if (summary.length() > 300) {
            summary = summary.substring(0, 297) + "...";
        }

        String url;
        if (!registrationUrl.isBlank() && registrationUrl.startsWith("http")) {
            url = registrationUrl;
        } else {
            url = BASE_URL + "/#evento-" + (id.isBlank() ? title.toLowerCase().replaceAll("[^a-z0-9]", "-") : id);
        }

        LocalDateTime published = LocalDateTime.now();
        String dateStr = event.path("updated_at").asText("");
        if (!dateStr.isBlank()) {
            try {
                published = OffsetDateTime.parse(dateStr).toLocalDateTime();
            } catch (Exception ignored) {}
        }

        return new NewsSummary(
                "Manguez.Al: " + title,
                summary,
                url,
                sourceName(),
                published
        );
    }

    public List<NewsSummary> fallbackEvents() {
        return List.of(
                new NewsSummary(
                        "Manguez.Al: Meetup do Ecossistema",
                        "Encontros temáticos de tecnologia, empreendedorismo e networking entre estudantes universitários, desenvolvedores e startups do Recife.",
                        BASE_URL + "/#meetup",
                        sourceName(),
                        LocalDateTime.now()
                ),
                new NewsSummary(
                        "Manguez.Al: Café do Mangue",
                        "Bate-papo matinal descontraído para gerar conexões, mentoria e troca de experiências entre profissionais e jovens inovadores de Pernambuco.",
                        BASE_URL + "/#cafe",
                        sourceName(),
                        LocalDateTime.now()
                ),
                new NewsSummary(
                        "Manguez.Al: Manguebeer",
                        "Happy hour e confraternização oficial da comunidade de inovação no Recife Antigo para fortalecer laços e parcerias.",
                        BASE_URL + "/#manguebeer",
                        sourceName(),
                        LocalDateTime.now()
                ),
                new NewsSummary(
                        "Manguez.Al: Manguetown",
                        "Encontros de prática de inglês focados em negócios, tecnologia e preparação de universitários para o mercado global.",
                        BASE_URL + "/#manguetown",
                        sourceName(),
                        LocalDateTime.now()
                )
        );
    }

    @Override
    public DetailedContent detailedCollect(String url) {
        try {
            // Se for URL da comunidade, podemos coletar a página principal ou seção
            var doc = requestDocument(url);
            if (doc != null && doc.body() != null) {
                String text = extractSummary(doc.body());
                if (!text.isBlank()) {
                    return new DetailedContent(text);
                }
            }
        } catch (Exception e) {
            log.error("Falha ao coletar detalhamento da Comunidade Manguezal para URL {}: {}", url, e.getMessage());
        }
        return null;
    }
}
