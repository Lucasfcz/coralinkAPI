package io.github.lucasfcz.coralink.modules.sources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UnibraCollectorTest extends BaseCollectorLiveTest {

    @Test
    @DisplayName("Teste de integração ao vivo: UNIBRA")
    void testLiveCollection() {
        UnibraCollector collector = new UnibraCollector();
        assertCollectorLive(collector, "UNIBRA");
    }
}
