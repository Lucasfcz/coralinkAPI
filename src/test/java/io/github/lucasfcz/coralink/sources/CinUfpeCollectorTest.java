package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CinUfpeCollectorTest {

    private static final String NEWS_URL = "https://portal.cin.ufpe.br/2026/07/24/curso-de-extensao-principios-matematicos-para-computacao-abre-inscricoes-para-reforcar-a-base-dos-estudantes-antes-do-semestre-2026-2/";

    @Test
    void detailedCollectShouldReturnContentAndImageForSpecificNewsUrl() {
        CinUfpeCollector collector = new CinUfpeCollector();
        DetailedContent result = collector.detailedCollect(NEWS_URL);

        assertNotNull(result);
        assertNotNull(result.fullContent());
        assertFalse(result.fullContent().isBlank());
        assertTrue(result.fullContent().length() > 100);
    }
}
