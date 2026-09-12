package io.github.lucasfcz.coralink.modules.userhelp;

import io.github.lucasfcz.coralink.infra.config.OpenApiConfig;
import io.github.lucasfcz.coralink.modules.userhelp.dto.UserHelpRequest;
import io.github.lucasfcz.coralink.modules.userhelp.dto.UserHelpResponse;
import io.github.lucasfcz.coralink.modules.userhelp.model.SuggestionType;
import io.github.lucasfcz.coralink.modules.userhelp.UserHelpService;
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

/**
 * Controlador de envio de sugestões pelos estudantes e visualização pela administração.
 */
@RestController
@RequestMapping("/suggestion")
@RequiredArgsConstructor
@Tag(name = "Sugestões e Ajuda", description = "Envio público de feedback/sugestões por estudantes e consulta por administradores")
public class UserHelpController {

    private final UserHelpService userHelpService;

    @Operation(
            summary = "Envio de Sugestão ou Pedido de Ajuda",
            description = "Endpoint público para qualquer estudante enviar sugestões de melhoria, novas fontes ou reportar problemas."
    )
    @ApiResponse(responseCode = "201", description = "Sugestão registrada com sucesso")
    @PostMapping("/create")
    public ResponseEntity<UserHelpResponse> createUserHelp(@Valid @RequestBody UserHelpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userHelpService.createUserHelp(request));
    }

    @Operation(
            summary = "Listar Todas as Sugestões (ADMIN)",
            description = "Consulta paginada de todas as sugestões enviadas por estudantes. Restrito a administradores."
    )
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<UserHelpResponse>> getAllUserHelp(Pageable pageable) {
        return ResponseEntity.ok(userHelpService.findAllUserHelp(pageable));
    }

    @Operation(
            summary = "Listar Sugestões por Tipo (ADMIN)",
            description = "Consulta paginada de sugestões filtradas por categoria (ex: BUG, NEW_SOURCE, SUGGESTION). Restrito a administradores."
    )
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{type}")
    public ResponseEntity<Page<UserHelpResponse>> getAllUserHelpByType(
            @Parameter(description = "Categoria da sugestão") @PathVariable SuggestionType type,
            Pageable pageable
    ) {
        return ResponseEntity.ok(userHelpService.getUserHelpsByType(type, pageable));
    }
}
