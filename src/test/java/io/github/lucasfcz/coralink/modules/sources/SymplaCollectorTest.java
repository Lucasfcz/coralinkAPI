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
}
