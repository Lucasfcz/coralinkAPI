package io.github.lucasfcz.coralink.modules.sources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UnicapCollectorTest extends BaseCollectorLiveTest {

    @Test
    @DisplayName("Teste de integração ao vivo: UNICAP")
    void testLiveCollection() {
        UnicapCollector collector = new UnicapCollector();
        assertCollectorLive(collector, "UNICAP");
    }
}
