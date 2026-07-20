package io.github.lucasfcz.coralink.services;

import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.mappers.RawOpportunityMapper;
import io.github.lucasfcz.coralink.model.RawOpportunity;
import io.github.lucasfcz.coralink.repositories.RawOpportunityRepository;
import io.github.lucasfcz.coralink.sources.Collector;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScrapingService {

    private final List<Collector> collectors;
    private final RawOpportunityRepository rawOpportunityRepository;
    private final RawOpportunityMapper rawOpportunityMapper;

    @Transactional
    public void collectAllNewOpportunities() {
        List<NewsSummary> allNews = collectFromAllSources();

        List<RawOpportunity> newOpportunities = allNews.stream()
                .filter(this::isNew) // if news url already exists in database will trow false and don't pass to next step
                .map(rawOpportunityMapper::toEntity)
                .toList();

        rawOpportunityRepository.saveAll(newOpportunities);
    }

    private List<NewsSummary> collectFromAllSources() {
        return collectors.stream()
                .flatMap(collector -> collector.collect().stream())
                .toList();
    }

    // Uses "!" because if the news already exists, we don't want to pass it to the next step of the pipeline
    private boolean isNew(NewsSummary news) {
        return !rawOpportunityRepository.existsByNewsUrl(news.url());
    }
}
