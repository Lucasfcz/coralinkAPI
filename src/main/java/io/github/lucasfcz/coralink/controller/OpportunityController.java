package io.github.lucasfcz.coralink.controller;

import io.github.lucasfcz.coralink.dto.OpportunityResponse;
import io.github.lucasfcz.coralink.enums.*;
import io.github.lucasfcz.coralink.services.OpportunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/opportunities")
@RequiredArgsConstructor
public class OpportunityController {

    private final OpportunityService opportunityService;

    @GetMapping
    public ResponseEntity<Page<OpportunityResponse>> getOpportunities(
            @RequestParam(required = false) OpportunityType type,
            @RequestParam(required = false) Set<TargetCourseAudience> targetCourseAudience,
            @RequestParam(required = false) Modality modality,
            @RequestParam(required = false) Boolean isFree,
            @RequestParam(required = false) Boolean isForAll,
            Pageable pageable) {
        return ResponseEntity.ok(opportunityService.getRelevantOpportunities(type, targetCourseAudience, modality, isFree, isForAll, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<OpportunityResponse>> findOpportunityByTitle(@RequestParam String title, Pageable pageable) {
        return ResponseEntity.ok(opportunityService.getOpportunitiesByTitle(title, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OpportunityResponse> findOpportunityById(@PathVariable Long id) {
        return ResponseEntity.ok(opportunityService.getOpportunityById(id));
    }

    @GetMapping("/quantity")
    public ResponseEntity<Integer> quantityOfOpportunities() {
        return ResponseEntity.ok(opportunityService.howManyOpportunitiesAreUpcoming());
    }
}
