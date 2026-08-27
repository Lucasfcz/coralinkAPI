package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.sources.collector.Collector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CollectorDiagnosticTest {

    private void assertAndDiagnoseCollector(String name, Collector collector, SourceName expectedSource) {
        System.out.println("==================================================");
        System.out.println("TESTING COLLECTOR: " + name);
        System.out.println("==================================================");

        assertEquals(expectedSource, collector.sourceName());

        List<NewsSummary> summaries = collector.collect();
        assertNotNull(summaries, name + " returned null summaries list");
        if (summaries.isEmpty()) {
            System.out.println("WARNING: " + name + " returned empty summaries list (host may be down or unreachable)");
            return;
        }
        System.out.println("Summaries collected count: " + summaries.size());

        for (int i = 0; i < summaries.size(); i++) {
            NewsSummary s = summaries.get(i);
            assertNotNull(s.title(), name + " summary [" + i + "] has null title");
            assertFalse(s.title().isBlank(), name + " summary [" + i + "] has blank title");

            assertNotNull(s.url(), name + " summary [" + i + "] has null URL");
            assertFalse(s.url().isBlank(), name + " summary [" + i + "] has blank URL");
            assertTrue(s.url().startsWith("http://") || s.url().startsWith("https://"),
                    name + " summary [" + i + "] URL does not start with http/https: " + s.url());

            assertNotNull(s.shortSummary(), name + " summary [" + i + "] has null shortSummary");
            assertFalse(s.shortSummary().isBlank(), name + " summary [" + i + "] has blank shortSummary");

            assertEquals(expectedSource, s.sourceName());
            assertNotNull(s.foundAt(), name + " summary [" + i + "] has null foundAt");
        }

        // Test detailed collection on up to first 3 items
        int itemsToTest = Math.min(3, summaries.size());
        for (int i = 0; i < itemsToTest; i++) {
            NewsSummary s = summaries.get(i);
            System.out.printf("  [%d] Title: %s%n      URL: %s%n      Summary: %s%n",
                    i + 1, s.title(), s.url(), s.shortSummary());

            DetailedContent detailed = collector.detailedCollect(s.url());
            assertNotNull(detailed, name + " detailed content was null for URL: " + s.url());

            assertNotNull(detailed.fullContent(), name + " fullContent was null for URL: " + s.url());
            assertFalse(detailed.fullContent().isBlank(), name + " fullContent was blank for URL: " + s.url());
            assertTrue(detailed.fullContent().length() > 0, name + " fullContent length is 0 for URL: " + s.url());

            assertNotNull(collector.fallbackImageUrl(), name + " fallbackImageUrl was null");
            assertFalse(collector.fallbackImageUrl().isBlank(), name + " fallbackImageUrl was blank");
            assertTrue(collector.fallbackImageUrl().startsWith("http://") || collector.fallbackImageUrl().startsWith("https://"),
                    name + " fallbackImageUrl does not start with http/https: " + collector.fallbackImageUrl());

            System.out.printf("      Fallback Image: %s%n      Detailed Content Length: %d%n",
                    collector.fallbackImageUrl(), detailed.fullContent().length());
        }
        System.out.println();
    }

    @Test
    @DisplayName("Diagnose UFPE Collector")
    void testUfpeCollector() {
        assertAndDiagnoseCollector("UFPE", new UfpeCollector(), SourceName.UFPE);
    }

    @Test
    @DisplayName("Diagnose PORTO_DIGITAL Collector")
    void testPortoDigitalCollector() {
        assertAndDiagnoseCollector("PORTO_DIGITAL", new PortoDigitalCollector(), SourceName.PORTO_DIGITAL);
    }

    @Test
    @DisplayName("Diagnose SYMPLA Collector")
    void testSymplaCollector() {
        assertAndDiagnoseCollector("SYMPLA", new SymplaCollector(), SourceName.SYMPLA);
    }

    @Test
    @DisplayName("Diagnose CESAR Collector")
    void testCesarCollector() {
        assertAndDiagnoseCollector("CESAR", new CesarCollector(), SourceName.CESAR);
    }

    @Test
    @DisplayName("Diagnose CESAR_SCHOOL Collector")
    void testCesarSchoolCollector() {
        assertAndDiagnoseCollector("CESAR_SCHOOL", new CesarSchoolCollector(), SourceName.CESAR_SCHOOL);
    }

    @Test
    @DisplayName("Diagnose CIN_UFPE Collector")
    void testCinUfpeCollector() {
        assertAndDiagnoseCollector("CIN_UFPE", new CinUfpeCollector(), SourceName.CIN_UFPE);
    }

    @Test
    @DisplayName("Diagnose IFPE Collector")
    void testIfpeCollector() {
        assertAndDiagnoseCollector("IFPE", new IfpeCollector(), SourceName.IFPE);
    }

    @Test
    @DisplayName("Diagnose UNIBRA Collector")
    void testUnibraCollector() {
        assertAndDiagnoseCollector("UNIBRA", new UnibraCollector(), SourceName.UNIBRA);
    }

    @Test
    @DisplayName("Diagnose UNIFAFIRE Collector")
    void testUnifafireCollector() {
        assertAndDiagnoseCollector("UNIFAFIRE", new UnifafireCollector(), SourceName.UNIFAFIRE);
    }

    @Test
    @DisplayName("Diagnose UPE Collector")
    void testUpeCollector() {
        assertAndDiagnoseCollector("UPE", new UpeCollector(), SourceName.UPE);
    }
}
