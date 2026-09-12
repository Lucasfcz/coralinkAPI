package io.github.lucasfcz.coralink.infra.security;

import io.github.lucasfcz.coralink.infra.exception.BadResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleAuthServiceTest {

    private JwtProperties jwtProperties;
    private MockEnvironment environment;
    private GoogleAuthService googleAuthService;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        environment = new MockEnvironment();
        googleAuthService = new GoogleAuthService(jwtProperties, environment);
    }

    @Test
    @DisplayName("SEC-01 Defense - dev-mock tokens MUST be rejected in production profile")
    void shouldRejectDevMockTokensInProductionProfile() {
        environment.setActiveProfiles("prod");

        assertThatThrownBy(() -> googleAuthService.verifyGoogleToken("dev-mock-admin@coralink.com"))
                .isInstanceOf(BadResponseException.class)
                .hasMessageContaining("Tokens de desenvolvimento não são permitidos em produção");
    }

    @Test
    @DisplayName("SEC-01 Success - dev-mock tokens are accepted in dev/test profile")
    void shouldAcceptDevMockTokensInDevProfile() {
        environment.setActiveProfiles("dev");

        GoogleAuthService.GoogleUserInfo user = googleAuthService.verifyGoogleToken("dev-mock-lucas@ufpe.br");

        assertThat(user).isNotNull();
        assertThat(user.email()).isEqualTo("lucas@ufpe.br");
        assertThat(user.name()).isEqualTo("lucas");
    }

    @Test
    @DisplayName("SEC-03 Defense - Production profile requires google-client-id configured")
    void shouldRejectWhenClientIdMissingInProductionProfile() {
        environment.setActiveProfiles("prod");
        jwtProperties.setGoogleClientId("");

        assertThatThrownBy(() -> googleAuthService.verifyGoogleToken("real-google-token"))
                .isInstanceOf(BadResponseException.class)
                .hasMessageContaining("Configuração de autenticação incompleta no servidor");
    }
}
