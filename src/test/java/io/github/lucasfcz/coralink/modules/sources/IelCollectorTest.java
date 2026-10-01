package io.github.lucasfcz.coralink.modules.sources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IelCollectorTest extends BaseCollectorLiveTest {

    @Test
    @DisplayName("Teste de integração ao vivo: IEL")
    void testLiveCollection() {
        IelCollector collector = new IelCollector();
        assertCollectorLive(collector, "IEL");
    }
}
