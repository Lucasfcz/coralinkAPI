package io.github.lucasfcz.coralink.services;

import io.github.lucasfcz.coralink.dto.NewsSummary;
import io.github.lucasfcz.coralink.mappers.RawOpportunityMapper;
import io.github.lucasfcz.coralink.model.RawOpportunity;
import io.github.lucasfcz.coralink.repositories.RawOpportunityRepository;
import io.github.lucasfcz.coralink.sources.collector.Collector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;
import java.util.stream.Collectors;

// class responsible for first AI screening to know if is relevant or not
// and then second AI extraction to extract relevant information from the opportunity.
// It also persists the results in the database.
@Service
@RequiredArgsConstructor
@Slf4j
public class  ScrapingService {

    private final List<Collector> collectors;
    private final RawOpportunityRepository rawOpportunityRepository;
    private final RawOpportunityMapper rawOpportunityMapper;

    public int collectAllNewOpportunitiesAndReturnQuantityCollected() {
        log.info("Registered collectors: {}", collectors.size());

        List<NewsSummary> allNews = collectFromAllSources();

        log.info("Total summaries collected from sources: {}", allNews.size());

        if (allNews.isEmpty()) {
            log.warn("No news summaries were collected");
            return 0;
        }

        Set<String> collectedUrls = allNews.stream()
                .map(NewsSummary::url)
                .collect(Collectors.toSet());

        Set<String> knownUrls = rawOpportunityRepository
                .findAllByNewsUrlIn(collectedUrls)
                .stream()
                .map(RawOpportunity::getNewsUrl)
                .collect(Collectors.toSet());

        log.info(
                "Collected URLs: {}, already known URLs: {}",
                collectedUrls.size(),
                knownUrls.size()
        );

        List<RawOpportunity> newOpportunities = allNews.stream()
                .collect(Collectors.toMap(
                        NewsSummary::url,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .filter(news -> !knownUrls.contains(news.url()))
                .map(rawOpportunityMapper::toEntity)
                .toList();

        log.info("New raw opportunities to persist: {}", newOpportunities.size());

        rawOpportunityRepository.saveAll(newOpportunities);

        return newOpportunities.size();
    }

    private List<NewsSummary> collectFromAllSources() {
        return collectors.stream()
                .flatMap(collector -> {
                    Instant start = Instant.now();
                    List<NewsSummary> collected;
                    try {
                        collected = collector.collect();
                    } catch (RuntimeException exception) {
                        log.error("Collector {} failed", collector.sourceName(), exception);
                        return List.<NewsSummary>of().stream();
                    }

                    log.info(
                            "Collector {} returned {} summaries in {} ms",
                            collector.sourceName(),
                            collected.size(),
                            Duration.between(start, Instant.now()).toMillis()
                    );

                    return collected.stream();
                })
                .toList();
    }
}
