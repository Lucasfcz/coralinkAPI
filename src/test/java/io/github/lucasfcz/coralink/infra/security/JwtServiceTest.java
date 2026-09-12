package io.github.lucasfcz.coralink.infra.security;

import io.github.lucasfcz.coralink.modules.auth.model.Role;
import io.github.lucasfcz.coralink.modules.auth.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256-compliance-12345");
        jwtProperties.setAccessTokenExpirationMs(60000); // 1 minuto
        jwtProperties.setRefreshTokenExpirationMs(3600000); // 1 hora
        jwtService = new JwtService(jwtProperties);
    }

    @Test
    @DisplayName("Should generate valid Access Token and extract claims correctly")
    void shouldGenerateValidAccessTokenAndExtractClaims() {
        User user = new User("lucas@example.com", "Lucas", "hash", Role.ROLE_ADMIN);
        user.setId(42L);

        String token = jwtService.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractEmail(token)).isEqualTo("lucas@example.com");
        assertThat(jwtService.extractRole(token)).isEqualTo("ROLE_ADMIN");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    @DisplayName("Should reject token when user email does not match")
    void shouldRejectTokenWhenUserEmailDoesNotMatch() {
        User user = new User("user1@example.com", "User One", "hash", Role.ROLE_USER);
        user.setId(1L);

        User differentUser = new User("user2@example.com", "User Two", "hash", Role.ROLE_USER);
        differentUser.setId(2L);

        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.isTokenValid(token, differentUser)).isFalse();
    }

    @Test
    @DisplayName("Should generate raw refresh token and compute consistent SHA-256 hash")
    void shouldGenerateRefreshTokenAndComputeHash() {
        String rawToken = jwtService.generateRawRefreshToken();
        assertThat(rawToken).isNotBlank();

        String hash1 = jwtService.hashToken(rawToken);
        String hash2 = jwtService.hashToken(rawToken);

        assertThat(hash1).isNotBlank();
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64); // SHA-256 hex string tem exatamente 64 caracteres
        assertThat(hash1).isNotEqualTo(rawToken); // O hash é diferente da string crua
    }
}
