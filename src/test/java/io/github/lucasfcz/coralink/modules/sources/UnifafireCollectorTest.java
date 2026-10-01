package io.github.lucasfcz.coralink.modules.sources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UnifafireCollectorTest extends BaseCollectorLiveTest {

    @Test
    @DisplayName("Teste de integração ao vivo: UNIFAFIRE")
    void testLiveCollection() {
        UnifafireCollector collector = new UnifafireCollector();
        assertCollectorLive(collector, "UNIFAFIRE");
    }
}
