package io.github.lucasfcz.coralink.modules.auth.dto;

/**
 * Resposta retornada após autenticação bem-sucedida ou renovação de token.
 * <p>
 * O {@code accessToken} é enviado no corpo da resposta para uso imediato pelo cliente.
 * O {@code refreshToken} é enviado simultaneamente em um cookie HTTP-Only (para navegadores)
 * e também incluído opcionalmente neste DTO para dar suporte a clientes móveis (Flutter/React Native).
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        Long expiresIn,
        String refreshToken,
        UserResponse user
) {
    public static AuthResponse of(String accessToken, Long expiresIn, String refreshToken, UserResponse user) {
        return new AuthResponse(accessToken, "Bearer", expiresIn, refreshToken, user);
    }
}
