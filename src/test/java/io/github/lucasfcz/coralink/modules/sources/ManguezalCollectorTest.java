package io.github.lucasfcz.coralink.modules.sources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.lucasfcz.coralink.modules.sources.dto.NewsSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ManguezalCollectorTest {

    private ManguezalCollector collector;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        collector = new ManguezalCollector();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testBasicConfig() {
        assertEquals("MANGUEZAL", collector.sourceName());
        assertTrue(collector.baseUrl().contains("manguezal.org"));
        assertTrue(collector.fallbackImageUrl().contains("manguezal"));
    }

    @Test
    void testMapSupabaseEventWithRegistrationUrl() {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("id", "3b59107c-093c-4e5e-a50b-642cbfe5af3d");
        event.put("title", "Meetup de IA & Startups");
        event.put("subtitle", "Encontros temáticos de inovação");
        event.put("description", "Painel prático com especialistas sobre novas tecnologias no Recife Antigo.");
        event.put("location", "Recife, PE");
        event.put("frequency", "Mensal");
        event.put("registration_url", "https://lu.ma/manguezal-ia-recife");
        event.put("updated_at", "2026-09-20T10:00:00+00:00");

        NewsSummary summary = collector.mapEvent(event);
        assertNotNull(summary);
        assertEquals("Manguez.Al: Meetup de IA & Startups", summary.title());
        assertEquals("https://lu.ma/manguezal-ia-recife", summary.url());
        assertEquals("MANGUEZAL", summary.sourceName());
        assertTrue(summary.shortSummary().contains("Encontros temáticos de inovação"));
        assertTrue(summary.shortSummary().contains("Recife Antigo"));
        assertTrue(summary.shortSummary().contains("Recife, PE"));
    }

    @Test
    void testMapSupabaseEventWithAnchorFallbackUrl() {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("id", "cafe-mangue-id");
        event.put("title", "Café do Mangue");
        event.put("subtitle", "Conexões matinais descontraídas");
        event.put("description", "Troca de experiências e networking.");
        event.put("location", "Recife, PE");
        event.put("frequency", "Quinzenal");
        event.put("registration_url", "");

        NewsSummary summary = collector.mapEvent(event);
        assertNotNull(summary);
        assertEquals("https://manguezal.org/#evento-cafe-mangue-id", summary.url());
    }

    @Test
    void testFallbackEvents() {
        List<NewsSummary> fallback = collector.fallbackEvents();
        assertFalse(fallback.isEmpty());
        assertTrue(fallback.stream().anyMatch(e -> e.title().contains("Café do Mangue")));
        assertTrue(fallback.stream().anyMatch(e -> e.title().contains("Meetup")));
        assertTrue(fallback.stream().anyMatch(e -> e.title().contains("Manguebeer")));
        assertTrue(fallback.stream().anyMatch(e -> e.title().contains("Manguetown")));
    }
}
