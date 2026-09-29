package io.github.lucasfcz.coralink.modules.sources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.lucasfcz.coralink.modules.sources.dto.NewsSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Even3CollectorTest {

    private Even3Collector collector;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        collector = new Even3Collector();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testBasicConfig() {
        assertEquals("EVEN3", collector.sourceName());
        assertTrue(collector.baseUrl().contains("even3.com.br"));
        assertTrue(collector.fallbackImageUrl().contains("even3.com"));
    }

    @Test
    void testPernambucoSubmissionMapping() {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("titulo", "I Congresso Pernambucano de Iniciação Científica");
        event.put("url", "https://even3.com.br/congresso-pe-2026/#submissoes");
        event.put("localRealizacao", "Recife, Pernambuco");
        event.put("categoria", "Multidisciplinar");
        event.put("eventoOnline", false);
        event.put("dataInicioSubmissao", "2026-09-01T00:00:00");
        event.put("dataLimiteSubmissao", "2026-11-20T00:00:00");

        NewsSummary summary = collector.mapSubmissionItem(event);
        assertNotNull(summary);
        assertEquals("I Congresso Pernambucano de Iniciação Científica", summary.title());
        assertEquals("https://even3.com.br/congresso-pe-2026/#submissoes", summary.url());
        assertEquals("EVEN3", summary.sourceName());
        assertTrue(summary.shortSummary().contains("[Multidisciplinar]"));
        assertTrue(summary.shortSummary().contains("20/11/2026"));
        assertTrue(summary.shortSummary().contains("Recife, Pernambuco"));
    }

    @Test
    void testOnlineSubmissionMapping() {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("titulo", "Simpósio Nacional de Inteligência Artificial");
        event.put("url", "https://even3.com.br/snia-2026/#submissoes");
        event.put("localRealizacao", ", ");
        event.put("categoria", "Tecnologia");
        event.put("eventoOnline", true);
        event.put("dataInicioSubmissao", "2026-09-10T00:00:00");
        event.put("dataLimiteSubmissao", "2026-10-30T00:00:00");

        NewsSummary summary = collector.mapSubmissionItem(event);
        assertNotNull(summary);
        assertEquals("Simpósio Nacional de Inteligência Artificial", summary.title());
        assertTrue(summary.shortSummary().contains("Formato: Online"));
    }

    @Test
    void testFilterOutUnrelatedStatesForSubmissions() {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("titulo", "Jornada Acadêmica do Sul");
        event.put("url", "https://even3.com.br/sul-2026/#submissoes");
        event.put("localRealizacao", "Porto Alegre, Rio Grande do Sul");
        event.put("categoria", "Educação");
        event.put("eventoOnline", false);

        NewsSummary summary = collector.mapSubmissionItem(event);
        assertNull(summary, "Eventos presenciais fora de PE devem ser ignorados");
    }

    @Test
    void testHighlightEventMapping() {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("titulo", "Artmed Experience Recife 2026");
        item.put("url", "https://www.even3.com.br/artmed-recife-2026");
        item.put("cidade", "Recife");
        item.put("estado", "Pernambuco");
        item.put("siglaEstado", "PE");
        item.put("resumoEvento", "Grande imersão com evidências científicas e prática acadêmica.");
        item.put("idFormatoEvento", 2);
        item.put("data", "2026-10-15T00:00:00");

        NewsSummary summary = collector.mapHighlightItem(item);
        assertNotNull(summary);
        assertEquals("Artmed Experience Recife 2026", summary.title());
        assertEquals("https://www.even3.com.br/artmed-recife-2026", summary.url());
        assertTrue(summary.shortSummary().contains("Grande imersão com evidências científicas"));
    }
}
