package io.github.lucasfcz.coralink.modules.auth.model;

/**
 * Papéis de autorização (Roles) da Coralink API.
 * <p>
 * O prefixo "ROLE_" é a convenção obrigatória do Spring Security para integração
 * com anotações de segurança como {@code @PreAuthorize("hasRole('ADMIN')")} ou
 * {@code requestMatchers("/admin/**").hasRole("ADMIN")}.
 * Quando usamos {@code hasRole("ADMIN")}, o Spring Security automaticamente busca
 * por uma {@code GrantedAuthority} com o nome "ROLE_ADMIN".
 */
public enum Role {
    ROLE_USER,
    ROLE_ADMIN
}
