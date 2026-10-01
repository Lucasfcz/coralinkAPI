package io.github.lucasfcz.coralink.modules.sources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecnplayCollectorTest extends BaseCollectorLiveTest {

    @Test
    @DisplayName("Teste de integração ao vivo: RECNPLAY")
    void testLiveCollection() {
        RecnplayCollector collector = new RecnplayCollector();
        assertCollectorLive(collector, "RECNPLAY");
    }
}
