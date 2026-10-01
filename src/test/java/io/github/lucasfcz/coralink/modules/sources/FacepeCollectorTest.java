package io.github.lucasfcz.coralink.modules.sources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FacepeCollectorTest extends BaseCollectorLiveTest {

    @Test
    @DisplayName("Teste de integração ao vivo: FACEPE")
    void testLiveCollection() {
        FacepeCollector collector = new FacepeCollector();
        assertCollectorLive(collector, "FACEPE");
    }
}
