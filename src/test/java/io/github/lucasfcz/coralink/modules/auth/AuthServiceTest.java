package io.github.lucasfcz.coralink.modules.auth;

import io.github.lucasfcz.coralink.modules.auth.dto.AuthResponse;
import io.github.lucasfcz.coralink.modules.auth.dto.LoginRequest;
import io.github.lucasfcz.coralink.modules.auth.dto.RegisterRequest;
import io.github.lucasfcz.coralink.modules.auth.model.Role;
import io.github.lucasfcz.coralink.infra.exception.BadResponseException;
import io.github.lucasfcz.coralink.modules.auth.model.RefreshToken;
import io.github.lucasfcz.coralink.modules.auth.model.User;
import io.github.lucasfcz.coralink.modules.auth.repository.RefreshTokenRepository;
import io.github.lucasfcz.coralink.modules.auth.repository.UserRepository;
import io.github.lucasfcz.coralink.infra.security.GoogleAuthService;
import io.github.lucasfcz.coralink.infra.security.JwtProperties;
import io.github.lucasfcz.coralink.infra.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private GoogleAuthService googleAuthService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Spy
    private JwtProperties jwtProperties = new JwtProperties();

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtProperties.setSecret("test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256-compliance-12345");
        jwtProperties.setAccessTokenExpirationMs(900000);
        jwtProperties.setRefreshTokenExpirationMs(604800000);

        jwtService = new JwtService(jwtProperties);
        authService = new AuthService(
                googleAuthService,
                userRepository,
                refreshTokenRepository,
                jwtService,
                jwtProperties,
                passwordEncoder
        );
    }

    @Test
    @DisplayName("Register - Should register a new local user successfully")
    void shouldRegisterNewLocalUserSuccessfully() {
        RegisterRequest request = new RegisterRequest("Lucas Silva", "lucas@email.com", "senhaForte123!");
        when(userRepository.findByEmail("lucas@email.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.user().email()).isEqualTo("lucas@email.com");
        assertThat(response.user().name()).isEqualTo("Lucas Silva");
        assertThat(response.user().role()).isEqualTo(Role.ROLE_USER);

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Register - Should throw BadResponseException if email already registered")
    void shouldThrowExceptionWhenRegisteringExistingEmail() {
        RegisterRequest request = new RegisterRequest("Lucas Silva", "lucas@email.com", "senhaForte123!");
        User existingUser = new User("lucas@email.com", "Lucas", "hash", Role.ROLE_USER);
        when(userRepository.findByEmail("lucas@email.com")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadResponseException.class)
                .hasMessageContaining("já está cadastrado");
    }

    @Test
    @DisplayName("Login - Should login local user with correct password")
    void shouldLoginLocalUserSuccessfully() {
        String hash = passwordEncoder.encode("senhaCorreta123!");
        User user = new User("lucas@email.com", "Lucas", hash, Role.ROLE_USER);
        user.setId(15L);

        when(userRepository.findByEmail("lucas@email.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest("lucas@email.com", "senhaCorreta123!");
        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.user().email()).isEqualTo("lucas@email.com");
    }

    @Test
    @DisplayName("Login - Should throw BadResponseException with incorrect password")
    void shouldThrowExceptionOnIncorrectPassword() {
        String hash = passwordEncoder.encode("senhaCorreta123!");
        User user = new User("lucas@email.com", "Lucas", hash, Role.ROLE_USER);
        user.setId(15L);

        when(userRepository.findByEmail("lucas@email.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest("lucas@email.com", "senhaIncorreta!");
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadResponseException.class)
                .hasMessageContaining("Credenciais inválidas");
    }

    @Test
    @DisplayName("Login with Google - Should create normal user with ROLE_USER")
    void shouldCreateNormalUserWithRoleUser() {
        when(googleAuthService.verifyGoogleToken("google-token-123"))
                .thenReturn(new GoogleAuthService.GoogleUserInfo(
                        "estudante@ufpe.br",
                        "Estudante UFPE",
                        "https://avatar.com/estudante",
                        "google-sub-1"
                ));

        when(userRepository.findByEmail("estudante@ufpe.br")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });

        AuthResponse response = authService.loginWithGoogle("google-token-123");

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.user().email()).isEqualTo("estudante@ufpe.br");
        assertThat(response.user().role()).isEqualTo(Role.ROLE_USER);

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Login with Google - Existing user with ROLE_ADMIN preserves their role")
    void shouldPreserveRoleAdminForExistingAdminUser() {
        User adminUser = new User("admin@coralink.com", "Admin Coralink", null, "google-sub-admin", Role.ROLE_ADMIN);
        adminUser.setId(1L);

        when(googleAuthService.verifyGoogleToken("google-token-admin"))
                .thenReturn(new GoogleAuthService.GoogleUserInfo(
                        "admin@coralink.com",
                        "Admin Coralink",
                        "https://avatar.com/admin",
                        "google-sub-admin"
                ));

        when(userRepository.findByEmail("admin@coralink.com")).thenReturn(Optional.of(adminUser));

        AuthResponse response = authService.loginWithGoogle("google-token-admin");

        assertThat(response.user().role()).isEqualTo(Role.ROLE_ADMIN);
    }

    @Test
    @DisplayName("Refresh Token - Should rotate token (revoke old and issue new)")
    void shouldRotateRefreshTokenSuccessfully() {
        String rawToken = "raw-refresh-token-xyz";
        String tokenHash = jwtService.hashToken(rawToken);

        User user = new User("user@coralink.com", "User", null, "google-id-1", Role.ROLE_USER);
        user.setId(5L);

        RefreshToken storedToken = new RefreshToken(user, tokenHash, Instant.now().plusSeconds(3600));
        storedToken.setId(100L);

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(storedToken));

        AuthResponse response = authService.refreshToken(rawToken);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotEqualTo(rawToken);
        assertThat(storedToken.isRevoked()).isTrue();

        verify(refreshTokenRepository, atLeastOnce()).save(storedToken);
        verify(refreshTokenRepository, atLeastOnce()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Refresh Token - Security Alert: Should revoke all sessions if a revoked token is reused")
    void shouldRevokeAllSessionsOnTokenReuseAttack() {
        String rawToken = "stolen-already-revoked-token";
        String tokenHash = jwtService.hashToken(rawToken);

        User user = new User("victim@coralink.com", "Victim User", null, "google-id-victim", Role.ROLE_USER);
        user.setId(5L);

        RefreshToken alreadyRevokedToken = new RefreshToken(user, tokenHash, Instant.now().plusSeconds(3600));
        alreadyRevokedToken.setId(101L);
        alreadyRevokedToken.setRevoked(true); // Já estava revogado!

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(alreadyRevokedToken));

        assertThatThrownBy(() -> authService.refreshToken(rawToken))
                .isInstanceOf(BadResponseException.class)
                .hasMessageContaining("Violação de segurança detectada");

        verify(refreshTokenRepository).revokeAllByUser(eq(user));
    }
}
