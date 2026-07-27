package io.github.lucasfcz.coralink.controller;

import io.github.lucasfcz.coralink.services.ScrapingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/opportunities")
@RequiredArgsConstructor
public class ScrapingController {

    private final ScrapingService scrapingService;

    @PostMapping("/collect")
    public ResponseEntity<Integer> collect() {
        int quantity = scrapingService.collectAllNewOpportunitiesAndReturnQuantityCollected();
        return ResponseEntity.ok(quantity);
    }
}
