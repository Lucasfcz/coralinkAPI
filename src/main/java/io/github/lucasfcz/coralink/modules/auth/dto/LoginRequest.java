package io.github.lucasfcz.coralink.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados para autenticação local por e-mail e senha")
public record LoginRequest(
        @Schema(description = "Endereço de e-mail cadastrado", example = "lucas@exemplo.com")
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @Schema(description = "Senha cadastrada", example = "senhaSegura123!")
        @NotBlank(message = "A senha é obrigatória")
        String password
) {
}
