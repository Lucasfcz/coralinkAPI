package io.github.lucasfcz.coralink.modules.usersuggestion;

import io.github.lucasfcz.coralink.infra.config.OpenApiConfig;
import io.github.lucasfcz.coralink.modules.usersuggestion.dto.UserSuggestionRequest;
import io.github.lucasfcz.coralink.modules.usersuggestion.dto.UserSuggestionResponse;
import io.github.lucasfcz.coralink.modules.usersuggestion.model.SuggestionType;
import io.github.lucasfcz.coralink.modules.auth.AuthService;
import io.github.lucasfcz.coralink.modules.auth.model.User;
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
@Tag(name = "Sugestões e Ajuda", description = "Envio de feedback/sugestões por estudantes autenticados e consulta por administradores")
public class UserSuggestionController {

    private final UserSuggestionService userSuggestionService;
    private final AuthService authService;

    @Operation(
            summary = "Envio de Sugestão ou Pedido de Ajuda",
            description = "Endpoint restrito a estudantes autenticados para envio de sugestões de melhoria, novas fontes ou reportar problemas."
    )
    @ApiResponse(responseCode = "201", description = "Sugestão registrada com sucesso")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    @PostMapping("/create")
    public ResponseEntity<UserSuggestionResponse> createUserSuggestion(@Valid @RequestBody UserSuggestionRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(userSuggestionService.createUserSuggestion(request, currentUser));
    }

    @Operation(
            summary = "Listar Todas as Sugestões (ADMIN)",
            description = "Consulta paginada de todas as sugestões enviadas por estudantes. Restrito a administradores."
    )
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<UserSuggestionResponse>> getAllUserSuggestions(Pageable pageable) {
        return ResponseEntity.ok(userSuggestionService.findAllUserSuggestions(pageable));
    }

    @Operation(
            summary = "Filtrar Sugestões por Tipo (ADMIN)",
            description = "Consulta paginada de sugestões filtradas por categoria (NEW_SOURCE, FEATURE, CORRECTION, BUG, OTHER). Restrito a administradores."
    )
    @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/type")
    public ResponseEntity<Page<UserSuggestionResponse>> getUserSuggestionsByType(
            @Parameter(description = "Tipo da sugestão") @RequestParam SuggestionType type,
            Pageable pageable) {
        return ResponseEntity.ok(userSuggestionService.getUserSuggestionsByType(type, pageable));
    }
}
