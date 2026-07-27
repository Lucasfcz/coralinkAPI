package io.github.lucasfcz.coralink.services;

import io.github.lucasfcz.coralink.dto.CollectionResult;
import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.repositories.RawOpportunityRepository;
import io.github.lucasfcz.coralink.sources.Collector;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ScrapingServiceTest {

    @Test
    void isolatesSourceErrorsAndContinuesCollection() {
        Collector failingCollector = mock(Collector.class);
        when(failingCollector.sourceName()).thenReturn(SourceName.CIN_UFPE);
        when(failingCollector.collect()).thenThrow(new RuntimeException("Source timeout"));

        Collector workingCollector = mock(Collector.class);
        when(workingCollector.sourceName()).thenReturn(SourceName.CIN_UFPE);
        NewsSummary news = new NewsSummary("Title 1", "Summary 1", "https://news.url/1", SourceName.CIN_UFPE, LocalDateTime.now());
        when(workingCollector.collect()).thenReturn(List.of(news));

        RawOpportunityRepository repository = mock(RawOpportunityRepository.class);
        when(repository.insertIfNotExists(anyString(), anyString(), anyString(), anyString())).thenReturn(1);

        ScrapingService scrapingService = new ScrapingService(List.of(failingCollector, workingCollector), repository);

        CollectionResult result = scrapingService.collectAllNewOpportunitiesAndReturnQuantityCollected();

        assertEquals(1, result.collected());
        assertEquals(1, result.sourceFailures());
        verify(repository, times(1)).insertIfNotExists("Title 1", "Summary 1", "https://news.url/1", "CIN_UFPE");
    }
}
