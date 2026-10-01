package io.github.lucasfcz.coralink.modules.sources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UpeCollectorTest extends BaseCollectorLiveTest {

    @Test
    @DisplayName("Teste de integração ao vivo: UPE")
    void testLiveCollection() {
        UpeCollector collector = new UpeCollector();
        assertCollectorLive(collector, "UPE");
    }
}
