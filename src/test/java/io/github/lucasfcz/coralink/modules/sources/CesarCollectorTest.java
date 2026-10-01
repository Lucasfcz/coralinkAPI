package io.github.lucasfcz.coralink.modules.sources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CesarCollectorTest extends BaseCollectorLiveTest {

    @Test
    @DisplayName("Teste de integração ao vivo: CESAR")
    void testLiveCollection() {
        CesarCollector collector = new CesarCollector();
        assertCollectorLive(collector, "CESAR");
    }
}
