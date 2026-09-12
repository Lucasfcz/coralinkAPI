package io.github.lucasfcz.coralink.modules.auth.dto;

/**
 * Payload opcional para renovação de sessão quando o cliente não usa cookies.
 */
public record RefreshTokenRequest(
        String refreshToken
) {
}
