package io.github.lucasfcz.coralink.scheduler;

import io.github.lucasfcz.coralink.dto.PipelineRunResult;
import io.github.lucasfcz.coralink.services.PipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SourceCheckJob {

    private final PipelineService pipelineService;

    @Scheduled(fixedDelayString = "${coralink.scheduler.source-check-rate-ms:1000}")
    public void checkSources() {
        log.info("Starting Coralink pipeline");
        PipelineRunResult result = pipelineService.runFullPipeline();
        log.info("Finished pipeline: collected={}, relevant={}, created={}, failures={}", result.collected(),
                result.screenedRelevant(), result.createdOpportunities(), result.failures());
    }
}
