package io.github.lucasfcz.coralink.modules.sources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DoityCollectorTest extends BaseCollectorLiveTest {

    @Test
    @DisplayName("Teste de integração ao vivo: DOITY")
    void testLiveCollection() {
        DoityCollector collector = new DoityCollector();
        assertCollectorLive(collector, "DOITY");
    }
}
