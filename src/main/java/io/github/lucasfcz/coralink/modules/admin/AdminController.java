package io.github.lucasfcz.coralink.modules.admin;

import io.github.lucasfcz.coralink.infra.config.OpenApiConfig;
import io.github.lucasfcz.coralink.modules.admin.dto.AdminOpportunityTypeUpdateRequest;
import io.github.lucasfcz.coralink.modules.admin.dto.AdminOpportunityUpdateRequest;
import io.github.lucasfcz.coralink.modules.admin.dto.DashboardMetricsResponse;
import io.github.lucasfcz.coralink.modules.opportunity.dto.OpportunityResponse;
import io.github.lucasfcz.coralink.modules.pipeline.dto.PipelineRunResponse;
import io.github.lucasfcz.coralink.modules.pipeline.dto.PipelineStatusResponse;
import io.github.lucasfcz.coralink.modules.pipeline.dto.RawOpportunityResponse;
import io.github.lucasfcz.coralink.modules.opportunity.OpportunityService;
import io.github.lucasfcz.coralink.modules.pipeline.PipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador de Operações Administrativas da Coralink API.
 * <p>
 * REQUISITO MANDATÓRIO DE SEGURANÇA:
 * Todos os métodos desta classe são estritamente restritos a usuários com a autoridade 'ROLE_ADMIN'.
 * Essa restrição é aplicada em duas camadas independentes (Defense-in-Depth):
 * 1. Na camada de filtro HTTP via {@code SecurityFilterChain} (.requestMatchers("/admin/**").hasRole("ADMIN"))
 * 2. Na camada de reflexão de métodos via anotação {@code @PreAuthorize("hasRole('ADMIN')")}
 * <p>
 * ARQUITETURA EM CAMADAS:
 * Este controlador não possui acesso direto a nenhum repositório, delegando toda a regra de negócio
 * para a camada de serviços (Service Layer) e trafegando exclusivamente DTOs.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administração", description = "Endpoints de controle, auditoria e métricas restritos exclusivamente a administradores (ROLE_ADMIN)")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class AdminController {

    private final PipelineService pipelineService;
    private final OpportunityService opportunityService;
    private final AdminDashboardService adminDashboardService;

    @Operation(
            summary = "Status da Esteira de Scraping e IA",
            description = "Retorna o status em tempo real do agendador e da execução atual do pipeline, incluindo contagem regressiva para o próximo ciclo."
    )
    @ApiResponse(responseCode = "200", description = "Status do pipeline retornado com sucesso")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @ApiResponse(responseCode = "403", description = "Acesso proibido (usuário comum sem papel de administrador)")
    @GetMapping("/pipeline/status")
    public ResponseEntity<PipelineStatusResponse> getPipelineStatus() {
        return ResponseEntity.ok(pipelineService.getPipelineStatus());
    }

    @Operation(
            summary = "Disparo Manual do Pipeline",
            description = "Dispara a execução do pipeline de scraping e IA em segundo plano de forma assíncrona, sem aguardar o intervalo do agendador."
    )
    @ApiResponse(responseCode = "202", description = "Execução iniciada em segundo plano")
    @ApiResponse(responseCode = "400", description = "O pipeline já está em execução no momento")
    @PostMapping("/pipeline/trigger")
    public ResponseEntity<Map<String, String>> triggerPipeline() {
        pipelineService.triggerPipelineAsynchronously();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "status", "ACCEPTED",
                "message", "Execução do pipeline iniciada em segundo plano com sucesso"
        ));
    }

    @Operation(
            summary = "Histórico de Execuções do Pipeline",
            description = "Lista de forma paginada o histórico de todas as rodadas de coleta com estatísticas de conversão e tempo de execução."
    )
    @ApiResponse(responseCode = "200", description = "Página de execuções retornada com sucesso")
    @GetMapping("/pipeline/runs")
    public ResponseEntity<Page<PipelineRunResponse>> getPipelineRuns(Pageable pageable) {
        return ResponseEntity.ok(pipelineService.getPipelineRuns(pageable));
    }

    @Operation(
            summary = "Itens Coletados em uma Execução Específica",
            description = "Lista todas as notícias brutas capturadas pelos coletores em uma rodada específica do pipeline."
    )
    @ApiResponse(responseCode = "200", description = "Página de matérias brutas retornada com sucesso")
    @GetMapping("/pipeline/runs/{id}/items")
    public ResponseEntity<Page<RawOpportunityResponse>> getPipelineRunItems(
            @Parameter(description = "Identificador da rodada do pipeline") @PathVariable Long id,
            Pageable pageable
    ) {
        return ResponseEntity.ok(pipelineService.getPipelineRunItems(id, pageable));
    }

    @Operation(
            summary = "Oportunidades Brutas com Falha na Extração",
            description = "Lista todas as matérias brutas triadas positivamente que atingiram o limite de tentativas de extração pela IA sem sucesso."
    )
    @ApiResponse(responseCode = "200", description = "Página de oportunidades que falharam na extração retornada com sucesso")
    @GetMapping("/pipeline/failed-extractions")
    public ResponseEntity<Page<RawOpportunityResponse>> getFailedExtractions(Pageable pageable) {
        return ResponseEntity.ok(adminDashboardService.getFailedExtractions(pageable));
    }

    @Operation(
            summary = "Atualização Administrativa de Oportunidade",
            description = "Permite corrigir títulos, prazos, resumos ou filtros de uma oportunidade após a extração por IA. Invalida o cache Redis automaticamente."
    )
    @ApiResponse(responseCode = "200", description = "Oportunidade atualizada com sucesso")
    @ApiResponse(responseCode = "404", description = "Oportunidade não encontrada")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/opportunities/{id}")
    public ResponseEntity<OpportunityResponse> updateOpportunity(
            @Parameter(description = "Identificador da oportunidade") @PathVariable Long id,
            @Valid @RequestBody AdminOpportunityUpdateRequest request
    ) {
        return ResponseEntity.ok(opportunityService.updateOpportunity(id, request));
    }

    @Operation(
            summary = "Atualização Rápida de Tipo de Oportunidade (ADMIN)",
            description = "Permite alterar diretamente a categoria/tipo de uma oportunidade pelo painel. Invalida o cache Redis automaticamente."
    )
    @ApiResponse(responseCode = "200", description = "Tipo da oportunidade atualizado com sucesso")
    @ApiResponse(responseCode = "404", description = "Oportunidade não encontrada")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/opportunities/{id}/type")
    public ResponseEntity<OpportunityResponse> updateOpportunityType(
            @Parameter(description = "Identificador da oportunidade") @PathVariable Long id,
            @Valid @RequestBody AdminOpportunityTypeUpdateRequest request
    ) {
        return ResponseEntity.ok(opportunityService.updateOpportunityType(id, request.type()));
    }

    @Operation(
            summary = "Soft Delete de Oportunidade (ADMIN)",
            description = "Marca a oportunidade como expirada definindo sua data de validade no passado, ocultando-a do feed público sem remover o registro do banco. Invalida o cache Redis correspondente."
    )
    @ApiResponse(responseCode = "204", description = "Oportunidade expirada com sucesso")
    @ApiResponse(responseCode = "404", description = "Oportunidade não encontrada")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/opportunities/{id}")
    public ResponseEntity<Void> softDeleteOpportunity(
            @Parameter(description = "Identificador da oportunidade a ser ocultada/expirada") @PathVariable Long id
    ) {
        opportunityService.softDeleteOpportunity(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Métricas do Painel Administrativo",
            description = "Retorna métricas consolidadas via consultas agregadas no PostgreSQL: funil de conversão da IA, distribuição por tipo/fonte e sugestões pendentes."
    )
    @ApiResponse(responseCode = "200", description = "Métricas do dashboard retornadas com sucesso")
    @GetMapping("/dashboard/metrics")
    public ResponseEntity<DashboardMetricsResponse> getDashboardMetrics() {
        return ResponseEntity.ok(adminDashboardService.getDashboardMetrics());
    }
}
