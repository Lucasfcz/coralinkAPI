package io.github.lucasfcz.coralink.services;

import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.mappers.RawOpportunityMapper;
import io.github.lucasfcz.coralink.model.RawOpportunity;
import io.github.lucasfcz.coralink.repositories.RawOpportunityRepository;
import io.github.lucasfcz.coralink.sources.collector.Collector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScrapingServiceTest {

    @Mock
    private Collector collector1;

    @Mock
    private Collector collector2;

    @Mock
    private RawOpportunityRepository rawOpportunityRepository;

    @Spy
    private RawOpportunityMapper rawOpportunityMapper = new RawOpportunityMapper();

    @Test
    @DisplayName("Should collect and persist only new opportunities")
    void shouldCollectAndPersistOnlyNewOpportunities() {
        when(collector1.sourceName()).thenReturn(SourceName.UFPE);
        when(collector2.sourceName()).thenReturn(SourceName.PORTO_DIGITAL);

        NewsSummary item1 = new NewsSummary(
                "Notícia 1", "Resumo 1", "https://example.com/1", SourceName.UFPE, LocalDateTime.now()
        );
        NewsSummary item2 = new NewsSummary(
                "Notícia 2", "Resumo 2", "https://example.com/2", SourceName.PORTO_DIGITAL, LocalDateTime.now()
        );

        when(collector1.collect()).thenReturn(List.of(item1));
        when(collector2.collect()).thenReturn(List.of(item2));

        RawOpportunity existingEntity = RawOpportunity.builder()
                .newsUrl("https://example.com/1")
                .build();

        when(rawOpportunityRepository.findAllByNewsUrlIn(Set.of("https://example.com/1", "https://example.com/2")))
                .thenReturn(List.of(existingEntity));

        ScrapingService scrapingService = new ScrapingService(
                List.of(collector1, collector2),
                rawOpportunityRepository,
                rawOpportunityMapper
        );

        int persistedCount = scrapingService.collectAllNewOpportunitiesAndReturnQuantityCollected();

        assertEquals(1, persistedCount);
        verify(rawOpportunityRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Should return 0 when all collectors return empty")
    void shouldReturnZeroWhenNoOpportunitiesCollected() {
        when(collector1.sourceName()).thenReturn(SourceName.UFPE);
        when(collector1.collect()).thenReturn(List.of());

        ScrapingService scrapingService = new ScrapingService(
                List.of(collector1),
                rawOpportunityRepository,
                rawOpportunityMapper
        );

        int count = scrapingService.collectAllNewOpportunitiesAndReturnQuantityCollected();

        assertEquals(0, count);
        verify(rawOpportunityRepository, never()).saveAll(anyList());
    }
}
