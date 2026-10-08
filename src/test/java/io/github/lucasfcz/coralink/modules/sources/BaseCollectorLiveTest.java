package io.github.lucasfcz.coralink.modules.sources;

import io.github.lucasfcz.coralink.modules.sources.collector.Collector;
import io.github.lucasfcz.coralink.modules.sources.dto.DetailedContent;
import io.github.lucasfcz.coralink.modules.sources.dto.NewsSummary;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public abstract class BaseCollectorLiveTest {

    private static final Set<String> INVALID_TITLES = Set.of(
            "ler mais", "leia mais", "saiba mais", "veja mais", "mais informações", "mais informacoes",
            "preços", "precos", "criar evento grátis", "criar evento gratis", "criar evento", "organizar evento",
            "carregar mais", "ver todos", "ver mais", "clique aqui", "banner", "sem título", "sem titulo",
            "undefined", "null"
    );

    protected void assertCollectorLive(Collector collector, String expectedSourceName) {
        assertNotNull(collector, "Instância do coletor não pode ser nula");
        assertEquals(expectedSourceName, collector.sourceName(), "Nome da fonte difere do esperado");

        assertNotNull(collector.fallbackImageUrl(), "URL da imagem fallback não pode ser nula");
        assertFalse(collector.fallbackImageUrl().isBlank(), "URL da imagem fallback não pode ser vazia");
        assertTrue(collector.fallbackImageUrl().startsWith("http://") || collector.fallbackImageUrl().startsWith("https://"),
                "URL da imagem fallback deve iniciar com http/https: " + collector.fallbackImageUrl());

        System.out.printf("[%s] Disparando requisição HTTP real para coleta geral (collect)...%n", expectedSourceName);
        List<NewsSummary> summaries = collector.collect();

        assertNotNull(summaries, "O método collect() retornou null para a fonte " + expectedSourceName);
        assertFalse(summaries.isEmpty(), "A fonte " + expectedSourceName + " retornou 0 oportunidades (lista vazia). Verifique seletores, endpoints ou bloqueio do portal.");

        System.out.printf("[%s] Sucesso na coleta geral: %d oportunidades encontradas.%n", expectedSourceName, summaries.size());

        for (int i = 0; i < summaries.size(); i++) {
            NewsSummary s = summaries.get(i);
            assertNotNull(s, String.format("[%s] Item %d: o summary é nulo", expectedSourceName, i));

            // Validação de título
            assertNotNull(s.title(), String.format("[%s] Item %d: título não pode ser nulo", expectedSourceName, i));
            assertFalse(s.title().isBlank(), String.format("[%s] Item %d: título não pode ser em branco", expectedSourceName, i));
            assertTrue(s.title().trim().length() >= 4,
                    String.format("[%s] Item %d: título muito curto ('%s')", expectedSourceName, i, s.title()));

            String normalizedTitle = s.title().trim().toLowerCase(Locale.ROOT);
            assertFalse(INVALID_TITLES.contains(normalizedTitle),
                    String.format("[%s] Item %d: título capturado é um placeholder/botão inválido ('%s')", expectedSourceName, i, s.title()));

            // Validação de URL
            assertNotNull(s.url(), String.format("[%s] Item %d: URL não pode ser nula", expectedSourceName, i));
            assertFalse(s.url().isBlank(), String.format("[%s] Item %d: URL não pode ser em branco", expectedSourceName, i));
            assertTrue(s.url().startsWith("http://") || s.url().startsWith("https://"),
                    String.format("[%s] Item %d: URL deve começar com http:// ou https:// ('%s')", expectedSourceName, i, s.url()));

            // Validação de shortSummary
            assertNotNull(s.shortSummary(), String.format("[%s] Item %d: shortSummary não pode ser nulo", expectedSourceName, i));
            assertFalse(s.shortSummary().isBlank(), String.format("[%s] Item %d: shortSummary não pode ser em branco", expectedSourceName, i));

            // Validação de integridade de dados
            assertEquals(expectedSourceName, s.sourceName(), String.format("[%s] Item %d: sourceName divergente", expectedSourceName, i));
            assertNotNull(s.foundAt(), String.format("[%s] Item %d: foundAt não pode ser nulo", expectedSourceName, i));
        }

        // Validação da coleta detalhada no primeiro item coletado
        NewsSummary first = summaries.getFirst();
        System.out.printf("[%s] Disparando requisição HTTP real para coleta detalhada: '%s' (%s)...%n",
                expectedSourceName, first.title(), first.url());

        DetailedContent detailed = collector.detailedCollect(first.url());
        assertNotNull(detailed, String.format("[%s] detailedCollect retornou null para a URL: %s", expectedSourceName, first.url()));
        assertNotNull(detailed.fullContent(), String.format("[%s] fullContent é nulo para a URL: %s", expectedSourceName, first.url()));
        assertFalse(detailed.fullContent().isBlank(), String.format("[%s] fullContent é vazio/em branco para a URL: %s", expectedSourceName, first.url()));
        assertTrue(detailed.fullContent().trim().length() >= 50,
                String.format("[%s] fullContent insuficiente (%d caracteres) para a URL: %s",
                        expectedSourceName, detailed.fullContent().trim().length(), first.url()));

        System.out.printf("[%s] TESTE APROVADO! Total coletado: %d itens | Corpo detalhado verificado: %d caracteres.%n",
                expectedSourceName, summaries.size(), detailed.fullContent().trim().length());
    }
}
