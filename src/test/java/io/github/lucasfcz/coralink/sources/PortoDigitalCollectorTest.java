package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.enums.SourceName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PortoDigitalCollectorTest {

    private PortoDigitalCollector collector;

    @BeforeEach
    void setUp() {
        collector = new PortoDigitalCollector();
    }

    @Test
    void testCollectFetchesLiveStories() {
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
            assertEquals(SourceName.PORTO_DIGITAL, summary.sourceName());
            assertNotNull(summary.foundAt());
        }
    }

    @Test
    void testDetailedCollectStoryblokEndpoint() {
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
