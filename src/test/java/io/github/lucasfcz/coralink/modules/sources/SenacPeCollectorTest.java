package io.github.lucasfcz.coralink.modules.sources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SenacPeCollectorTest extends BaseCollectorLiveTest {

    @Test
    @DisplayName("Teste de integração ao vivo: SENAC_PE")
    void testLiveCollection() {
        SenacPeCollector collector = new SenacPeCollector();
        assertCollectorLive(collector, "SENAC_PE");
    }
}
