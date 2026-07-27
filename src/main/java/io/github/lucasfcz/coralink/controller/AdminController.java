package io.github.lucasfcz.coralink.controller;

import io.github.lucasfcz.coralink.dto.PipelineRunResult;
import io.github.lucasfcz.coralink.services.OpportunityService;
import io.github.lucasfcz.coralink.services.PipelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final PipelineService pipelineService;

    @PostMapping("pipeline/run")
    public PipelineRunResult run() { return pipelineService.runFullPipeline(); }

}
