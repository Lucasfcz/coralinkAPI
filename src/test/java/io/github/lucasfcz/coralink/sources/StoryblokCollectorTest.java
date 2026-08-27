package io.github.lucasfcz.coralink.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.sources.collector.StoryblokCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class StoryblokCollectorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestStoryblokCollector collector;

    private static class TestStoryblokCollector extends StoryblokCollector {
        @Override
        protected String baseUrl() {
            return "https://example.com";
        }

        @Override
        protected String imageFallBackUrl() {
            return "https://example.com/fallback.png";
        }

        @Override
        protected String apiToken() {
            return "test-token-123";
        }

        @Override
        protected String storiesEndpoint() {
            return "https://api.storyblok.com/v1/cdn/stories?token=" + apiToken();
        }

        @Override
        protected String singleStoryEndpointTemplate() {
            return "https://api.storyblok.com/v1/cdn/stories/noticias/%s?token=%s&version=published";
        }

        @Override
        public SourceName sourceName() {
            return SourceName.PORTO_DIGITAL;
        }

        @Override
        protected NewsSummary mapStory(JsonNode story) {
            return null;
        }

        // Expor métodos protegidos para teste
        public String testExtractRichText(JsonNode node) {
            return extractRichText(node);
        }

        public String testExtractFirstParagraph(JsonNode node) {
            return extractFirstParagraph(node);
        }

        public LocalDateTime testParseStoryblokDate(JsonNode story) {
            return parseStoryblokDate(story);
        }
    }

    @BeforeEach
    void setUp() {
        collector = new TestStoryblokCollector();
    }

    @Test
    @DisplayName("Should extract complex RichText AST with paragraphs, headings, bullet lists, and hard breaks")
    void shouldExtractComplexRichTextAst() throws Exception {
        String json = """
                {
                    "type": "doc",
                    "content": [
                        {
                            "type": "heading",
                            "content": [
                                { "type": "text", "text": "Título Principal" }
                            ]
                        },
                        {
                            "type": "paragraph",
                            "content": [
                                { "type": "text", "text": "Primeiro parágrafo introdutório." },
                                { "type": "hard_break" },
                                { "type": "text", "text": "Linha após quebra direta." }
                            ]
                        },
                        {
                            "type": "bullet_list",
                            "content": [
                                {
                                    "type": "list_item",
                                    "content": [
                                        {
                                            "type": "paragraph",
                                            "content": [
                                                { "type": "text", "text": "Item 1 da lista" }
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "type": "list_item",
                                    "content": [
                                        {
                                            "type": "paragraph",
                                            "content": [
                                                { "type": "text", "text": "Item 2 da lista" }
                                            ]
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
                """;

        JsonNode root = MAPPER.readTree(json);
        String extracted = collector.testExtractRichText(root);

        assertNotNull(extracted);
        assertTrue(extracted.contains("Título Principal"));
        assertTrue(extracted.contains("Primeiro parágrafo introdutório."));
        assertTrue(extracted.contains("Linha após quebra direta."));
        assertTrue(extracted.contains("- Item 1 da lista"));
        assertTrue(extracted.contains("- Item 2 da lista"));
    }

    @Test
    @DisplayName("Should return empty string for null or missing AST nodes")
    void shouldReturnEmptyStringForNullOrMissingNodes() {
        assertEquals("", collector.testExtractRichText(null));
        assertEquals("", collector.testExtractRichText(MAPPER.nullNode()));
        assertEquals("", collector.testExtractRichText(MAPPER.missingNode()));
    }

    @Test
    @DisplayName("Should extract first paragraph cleanly")
    void shouldExtractFirstParagraphCleanly() throws Exception {
        String json = """
                {
                    "type": "doc",
                    "content": [
                        {
                            "type": "paragraph",
                            "content": [
                                { "type": "text", "text": "Este é o primeiro parágrafo de resumo." }
                            ]
                        },
                        {
                            "type": "paragraph",
                            "content": [
                                { "type": "text", "text": "Este é o segundo parágrafo que não deve entrar no resumo." }
                            ]
                        }
                    ]
                }
                """;

        JsonNode root = MAPPER.readTree(json);
        String firstParagraph = collector.testExtractFirstParagraph(root);

        assertEquals("Este é o primeiro parágrafo de resumo.", firstParagraph);
    }

    @Test
    @DisplayName("Should parse author post_date format (yyyy-MM-dd HH:mm)")
    void shouldParseAuthorPostDate() throws Exception {
        String json = """
                {
                    "content": {
                        "post_date": "2026-07-21 14:30"
                    }
                }
                """;

        JsonNode story = MAPPER.readTree(json);
        LocalDateTime date = collector.testParseStoryblokDate(story);

        assertEquals(LocalDateTime.of(2026, 7, 21, 14, 30), date);
    }

    @Test
    @DisplayName("Should parse system first_published_at format (ISO-8601 UTC)")
    void shouldParseSystemFirstPublishedAt() throws Exception {
        String json = """
                {
                    "content": {},
                    "first_published_at": "2026-07-21T15:08:47.107Z"
                }
                """;

        JsonNode story = MAPPER.readTree(json);
        LocalDateTime date = collector.testParseStoryblokDate(story);

        assertEquals(LocalDateTime.of(2026, 7, 21, 15, 8, 47, 107000000), date);
    }

    @Test
    @DisplayName("Should fallback to current date when date fields are missing or empty")
    void shouldFallbackToCurrentDateWhenMissing() throws Exception {
        String json = """
                {
                    "content": {}
                }
                """;

        JsonNode story = MAPPER.readTree(json);
        LocalDateTime date = collector.testParseStoryblokDate(story);

        assertNotNull(date);
        assertTrue(date.isAfter(LocalDateTime.now().minusMinutes(1)));
    }
}
