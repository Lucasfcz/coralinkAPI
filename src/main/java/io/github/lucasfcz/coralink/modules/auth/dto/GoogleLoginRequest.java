package io.github.lucasfcz.coralink.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload recebido pelo endpoint de login social {@code POST /auth/google}.
 * Contém o Google ID Token (formato JWT emitido pelo Google Identity Services).
 */
public record GoogleLoginRequest(
        @NotBlank(message = "O ID Token do Google é obrigatório")
        String idToken
) {
}
