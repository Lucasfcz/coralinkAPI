package io.github.lucasfcz.coralink.modules.sources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class Even3CollectorTest extends BaseCollectorLiveTest {

    @Test
    @DisplayName("Teste de integração ao vivo: EVEN3")
    void testLiveCollection() {
        Even3Collector collector = new Even3Collector();
        assertCollectorLive(collector, "EVEN3");
    }
}
