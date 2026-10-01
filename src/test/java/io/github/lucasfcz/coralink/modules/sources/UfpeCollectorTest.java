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
}
