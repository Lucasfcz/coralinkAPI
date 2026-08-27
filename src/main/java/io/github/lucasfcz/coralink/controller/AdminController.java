package io.github.lucasfcz.coralink.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    // Controlador reservado para funcionalidades administrativas pré-MVP.
    // Futuramente receberá autenticação via Spring Security e painel com métricas do sistema.


    @GetMapping("/healthy-check")
    public String healthyCheck() {
        return "Everything is OK";
    }
}
