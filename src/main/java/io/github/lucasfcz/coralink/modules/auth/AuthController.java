package io.github.lucasfcz.coralink.modules.auth;

import io.github.lucasfcz.coralink.infra.config.OpenApiConfig;
import io.github.lucasfcz.coralink.modules.auth.dto.*;
import io.github.lucasfcz.coralink.modules.auth.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador de Autenticação pública e gestão de sessão.
 * <p>
 * Suporta:
 * 1. Cadastro local por e-mail e senha com BCrypt.
 * 2. Login local por e-mail e senha.
 * 3. Login social com Google OAuth2 (troca do ID Token por tokens próprios).
 * 4. Renovação de tokens via Refresh Token Rotation (RTR).
 * 5. Logout seguro com invalidação de cookies HttpOnly.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints para registro local, login clássico, login social Google e gestão de tokens")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;

    @Operation(
            summary = "Cadastro Local de Usuário",
            description = "Cria uma nova conta de usuário com e-mail, nome e senha protegida por hash BCrypt, emitindo os tokens de acesso imediatamente."
    )
    @ApiResponse(responseCode = "201", description = "Usuário cadastrado com sucesso")
    @ApiResponse(responseCode = "400", description = "E-mail já existente ou parâmetros inválidos")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.register(request);
        authCookieService.attachRefreshTokenCookie(httpRequest, response, authResponse.refreshToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(authCookieService.sanitizeForClient(httpRequest, authResponse));
    }

    @Operation(
            summary = "Login Local por E-mail e Senha",
            description = "Autentica um usuário existente por e-mail e senha, retornando o Access Token no JSON e o Refresh Token em cookie seguro HttpOnly."
    )
    @ApiResponse(responseCode = "200", description = "Login realizado com sucesso")
    @ApiResponse(responseCode = "400", description = "Credenciais inválidas ou usuário inativo")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.login(request);
        authCookieService.attachRefreshTokenCookie(httpRequest, response, authResponse.refreshToken());
        return ResponseEntity.ok(authCookieService.sanitizeForClient(httpRequest, authResponse));
    }

    @Operation(
            summary = "Login Social com Google OAuth2",
            description = "Valida o ID Token JWT emitido pelo Google e converte para tokens próprios do ecossistema Coralink."
    )
    @ApiResponse(responseCode = "200", description = "Login social efetuado com sucesso")
    @ApiResponse(responseCode = "400", description = "Token Google inválido ou expirado")
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.loginWithGoogle(request.idToken());
        authCookieService.attachRefreshTokenCookie(httpRequest, response, authResponse.refreshToken());
        return ResponseEntity.ok(authCookieService.sanitizeForClient(httpRequest, authResponse));
    }

    @Operation(
            summary = "Renovação de Token de Acesso (RTR)",
            description = "Emite um novo Access Token e rotaciona o Refresh Token. Detecta tentativas de reutilização maliciosa."
    )
    @ApiResponse(responseCode = "200", description = "Tokens renovados com sucesso")
    @ApiResponse(responseCode = "400", description = "Refresh token expirado, inválido ou revogado")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody(required = false) RefreshTokenRequest bodyRequest,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        String token = authCookieService.resolveRefreshToken(httpRequest, bodyRequest);
        AuthResponse authResponse = authService.refreshToken(token);
        authCookieService.attachRefreshTokenCookie(httpRequest, response, authResponse.refreshToken());
        return ResponseEntity.ok(authCookieService.sanitizeForClient(httpRequest, authResponse));
    }

    @Operation(
            summary = "Logout da Sessão",
            description = "Revoga o Refresh Token no banco de dados e limpa os cookies HttpOnly de autenticação do cliente."
    )
    @ApiResponse(responseCode = "204", description = "Sessão encerrada com sucesso")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) RefreshTokenRequest bodyRequest,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        String token = authCookieService.resolveRefreshToken(httpRequest, bodyRequest);
        authService.logout(token);
        authCookieService.clearRefreshTokenCookie(httpRequest, response);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Perfil do Usuário Autenticado",
            description = "Retorna os dados do usuário atual a partir do contexto do token Bearer."
    )
    @ApiResponse(responseCode = "200", description = "Perfil retornado com sucesso")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        User user = authService.getCurrentAuthenticatedUser();
        UserResponse response = new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getAvatarUrl(),
                user.getRole()
        );
        return ResponseEntity.ok(response);
    }
}
