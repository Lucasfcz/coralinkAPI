package io.github.lucasfcz.coralink.modules.auth;

import io.github.lucasfcz.coralink.infra.security.JwtProperties;
import io.github.lucasfcz.coralink.modules.auth.dto.AuthResponse;
import io.github.lucasfcz.coralink.modules.auth.dto.RefreshTokenRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * Serviço dedicado à gestão de cookies de autenticação e adaptação de respostas HTTP.
 * <p>
 * Centraliza a criação, limpeza e extração de Refresh Tokens via cookies seguros HttpOnly,
 * além de sanitizar a resposta conforme o tipo de cliente (Web ou Mobile).
 */
@Service
@RequiredArgsConstructor
public class AuthCookieService {

    public static final String REFRESH_COOKIE_NAME = "coralink_refresh_token";

    private final JwtProperties jwtProperties;

    /**
     * Anexa o cookie HttpOnly contendo o Refresh Token na resposta HTTP.
     * O caminho "/" garante compatibilidade com chamadas diretas e proxies/rewrites do Next.js.
     */
    public void attachRefreshTokenCookie(HttpServletRequest request, HttpServletResponse response, String refreshToken) {
        boolean isHttps = isHttps(request);
        long maxAgeSeconds = jwtProperties.getRefreshTokenExpirationMs() / 1000;

        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(isHttps)
                .sameSite(isHttps ? "None" : "Lax")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Limpa o cookie de Refresh Token expirando-o imediatamente.
     */
    public void clearRefreshTokenCookie(HttpServletRequest request, HttpServletResponse response) {
        boolean isHttps = isHttps(request);

        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(isHttps)
                .sameSite(isHttps ? "None" : "Lax")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Resolve o Refresh Token a partir dos cookies da requisição ou do corpo da mensagem JSON.
     */
    public String resolveRefreshToken(HttpServletRequest request, RefreshTokenRequest bodyRequest) {
        if (request.getCookies() != null) {
            String cookieToken = Arrays.stream(request.getCookies())
                    .filter(c -> REFRESH_COOKIE_NAME.equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
            if (cookieToken != null && !cookieToken.isBlank()) {
                return cookieToken;
            }
        }

        if (bodyRequest != null && bodyRequest.refreshToken() != null && !bodyRequest.refreshToken().isBlank()) {
            return bodyRequest.refreshToken();
        }

        return null;
    }

    /**
     * Oculta o refresh token do corpo JSON para clientes Web (onde o cookie HttpOnly já é suficiente),
     * mas preserva no JSON caso seja um aplicativo móvel (Header 'X-Client-Type: mobile').
     */
    public AuthResponse sanitizeForClient(HttpServletRequest httpRequest, AuthResponse authResponse) {
        boolean isMobile = "mobile".equalsIgnoreCase(httpRequest.getHeader("X-Client-Type"));
        return isMobile
                ? authResponse
                : AuthResponse.of(authResponse.accessToken(), authResponse.expiresIn(), null, authResponse.user());
    }

    private boolean isHttps(HttpServletRequest request) {
        return request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }
}
