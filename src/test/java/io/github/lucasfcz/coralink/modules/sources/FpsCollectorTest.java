package io.github.lucasfcz.coralink.modules.sources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FpsCollectorTest extends BaseCollectorLiveTest {

    @Test
    @DisplayName("Teste de integração ao vivo: FPS")
    void testLiveCollection() {
        FpsCollector collector = new FpsCollector();
        assertCollectorLive(collector, "FPS");
    }
}
