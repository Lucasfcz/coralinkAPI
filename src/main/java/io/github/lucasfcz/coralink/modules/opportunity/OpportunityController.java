package io.github.lucasfcz.coralink.modules.opportunity;

import io.github.lucasfcz.coralink.modules.opportunity.dto.OpportunityResponse;
import io.github.lucasfcz.coralink.modules.opportunity.enums.Modality;
import io.github.lucasfcz.coralink.modules.opportunity.enums.OpportunityType;
import io.github.lucasfcz.coralink.modules.opportunity.enums.TargetCourseAudience;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * Controlador público de consulta a oportunidades acadêmicas e profissionais.
 * Todas as rotas deste controlador são de acesso livre (sem autenticação obrigatória).
 */
@RestController
@RequestMapping("/opportunities")
@RequiredArgsConstructor
@Tag(name = "Oportunidades", description = "Consulta pública ao feed e detalhes de oportunidades filtradas por IA")
public class OpportunityController {

    private final OpportunityService opportunityService;

    @Operation(
            summary = "Feed de Oportunidades com Filtros Avançados",
            description = "Retorna uma página de oportunidades vigentes com suporte a filtros combinados por título, tipo, público-alvo, modalidade e gratuidade."
    )
    @ApiResponse(responseCode = "200", description = "Página de oportunidades retornada com sucesso")
    @GetMapping
    public ResponseEntity<Page<OpportunityResponse>> getOpportunities(
            @Parameter(description = "Termo de busca no título da oportunidade") @RequestParam(required = false) String title,
            @Parameter(description = "Tipo/categoria da oportunidade (ex: HACKATHON, EVENT, INTERNSHIP)") @RequestParam(required = false) OpportunityType type,
            @Parameter(description = "Cursos ou públicos-alvo recomendados") @RequestParam(required = false) Set<TargetCourseAudience> targetCourseAudience,
            @Parameter(description = "Modalidade de realização (IN_PERSON, ONLINE, HYBRID)") @RequestParam(required = false) Modality modality,
            @Parameter(description = "Fonte ou instituição de origem (ex: UFPE, CESAR_SCHOOL, CIN_UFPE)") @RequestParam(required = false) String sourceName,
            @Parameter(description = "Filtro de gratuidade (true = gratuita, false = paga)") @RequestParam(required = false) Boolean isFree,
            @Parameter(description = "Se false, significa que apenas estudantes da faculdade podem participar") @RequestParam(required = false) Boolean isForAll,
            Pageable pageable) {
        return ResponseEntity.ok(opportunityService.getRelevantOpportunities(title, type, targetCourseAudience, modality, sourceName, isFree, isForAll, pageable));
    }

    @Operation(
            summary = "Busca de Oportunidades por Título",
            description = "Atalho para pesquisa textual no título das oportunidades."
    )
    @ApiResponse(responseCode = "200", description = "Resultados encontrados")
    @GetMapping("/search")
    public ResponseEntity<Page<OpportunityResponse>> findOpportunityByTitle(
            @Parameter(description = "Palavra-chave a buscar") @RequestParam String title,
            Pageable pageable) {
        return ResponseEntity.ok(opportunityService.getRelevantOpportunities(title, null, null, null, null, null, pageable));
    }

    @Operation(
            summary = "Quantidade Total de Oportunidades Vigentes",
            description = "Contagem total de oportunidades ativas cuja data limite ainda não expirou. Cache de 30 minutos no Redis."
    )
    @ApiResponse(responseCode = "200", description = "Quantidade calculada com sucesso")
    @GetMapping("/quantity")
    public ResponseEntity<Integer> quantityOfOpportunities() {
        return ResponseEntity.ok(opportunityService.howManyOpportunitiesAreUpcoming());
    }

    @Operation(
            summary = "Detalhes de uma Oportunidade",
            description = "Retorna todos os dados detalhados de uma oportunidade a partir do seu ID. Consulta otimizada com cache Redis de 2 horas."
    )
    @ApiResponse(responseCode = "200", description = "Oportunidade encontrada")
    @ApiResponse(responseCode = "404", description = "Oportunidade não encontrada")
    @GetMapping("/{id}")
    public ResponseEntity<OpportunityResponse> findOpportunityById(
            @Parameter(description = "Identificador único da oportunidade") @PathVariable Long id) {
        return ResponseEntity.ok(opportunityService.getOpportunityById(id));
    }
}
