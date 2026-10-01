package io.github.lucasfcz.coralink.modules.sources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CinUfpeCollectorTest extends BaseCollectorLiveTest {

    @Test
    @DisplayName("Teste de integração ao vivo: CIN_UFPE")
    void testLiveCollection() {
        CinUfpeCollector collector = new CinUfpeCollector();
        assertCollectorLive(collector, "CIN_UFPE");
    }
}
