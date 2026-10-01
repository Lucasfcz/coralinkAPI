package io.github.lucasfcz.coralink.modules.sources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UfpeCollectorTest extends BaseCollectorLiveTest {

    @Test
    @DisplayName("Teste de integração ao vivo: UFPE")
    void testLiveCollection() {
        UfpeCollector collector = new UfpeCollector();
        assertCollectorLive(collector, "UFPE");
    }

    @Test
    @DisplayName("Teste de modo dev: UFPE deve retornar no maximo 1 item")
    void testDevModeLimit() {
        UfpeCollector collector = new UfpeCollector();
        collector.setDevMode(true);
        var result = collector.collect();
        org.junit.jupiter.api.Assertions.assertTrue(result.size() <= 1);
    }
}
