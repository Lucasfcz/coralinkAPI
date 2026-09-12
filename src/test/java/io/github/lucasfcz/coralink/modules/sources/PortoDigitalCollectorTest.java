package io.github.lucasfcz.coralink.modules.sources;

import io.github.lucasfcz.coralink.modules.sources.dto.DetailedContent;
import io.github.lucasfcz.coralink.modules.sources.dto.NewsSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PortoDigitalCollectorTest {

    private PortoDigitalCollector collector;

    @BeforeEach
    void setUp() {
        String token = System.getenv("PORTO_DIGITAL_STORYBLOK_TOKEN");
        if (token == null || token.isBlank()) {
            token = System.getProperty("PORTO_DIGITAL_STORYBLOK_TOKEN");
        }
        collector = new PortoDigitalCollector(token);
    }

    @Test
    void testCollectFetchesLiveStories() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                System.getenv("PORTO_DIGITAL_STORYBLOK_TOKEN") != null || System.getProperty("PORTO_DIGITAL_STORYBLOK_TOKEN") != null,
                "PORTO_DIGITAL_STORYBLOK_TOKEN não configurado no ambiente; pulando teste de integração ao vivo."
        );
        List<NewsSummary> summaries = collector.collect();
        assertNotNull(summaries);
        assertFalse(summaries.isEmpty());

        for (NewsSummary summary : summaries) {
            assertNotNull(summary.title());
            assertFalse(summary.title().isBlank());
            assertNotNull(summary.url());
            assertTrue(summary.url().startsWith("https://www.portodigital.org/noticias/"));
            assertNotNull(summary.shortSummary());
            assertFalse(summary.shortSummary().isBlank());
            assertEquals("PORTO_DIGITAL", summary.sourceName());
            assertNotNull(summary.foundAt());
        }
    }

    @Test
    void testDetailedCollectStoryblokEndpoint() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                System.getenv("PORTO_DIGITAL_STORYBLOK_TOKEN") != null || System.getProperty("PORTO_DIGITAL_STORYBLOK_TOKEN") != null,
                "PORTO_DIGITAL_STORYBLOK_TOKEN não configurado no ambiente; pulando teste de integração ao vivo."
        );
        List<NewsSummary> summaries = collector.collect();
        assertFalse(summaries.isEmpty());

        NewsSummary first = summaries.get(0);
        DetailedContent detailed = collector.detailedCollect(first.url());
        assertNotNull(detailed);
        assertNotNull(detailed.fullContent());
        assertFalse(detailed.fullContent().isBlank());
    }

    @Test
    void testDetailedCollectInvalidUrlReturnsNull() {
        DetailedContent detailed = collector.detailedCollect("https://invalid-non-existent-domain-12345.org/noticias/non-existent-slug");
        assertNull(detailed);
    }
}
