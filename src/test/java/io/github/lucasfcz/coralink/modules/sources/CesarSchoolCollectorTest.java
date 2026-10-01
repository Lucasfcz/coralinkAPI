package io.github.lucasfcz.coralink.modules.sources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CesarSchoolCollectorTest extends BaseCollectorLiveTest {

    @Test
    @DisplayName("Teste de integração ao vivo: CESAR_SCHOOL")
    void testLiveCollection() {
        CesarSchoolCollector collector = new CesarSchoolCollector();
        assertCollectorLive(collector, "CESAR_SCHOOL");
    }
}
