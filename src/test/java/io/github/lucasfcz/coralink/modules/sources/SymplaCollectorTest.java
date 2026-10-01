package io.github.lucasfcz.coralink.modules.sources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SymplaCollectorTest extends BaseCollectorLiveTest {

    @Test
    @DisplayName("Teste de integração ao vivo: SYMPLA")
    void testLiveCollection() {
        SymplaCollector collector = new SymplaCollector();
        assertCollectorLive(collector, "SYMPLA");
    }

    @Test
    @DisplayName("Teste de modo dev: SYMPLA deve retornar no maximo 1 item")
    void testDevModeLimit() {
        SymplaCollector collector = new SymplaCollector();
        collector.setDevMode(true);
        var result = collector.collect();
        org.junit.jupiter.api.Assertions.assertTrue(result.size() <= 1);
    }
}
