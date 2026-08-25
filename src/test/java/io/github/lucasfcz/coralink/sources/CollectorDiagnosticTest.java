package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.NewsSummary;
import org.junit.jupiter.api.Test;

import java.util.List;

public class CollectorDiagnosticTest {

    @Test
    void diagnoseAllFourCollectors() {
        testCollector("UFPE", new UfpeCollector());
        testCollector("UPE", new UpeCollector());
        testCollector("UNIFAFIRE", new UnifafireCollector());
        testCollector("PORTO_DIGITAL", new PortoDigitalCollector());
    }

    private void testCollector(String name, io.github.lucasfcz.coralink.sources.collector.Collector collector) {
        System.out.println("==================================================");
        System.out.println("TESTING COLLECTOR: " + name);
        System.out.println("==================================================");
        try {
            List<NewsSummary> summaries = collector.collect();
            System.out.println("Summaries collected count: " + summaries.size());
            for (int i = 0; i < Math.min(3, summaries.size()); i++) {
                NewsSummary s = summaries.get(i);
                System.out.printf("  [%d] Title: %s%n      URL: %s%n      Summary: %s%n",
                        i + 1, s.title(), s.url(), s.shortSummary());
                DetailedContent detailed = collector.detailedCollect(s.url());
                System.out.printf("      Detailed Image: %s%n      Detailed Content Length: %d%n",
                        detailed != null ? detailed.imageUrl() : "NULL",
                        (detailed != null && detailed.fullContent() != null) ? detailed.fullContent().length() : 0);
            }
        } catch (Exception e) {
            System.out.println("ERROR collecting for " + name + ": " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }
}
