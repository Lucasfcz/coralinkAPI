package io.github.lucasfcz.coralink.modules.auth;

import io.github.lucasfcz.coralink.modules.auth.dto.AuthResponse;
import io.github.lucasfcz.coralink.modules.auth.dto.LoginRequest;
import io.github.lucasfcz.coralink.modules.auth.dto.RegisterRequest;
import io.github.lucasfcz.coralink.modules.auth.dto.UserResponse;
import io.github.lucasfcz.coralink.modules.auth.model.Role;
import io.github.lucasfcz.coralink.infra.exception.BadResponseException;
import io.github.lucasfcz.coralink.modules.auth.model.RefreshToken;
import io.github.lucasfcz.coralink.modules.auth.model.User;
import io.github.lucasfcz.coralink.modules.auth.repository.RefreshTokenRepository;
import io.github.lucasfcz.coralink.modules.auth.repository.UserRepository;
import io.github.lucasfcz.coralink.infra.security.GoogleAuthService;
import io.github.lucasfcz.coralink.infra.security.JwtProperties;
import io.github.lucasfcz.coralink.infra.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Serviço responsável por orquestrar o fluxo de autenticação, emissão e rotação de tokens.
 * <p>
 * Suporta autenticação social (Google OAuth2) e autenticação local (E-mail e Senha com BCrypt).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final GoogleAuthService googleAuthService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;

    /**
     * Cadastro local de novo usuário com e-mail e senha.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new BadResponseException("Este endereço de e-mail já está cadastrado");
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        Role role = Role.ROLE_USER;

        User newUser = new User(
                request.email().trim().toLowerCase(),
                request.name().trim(),
                encodedPassword,
                role
        );

        User savedUser = userRepository.save(newUser);
        log.info("Usuário '{}' cadastrado com sucesso com papel '{}'", savedUser.getEmail(), savedUser.getRole());

        return issueTokensForUser(savedUser);
    }

    /**
     * Login local por e-mail e senha.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new BadResponseException("Credenciais inválidas"));

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new BadResponseException("Esta conta foi criada com o Google. Por favor, efetue login usando o Google.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadResponseException("Credenciais inválidas");
        }

        validateUserActive(user);

        return issueTokensForUser(user);
    }

    /**
     * Login social com Google Identity Services.
     */
    @Transactional
    public AuthResponse loginWithGoogle(String idToken) {
        GoogleAuthService.GoogleUserInfo googleUser = googleAuthService.verifyGoogleToken(idToken);

        User user = userRepository.findByEmail(googleUser.email())
                .orElseGet(() -> createNewUserFromGoogle(googleUser));

        updateUserProfileIfChanged(user, googleUser);
        validateUserActive(user);

        return issueTokensForUser(user);
    }

    /**
     * Renova o Access Token com Refresh Token Rotation (RTR).
     */
    @Transactional
    public AuthResponse refreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BadResponseException("Refresh token não informado");
        }

        String tokenHash = jwtService.hashToken(rawRefreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    log.warn("Tentativa de renovação com refresh token inexistente no banco");
                    return new BadResponseException("Refresh token inválido");
                });

        // Detecção de Reuso de Token (Ataque de Repetição)
        if (storedToken.isRevoked()) {
            log.error("ALERTA DE SEGURANÇA: Reutilização de token revogado detectada para '{}'! Revogando todas as sessões.",
                    storedToken.getUser().getEmail());
            refreshTokenRepository.revokeAllByUser(storedToken.getUser());
            throw new BadResponseException("Violação de segurança detectada: sessão encerrada por precaução");
        }

        if (Instant.now().isAfter(storedToken.getExpiresAt())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new BadResponseException("Refresh token expirado. Por favor, efetue login novamente.");
        }

        User user = storedToken.getUser();
        validateUserActive(user);

        // 1. Revoga o token atual
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        // 2. Emite novo par (Rotação)
        return issueTokensForUser(user);
    }

    /**
     * Invalida a sessão revogando o Refresh Token apresentado.
     */
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = jwtService.hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
                log.debug("Refresh token do usuário '{}' revogado no logout", token.getUser().getEmail());
            });
        }
    }

    /**
     * Expurgo periódico de tokens revogados ou expirados.
     */
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        log.info("Iniciando limpeza periódica de refresh tokens expirados ou revogados...");
        refreshTokenRepository.deleteExpiredOrRevokedTokens(Instant.now());
    }

    /**
     * Retorna o usuário atualmente autenticado a partir do SecurityContextHolder.
     */
    @Transactional(readOnly = true)
    public User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            throw new BadResponseException("Nenhum usuário autenticado no contexto atual");
        }
        return userRepository.findById(user.getId())
                .orElseThrow(() -> new BadResponseException("Usuário não encontrado"));
    }

    private AuthResponse issueTokensForUser(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = jwtService.generateRawRefreshToken();
        String refreshTokenHash = jwtService.hashToken(rawRefreshToken);

        Instant expiresAt = Instant.now().plusMillis(jwtProperties.getRefreshTokenExpirationMs());

        RefreshToken refreshToken = new RefreshToken(user, refreshTokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);

        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getAvatarUrl(),
                user.getRole()
        );

        return AuthResponse.of(
                accessToken,
                jwtProperties.getAccessTokenExpirationMs() / 1000,
                rawRefreshToken,
                userResponse
        );
    }

    private User createNewUserFromGoogle(GoogleAuthService.GoogleUserInfo googleUser) {
        log.info("Cadastrando novo usuário Google '{}' com papel '{}'", googleUser.email(), Role.ROLE_USER);

        User newUser = new User(
                googleUser.email(),
                googleUser.name(),
                googleUser.avatarUrl(),
                googleUser.googleId(),
                Role.ROLE_USER
        );

        return userRepository.save(newUser);
    }

    private void updateUserProfileIfChanged(User user, GoogleAuthService.GoogleUserInfo googleUser) {
        boolean changed = false;
        if (googleUser.avatarUrl() != null && !googleUser.avatarUrl().equals(user.getAvatarUrl())) {
            user.setAvatarUrl(googleUser.avatarUrl());
            changed = true;
        }
        if (googleUser.name() != null && !googleUser.name().isBlank() && !googleUser.name().equals(user.getName())) {
            user.setName(googleUser.name());
            changed = true;
        }
        if (user.getGoogleId() == null && googleUser.googleId() != null) {
            user.setGoogleId(googleUser.googleId());
            changed = true;
        }
        if (changed) {
            userRepository.save(user);
        }
    }

    private void validateUserActive(User user) {
        if (!user.isActive()) {
            throw new BadResponseException("Esta conta de usuário foi desativada");
        }
    }
}
