package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.enums.SourceName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WordPressCollectorsTest {

    @Test
    void testCesarCollectorConfig() {
        CesarCollector collector = new CesarCollector();
        assertEquals(SourceName.CESAR, collector.sourceName());
        assertTrue(collector.baseUrl().contains("cesar.org.br"));
        assertTrue(collector.postsEndpoint().contains("wp-json"));
    }

    @Test
    void testCesarSchoolCollectorConfig() {
        CesarSchoolCollector collector = new CesarSchoolCollector();
        assertEquals(SourceName.CESAR_SCHOOL, collector.sourceName());
        assertTrue(collector.baseUrl().contains("cesar.school"));
    }

    @Test
    void testCinUfpeCollectorConfig() {
        CinUfpeCollector collector = new CinUfpeCollector();
        assertEquals(SourceName.CIN_UFPE, collector.sourceName());
        assertTrue(collector.baseUrl().contains("cin.ufpe.br"));
    }

    @Test
    void testIfpeCollectorConfig() {
        IfpeCollector collector = new IfpeCollector();
        assertEquals(SourceName.IFPE, collector.sourceName());
        assertTrue(collector.baseUrl().contains("ifpe.edu.br"));
        assertFalse(collector.campusPaths().isEmpty());

        String recifeEndpoint = collector.singlePostEndpoint("meu-post", "https://portal.ifpe.edu.br/recife/noticias/meu-post");
        assertEquals("https://portal.ifpe.edu.br/recife/wp-json/wp/v2/posts?slug=meu-post", recifeEndpoint);

        String paulistaEndpoint = collector.singlePostEndpoint("evento-ti", "https://portal.ifpe.edu.br/paulista/noticias/evento-ti/");
        assertEquals("https://portal.ifpe.edu.br/paulista/wp-json/wp/v2/posts?slug=evento-ti", paulistaEndpoint);

        String genericEndpoint = collector.singlePostEndpoint("geral", "https://portal.ifpe.edu.br/noticias/geral");
        assertEquals("https://portal.ifpe.edu.br/wp-json/wp/v2/posts?slug=geral", genericEndpoint);
    }

    @Test
    void testStandardSinglePostEndpoint() {
        CesarCollector cesarCollector = new CesarCollector();
        assertEquals("https://www.cesar.org.br/painel/wp-json/wp/v2/posts?slug=noticia-1",
                cesarCollector.singlePostEndpoint("noticia-1", "https://www.cesar.org.br/w/noticia-1"));

        CinUfpeCollector cinCollector = new CinUfpeCollector();
        assertEquals("https://portal.cin.ufpe.br/wp-json/wp/v2/posts?slug=noticia-2",
                cinCollector.singlePostEndpoint("noticia-2", "https://portal.cin.ufpe.br/noticia-2"));
    }

    @Test
    void testUnifafireCollectorConfig() {
        UnifafireCollector collector = new UnifafireCollector();
        assertEquals(SourceName.UNIFAFIRE, collector.sourceName());
        assertTrue(collector.baseUrl().contains("unifafire.edu.br"));
    }

    @Test
    void testUpeCollectorConfig() {
        UpeCollector collector = new UpeCollector();
        assertEquals(SourceName.UPE, collector.sourceName());
        assertTrue(collector.baseUrl().contains("upe.br"));
    }
}
