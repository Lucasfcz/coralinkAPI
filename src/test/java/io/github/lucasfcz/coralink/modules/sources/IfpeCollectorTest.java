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
}
