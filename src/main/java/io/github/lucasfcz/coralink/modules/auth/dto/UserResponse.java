package io.github.lucasfcz.coralink.modules.auth.dto;

import io.github.lucasfcz.coralink.modules.auth.model.Role;

/**
 * Representação dos dados públicos do usuário retornados após autenticação.
 */
public record UserResponse(
        Long id,
        String email,
        String name,
        String avatarUrl,
        Role role
) {
}
