package io.github.lucasfcz.coralink.modules.sources;

import io.github.lucasfcz.coralink.modules.sources.dto.DetailedContent;
import io.github.lucasfcz.coralink.modules.sources.dto.NewsSummary;
import io.github.lucasfcz.coralink.modules.sources.collector.Collector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CollectorDiagnosticTest {

    private void assertAndDiagnoseCollector(String name, Collector collector, String expectedSource) {
        System.out.println("==================================================");
        System.out.println("TESTING COLLECTOR: " + name);
        System.out.println("==================================================");

        assertEquals(expectedSource, collector.sourceName());

        List<NewsSummary> summaries = collector.collect();
        assertNotNull(summaries, name + " returned null summaries list");
        if (summaries.isEmpty()) {
            System.err.printf("[DIAGNOSTIC WARNING] Coletor '%s' retornou lista vazia de oportunidades. Verifique a acessibilidade do host remoto ou instabilidades no portal.%n", name);
            return;
        }
        System.out.printf("[DIAGNOSTIC SUCCESS] Coletor '%s' coletou com sucesso %d oportunidades.%n", name, summaries.size());

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
        assertAndDiagnoseCollector("UFPE", new UfpeCollector(), "UFPE");
    }

    @Test
    @DisplayName("Diagnose PORTO_DIGITAL Collector")
    void testPortoDigitalCollector() {
        assertAndDiagnoseCollector("PORTO_DIGITAL", new PortoDigitalCollector(), "PORTO_DIGITAL");
    }

    @Test
    @DisplayName("Diagnose SYMPLA Collector")
    void testSymplaCollector() {
        assertAndDiagnoseCollector("SYMPLA", new SymplaCollector(), "SYMPLA");
    }

    @Test
    @DisplayName("Diagnose CESAR Collector")
    void testCesarCollector() {
        assertAndDiagnoseCollector("CESAR", new CesarCollector(), "CESAR");
    }

    @Test
    @DisplayName("Diagnose CESAR_SCHOOL Collector")
    void testCesarSchoolCollector() {
        assertAndDiagnoseCollector("CESAR_SCHOOL", new CesarSchoolCollector(), "CESAR_SCHOOL");
    }

    @Test
    @DisplayName("Diagnose CIN_UFPE Collector")
    void testCinUfpeCollector() {
        assertAndDiagnoseCollector("CIN_UFPE", new CinUfpeCollector(), "CIN_UFPE");
    }

    @Test
    @DisplayName("Diagnose IFPE Collector")
    void testIfpeCollector() {
        assertAndDiagnoseCollector("IFPE", new IfpeCollector(), "IFPE");
    }

    @Test
    @DisplayName("Diagnose UNIBRA Collector")
    void testUnibraCollector() {
        assertAndDiagnoseCollector("UNIBRA", new UnibraCollector(), "UNIBRA");
    }

    @Test
    @DisplayName("Diagnose UNIFAFIRE Collector")
    void testUnifafireCollector() {
        assertAndDiagnoseCollector("UNIFAFIRE", new UnifafireCollector(), "UNIFAFIRE");
    }

    @Test
    @DisplayName("Diagnose UPE Collector")
    void testUpeCollector() {
        assertAndDiagnoseCollector("UPE", new UpeCollector(), "UPE");
    }

    @Test
    @DisplayName("Diagnose SENAC_PE Collector")
    void testSenacPeCollector() {
        assertAndDiagnoseCollector("SENAC_PE", new SenacPeCollector(), "SENAC_PE");
    }

    @Test
    @DisplayName("Diagnose FACEPE Collector")
    void testFacepeCollector() {
        assertAndDiagnoseCollector("FACEPE", new FacepeCollector(), "FACEPE");
    }
}
