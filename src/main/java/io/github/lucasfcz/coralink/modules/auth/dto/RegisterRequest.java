package io.github.lucasfcz.coralink.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para cadastro de novo usuário na plataforma")
public record RegisterRequest(
        @Schema(description = "Nome completo do usuário", example = "Lucas Silva")
        @NotBlank(message = "O nome é obrigatório")
        String name,

        @Schema(description = "Endereço de e-mail do usuário", example = "lucas@exemplo.com")
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @Schema(description = "Senha de acesso (mínimo de 8 caracteres)", example = "senhaSegura123!")
        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, message = "A senha deve conter no mínimo 8 caracteres")
        String password
) {
}
