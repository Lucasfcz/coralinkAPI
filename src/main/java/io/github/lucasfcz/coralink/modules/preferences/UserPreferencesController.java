package io.github.lucasfcz.coralink.modules.preferences;

import io.github.lucasfcz.coralink.infra.config.OpenApiConfig;
import io.github.lucasfcz.coralink.modules.auth.AuthService;
import io.github.lucasfcz.coralink.modules.auth.model.User;
import io.github.lucasfcz.coralink.modules.preferences.dto.UserPreferencesPatchRequest;
import io.github.lucasfcz.coralink.modules.preferences.dto.UserPreferencesRequest;
import io.github.lucasfcz.coralink.modules.preferences.dto.UserPreferencesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/preferences")
@RequiredArgsConstructor
@Tag(name = "Preferências do Usuário", description = "Endpoints para gerenciamento das preferências de recomendação e filtragem de oportunidades")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class UserPreferencesController {

    private final UserPreferencesService preferencesService;
    private final AuthService authService;

    @Operation(
            summary = "Obter Preferências do Usuário Autenticado",
            description = "Retorna as preferências configuradas pelo usuário logado (cursos de interesse, instituições e tipos)."
    )
    @ApiResponse(responseCode = "200", description = "Preferências recuperadas com sucesso")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @ApiResponse(responseCode = "404", description = "Preferências ainda não cadastradas para o usuário")
    @GetMapping
    public ResponseEntity<UserPreferencesResponse> getPreferences() {
        User currentUser = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(preferencesService.getPreferencesByUserId(currentUser.getId()));
    }

    @Operation(
            summary = "Salvar ou Substituir Preferências",
            description = "Salva ou substitui integralmente as preferências do usuário autenticado."
    )
    @ApiResponse(responseCode = "200", description = "Preferências salvas com sucesso")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @PostMapping
    public ResponseEntity<UserPreferencesResponse> savePreferences(@Valid @RequestBody UserPreferencesRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(preferencesService.savePreferences(currentUser.getId(), request));
    }

    @Operation(
            summary = "Atualizar Parcialmente as Preferências (HTTP PATCH)",
            description = "Atualiza parcialmente os campos especificados nas preferências do usuário autenticado."
    )
    @ApiResponse(responseCode = "200", description = "Preferências atualizadas com sucesso")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @PatchMapping
    public ResponseEntity<UserPreferencesResponse> updatePreferences(@Valid @RequestBody UserPreferencesPatchRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(preferencesService.updatePreferences(currentUser.getId(), request));
    }
}
