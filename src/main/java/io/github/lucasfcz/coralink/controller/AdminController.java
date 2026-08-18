package io.github.lucasfcz.coralink.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    // before MVP and had a relevant audience, will expand to use login and this class will be for admin have controll about all statistics of system

    @GetMapping("/healthy-check")
    public String healthyCheck() {
        return "Everything is OK";
    }
}
