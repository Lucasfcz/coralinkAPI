package io.github.lucasfcz.coralink.services;

import io.github.lucasfcz.coralink.ai.ExtractionService;
import io.github.lucasfcz.coralink.ai.ScreeningService;
import io.github.lucasfcz.coralink.dto.*;
import io.github.lucasfcz.coralink.exceptions.NotFoundException;
import io.github.lucasfcz.coralink.model.RawOpportunity;
import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.repositories.RawOpportunityRepository;
import io.github.lucasfcz.coralink.sources.collector.Collector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PipelineService {

    private final ScrapingService scrapingService;
    private final RawOpportunityRepository rawOpportunityRepository;
    private final ScreeningService screeningService;
    private final ExtractionService extractionService;
    private final PipelinePersistenceService persistenceService;
    private final List<Collector> collectors;

    @Scheduled(fixedDelayString = "${coralink.scheduler.source-check-rate-ms}")
    @CacheEvict(value = {"opportunities", "opportunities_count"}, allEntries = true)
    public PipelineRunResult runFullPipeline() {
        Instant start = Instant.now();
        log.info("Pipeline started");

        int collected = scrapingService.collectAllNewOpportunitiesAndReturnQuantityCollected();
        PhaseResult screening = runScreeningPhase();
        PhaseResult extraction = runExtractionPhase();

        PipelineRunResult result = new PipelineRunResult(collected, screening.relevantFound(), extraction.relevantFound(),
                screening.irrelevantFound() + extraction.irrelevantFound());

        Duration duration = Duration.between(start, Instant.now());
        log.info("Pipeline finished in {} — collected={}, screeningRelevant={}, extractionRelevant={}, failures={}",
                formatDuration(duration), collected, screening.relevantFound(), extraction.relevantFound(),
                screening.irrelevantFound() + extraction.irrelevantFound());

        return result;
    }

    private String formatDuration(Duration duration) {
        long minutes = duration.toMinutes();
        long seconds = duration.minusMinutes(minutes).getSeconds();
        return "%dm%02ds".formatted(minutes, seconds);
    }
    private PhaseResult runScreeningPhase() {
        List<RawOpportunity> rawOpportunities = rawOpportunityRepository.findByScreenedRelevantIsNull();
        if (rawOpportunities.isEmpty()) return PhaseResult.empty();

        try {
            ScreeningBatchResult response = screeningService.screen(rawOpportunities);
            Map<Long, RawOpportunity> rawOpportunityById = indexById(rawOpportunities);

            int relevant = 0;
            int failures = 0;
            for (ScreeningResult result : response.screeningResults()) {
                try {
                    persistenceService.saveScreening(rawOpportunityById.get(result.rawOpportunityId()), result);
                    if (result.isRelevant()) relevant++;
                } catch (RuntimeException exception) {
                    failures++;
                    log.error("Failed to persist screening result for raw opportunity {}", result.rawOpportunityId(), exception);
                }
            }
            return new PhaseResult(relevant, failures);

        } catch (RuntimeException exception) {
            log.error("Screening batch failed", exception);
            return new PhaseResult(0, rawOpportunities.size());
        }
    }

    private PhaseResult runExtractionPhase() {
        List<RawOpportunity> pending = rawOpportunityRepository.findByScreenedRelevantIsTrueAndBecameOpportunityIsFalse();
        if (pending.isEmpty()) return PhaseResult.empty();

        Map<Long, DetailedContent> contents = collectDetails(pending);
        List<RawOpportunity> ready = pending.stream().filter(raw -> contents.containsKey(raw.getId())).toList();
        int collectFailures = pending.size() - ready.size();

        if (ready.isEmpty()) return new PhaseResult(0, collectFailures);

        try {
            ExtractionBatchResult response = extractionService.extract(ready, contents);
            Map<Long, RawOpportunity> byId = indexById(ready);

            int successes = 0;
            int failures = collectFailures;
            for (ExtractionResult result : response.extractionResults()) {
                try {
                    RawOpportunity raw = byId.get(result.rawOpportunityId());
                    persistenceService.saveOpportunity(raw, result, contents.get(raw.getId()));
                    successes++;
                } catch (RuntimeException exception) {
                    failures++;
                    log.error("Failed to persist extracted opportunity {}", result.rawOpportunityId(), exception);
                }
            }
            return new PhaseResult(successes, failures);

        } catch (RuntimeException exception) {
            log.error("Extraction batch failed", exception);
            return new PhaseResult(0, collectFailures + ready.size());
        }
    }

    private Map<Long, DetailedContent> collectDetails(List<RawOpportunity> rawOpportunities) {
        Map<Long, DetailedContent> contents = new HashMap<>();
        for (RawOpportunity raw : rawOpportunities) {
            try {
                Collector collector = resolveCollector(raw.getSourceName());
                DetailedContent detail = collector.detailedCollect(raw.getNewsUrl());
                if (detail != null && detail.fullContent() != null && !detail.fullContent().isBlank()) {
                    contents.put(raw.getId(), detail);
                }
            } catch (RuntimeException exception) {
                log.error("Failed to collect detail for raw opportunity {}", raw.getId(), exception);
            }
        }
        return contents;
    }

    private Collector resolveCollector(SourceName sourceName) {
        return collectors.stream()
                .filter(c -> c.sourceName() == sourceName)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No collector registered for " + sourceName));
    }

    private Map<Long, RawOpportunity> indexById(List<RawOpportunity> items) {
        return items.stream().collect(Collectors.toMap(RawOpportunity::getId, Function.identity()));
    }
}