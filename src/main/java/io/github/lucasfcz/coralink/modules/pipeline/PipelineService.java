package io.github.lucasfcz.coralink.modules.pipeline;

import io.github.lucasfcz.coralink.modules.ai.ExtractionService;
import io.github.lucasfcz.coralink.modules.ai.ScreeningService;
import io.github.lucasfcz.coralink.modules.pipeline.dto.*;
import io.github.lucasfcz.coralink.modules.sources.dto.DetailedContent;
import io.github.lucasfcz.coralink.modules.ai.dto.ScreeningBatchResult;
import io.github.lucasfcz.coralink.modules.ai.dto.ScreeningResult;
import io.github.lucasfcz.coralink.modules.ai.dto.ExtractionBatchResult;
import io.github.lucasfcz.coralink.modules.ai.dto.ExtractionResult;
import io.github.lucasfcz.coralink.modules.pipeline.enums.PipelineStatus;
import io.github.lucasfcz.coralink.infra.exception.BadResponseException;
import io.github.lucasfcz.coralink.infra.exception.NotFoundException;
import io.github.lucasfcz.coralink.modules.pipeline.model.PipelineRun;
import io.github.lucasfcz.coralink.modules.pipeline.model.RawOpportunity;
import io.github.lucasfcz.coralink.modules.pipeline.repository.PipelineRunRepository;
import io.github.lucasfcz.coralink.modules.pipeline.repository.RawOpportunityRepository;
import io.github.lucasfcz.coralink.modules.sources.collector.Collector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final PipelineRunRepository pipelineRunRepository;
    private final RawOpportunityMapper rawOpportunityMapper;

    @Value("${coralink.scheduler.source-check-rate-ms:43200000}")
    private long schedulerRateMs;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Instant lastRunStartedAt;
    private volatile Instant lastRunFinishedAt;

    @Scheduled(fixedDelayString = "${coralink.scheduler.source-check-rate-ms}")
    @CacheEvict(value = {"opportunities", "opportunities_count"}, allEntries = true)
    public PipelineRunResult runFullPipeline() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Tentativa de execução simultânea do pipeline ignorada; o processo já está rodando.");
            return null;
        }

        Instant start = Instant.now();
        this.lastRunStartedAt = start;
        log.info("Pipeline started at {}", start);

        PipelineRun runRecord = createRunningRecord(start);

        try {
            int collected = scrapingService.collectAllNewOpportunitiesAndReturnQuantityCollected();
            assignRunToRecentRawOpportunities(runRecord.getId());

            PhaseResult screening = runScreeningPhase();
            PhaseResult extraction = runExtractionPhase();

            Instant finish = Instant.now();
            this.lastRunFinishedAt = finish;

            return finalizeSuccessRun(runRecord, start, finish, collected, screening, extraction);
        } catch (Exception e) {
            log.error("Erro fatal durante a execução do pipeline", e);
            finalizeFailedRun(runRecord, e);
            throw e;
        } finally {
            running.set(false);
        }
    }

    private PipelineRun createRunningRecord(Instant start) {
        PipelineRun record = new PipelineRun(start, PipelineStatus.RUNNING);
        return pipelineRunRepository.save(record);
    }

    private PipelineRunResult finalizeSuccessRun(
            PipelineRun runRecord,
            Instant start,
            Instant finish,
            int collected,
            PhaseResult screening,
            PhaseResult extraction
    ) {
        Duration duration = Duration.between(start, finish);
        int failures = screening.irrelevantFound() + extraction.irrelevantFound();

        runRecord.setFinishedAt(finish);
        runRecord.setStatus(PipelineStatus.SUCCESS);
        runRecord.setCollectedCount(collected);
        runRecord.setScreeningRelevantCount(screening.relevantFound());
        runRecord.setExtractionRelevantCount(extraction.relevantFound());
        runRecord.setFailuresCount(failures);
        runRecord.setDurationMs(duration.toMillis());
        pipelineRunRepository.save(runRecord);

        PipelineRunResult result = new PipelineRunResult(collected, screening.relevantFound(), extraction.relevantFound(), failures);

        log.info("Pipeline finished in {} — collected={}, screeningRelevant={}, extractionRelevant={}, failures={}",
                formatDuration(duration), collected, screening.relevantFound(), extraction.relevantFound(), failures);

        return result;
    }

    private void finalizeFailedRun(PipelineRun runRecord, Exception exception) {
        runRecord.setFinishedAt(Instant.now());
        runRecord.setStatus(PipelineStatus.FAILED);
        runRecord.setErrorMessage(exception.getMessage());
        pipelineRunRepository.save(runRecord);
    }

    private void assignRunToRecentRawOpportunities(Long pipelineRunId) {
        List<RawOpportunity> unassigned = rawOpportunityRepository.findByPipelineRunIdIsNull();
        for (RawOpportunity raw : unassigned) {
            raw.assignPipelineRun(pipelineRunId);
        }
        if (!unassigned.isEmpty()) {
            rawOpportunityRepository.saveAll(unassigned);
        }
    }

    @Transactional(readOnly = true)
    public PipelineStatusResponse getPipelineStatus() {
        boolean isRunning = running.get();
        PipelineRun lastRun = pipelineRunRepository.findTopByOrderByStartedAtDesc().orElse(null);

        Instant nextRunEstimatedAt = null;
        Long secondsRemaining = null;
        String formattedRemaining;

        if (isRunning) {
            formattedRemaining = "Em execução agora";
        } else if (lastRunFinishedAt != null || (lastRun != null && lastRun.getFinishedAt() != null)) {
            Instant baseFinish = lastRunFinishedAt != null ? lastRunFinishedAt : lastRun.getFinishedAt();
            nextRunEstimatedAt = baseFinish.plusMillis(schedulerRateMs);
            long diffSeconds = Duration.between(Instant.now(), nextRunEstimatedAt).getSeconds();
            secondsRemaining = Math.max(0, diffSeconds);
            formattedRemaining = formatRemainingTime(secondsRemaining);
        } else {
            formattedRemaining = "Aguardando primeira execução";
        }

        return new PipelineStatusResponse(
                isRunning,
                lastRunStartedAt != null ? lastRunStartedAt : (lastRun != null ? lastRun.getStartedAt() : null),
                lastRunFinishedAt != null ? lastRunFinishedAt : (lastRun != null ? lastRun.getFinishedAt() : null),
                nextRunEstimatedAt,
                secondsRemaining,
                formattedRemaining,
                lastRun != null ? toPipelineRunResponse(lastRun) : null
        );
    }

    @Transactional(readOnly = true)
    public Page<PipelineRunResponse> getPipelineRuns(Pageable pageable) {
        return pipelineRunRepository.findAllByOrderByStartedAtDesc(pageable)
                .map(this::toPipelineRunResponse);
    }

    @Transactional(readOnly = true)
    public Page<RawOpportunityResponse> getPipelineRunItems(Long runId, Pageable pageable) {
        return rawOpportunityRepository.findByPipelineRunId(runId, pageable)
                .map(rawOpportunityMapper::toResponse);
    }

    public void triggerPipelineAsynchronously() {
        if (running.get()) {
            throw new BadResponseException("O pipeline já está em execução no momento.");
        }
        CompletableFuture.runAsync(this::runFullPipeline);
    }

    private String formatRemainingTime(Long secondsRemaining) {
        long hours = secondsRemaining / 3600;
        long minutes = (secondsRemaining % 3600) / 60;
        long seconds = secondsRemaining % 60;
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
    }

    private String formatDuration(Duration duration) {
        long minutes = duration.toMinutes();
        long seconds = duration.minusMinutes(minutes).getSeconds();
        return "%dm%02ds".formatted(minutes, seconds);
    }

    public PipelineRunResponse toPipelineRunResponse(PipelineRun run) {
        return new PipelineRunResponse(
                run.getId(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getStatus(),
                run.getCollectedCount(),
                run.getScreeningRelevantCount(),
                run.getExtractionRelevantCount(),
                run.getFailuresCount(),
                run.getDurationMs(),
                run.getErrorMessage()
        );
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
        List<RawOpportunity> pending = rawOpportunityRepository.findByScreenedRelevantIsTrueAndBecameOpportunityIsFalseAndExtractionAttemptsLessThan(3);
        if (pending.isEmpty()) return PhaseResult.empty();

        Map<Long, DetailedContent> contents = collectDetailsFromPipeline(pending);
        List<RawOpportunity> ready = pending.stream().filter(raw -> contents.containsKey(raw.getId())).toList();
        int collectFailures = pending.size() - ready.size();

        if (ready.isEmpty()) return new PhaseResult(0, collectFailures);

        try {
            ExtractionBatchResult response = extractionService.extract(ready, contents);
            Map<Long, RawOpportunity> byId = indexById(ready);

            Set<Long> succeededIds = new HashSet<>();
            int successes = 0;
            int failures = collectFailures;
            for (ExtractionResult result : response.extractionResults()) {
                try {
                    RawOpportunity raw = byId.get(result.rawOpportunityId());
                    Collector collector = resolveCollector(raw.getSourceName());
                    persistenceService.saveOpportunity(raw, result, collector.fallbackImageUrl());
                    succeededIds.add(result.rawOpportunityId());
                    successes++;
                } catch (RuntimeException exception) {
                    failures++;
                    log.error("Failed to persist extracted opportunity {}", result.rawOpportunityId(), exception);
                    persistenceService.recordExtractionFailure(byId.get(result.rawOpportunityId()), "Erro de persistência: " + exception.getMessage());
                }
            }

            for (RawOpportunity raw : ready) {
                if (!succeededIds.contains(raw.getId())) {
                    persistenceService.recordExtractionFailure(raw, "IA não retornou dados estruturados válidos após tentativas");
                }
            }

            return new PhaseResult(successes, failures);

        } catch (RuntimeException exception) {
            log.error("Extraction batch failed", exception);
            for (RawOpportunity raw : ready) {
                persistenceService.recordExtractionFailure(raw, "Falha crítica na chamada de IA: " + exception.getMessage());
            }
            return new PhaseResult(0, collectFailures + ready.size());
        }
    }

    private Map<Long, DetailedContent> collectDetailsFromPipeline(List<RawOpportunity> rawOpportunities) {
        Map<Long, DetailedContent> contents = new HashMap<>();
        for (RawOpportunity raw : rawOpportunities) {
            try {
                Collector collector = resolveCollector(raw.getSourceName());
                DetailedContent detail = collector.detailedCollect(raw.getNewsUrl());
                if (detail != null && detail.fullContent() != null && !detail.fullContent().isBlank()) {
                    contents.put(raw.getId(), detail);
                } else {
                    persistenceService.recordExtractionFailure(raw, "Conteúdo detalhado ausente ou inacessível no portal de origem");
                }
            } catch (RuntimeException exception) {
                log.error("Failed to collect detail for raw opportunity {}", raw.getId(), exception);
                persistenceService.recordExtractionFailure(raw, "Erro ao acessar página de detalhes: " + exception.getMessage());
            }
        }
        return contents;
    }

    private Collector resolveCollector(String sourceName) {
        return collectors.stream()
                .filter(c -> c.sourceName().equalsIgnoreCase(sourceName))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No collector registered for " + sourceName));
    }

    private Map<Long, RawOpportunity> indexById(List<RawOpportunity> items) {
        return items.stream().collect(Collectors.toMap(RawOpportunity::getId, Function.identity()));
    }
}