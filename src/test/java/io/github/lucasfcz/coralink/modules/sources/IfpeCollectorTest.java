package io.github.lucasfcz.coralink.modules.sources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IfpeCollectorTest extends BaseCollectorLiveTest {

    @Test
    @DisplayName("Teste de integração ao vivo: IFPE")
    void testLiveCollection() {
        IfpeCollector collector = new IfpeCollector();
        assertCollectorLive(collector, "IFPE");
    }

    @Test
    @DisplayName("Teste de modo dev: IFPE deve retornar no maximo 1 item")
    void testDevModeLimit() {
        IfpeCollector collector = new IfpeCollector();
        collector.setDevMode(true);
        var result = collector.collect();
        org.junit.jupiter.api.Assertions.assertTrue(result.size() <= 1);
    }
}
