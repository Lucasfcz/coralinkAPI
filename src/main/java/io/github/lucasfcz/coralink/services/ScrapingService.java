package io.github.lucasfcz.coralink.services;

import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.mappers.RawOpportunityMapper;
import io.github.lucasfcz.coralink.model.RawOpportunity;
import io.github.lucasfcz.coralink.repositories.RawOpportunityRepository;
import io.github.lucasfcz.coralink.sources.Collector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// class responsible for first AI screening to know if is relevant or not
// and then second AI extraction to extract relevant information from the opportunity.
// It also persists the results in the database.
@Service
@RequiredArgsConstructor
public class ScrapingService {

    private final List<Collector> collectors;
    private final RawOpportunityRepository rawOpportunityRepository;
    private final RawOpportunityMapper rawOpportunityMapper;

    public int collectAllNewOpportunitiesAndReturnQuantityCollected() {
        List<NewsSummary> allNews = collectFromAllSources();
        if (allNews.isEmpty()) {
            return 0;
        }

        Set<String> knownUrls = rawOpportunityRepository.findAllByNewsUrlIn(
                        allNews.stream().map(NewsSummary::url).collect(Collectors.toSet()))
                .stream()
                .map(RawOpportunity::getNewsUrl)
                .collect(Collectors.toSet());

        List<RawOpportunity> newOpportunities = allNews.stream()
                .collect(Collectors.toMap(NewsSummary::url, Function.identity(), (first, ignored) -> first, LinkedHashMap::new))
                .values().stream()
                .filter(news -> !knownUrls.contains(news.url()))
                .map(rawOpportunityMapper::toEntity)
                .toList();

        rawOpportunityRepository.saveAll(newOpportunities);
        return newOpportunities.size();
    }

    private List<NewsSummary> collectFromAllSources() {
        return collectors.stream()
                .flatMap(collector -> collector.collect().stream())
                .toList();
    }

}
