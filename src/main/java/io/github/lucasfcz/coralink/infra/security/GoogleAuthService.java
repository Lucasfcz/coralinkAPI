package io.github.lucasfcz.coralink.infra.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import io.github.lucasfcz.coralink.infra.exception.BadResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Serviço responsável pela validação criptográfica do Google ID Token.
 * <p>
 * Garante que a identidade do usuário seja comprovada diretamente contra
 * as chaves públicas do Google Identity Services (Google API), impedindo
 * falsificação de identidade ou tokens forjados por atacantes.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleAuthService {

    private final JwtProperties jwtProperties;
    private final org.springframework.core.env.Environment environment;

    public record GoogleUserInfo(
            String email,
            String name,
            String avatarUrl,
            String googleId
    ) {
    }

    public GoogleUserInfo verifyGoogleToken(String idTokenString) {
        verifyTokenIsValid(idTokenString);

        // SEC-01 FIX: Mock tokens são terminantemente proibidos fora dos perfis de 'dev' e 'test'
        if (idTokenString.startsWith("dev-mock-")) {
            if (!environment.matchesProfiles("dev", "test")) {
                log.error("Tentativa de uso de mock token em ambiente não autorizado!");
                throw new BadResponseException("Tokens de desenvolvimento não são permitidos em produção");
            }
            return handleDevMockToken(idTokenString);
        }

        // SEC-03 FIX: Em produção, o Google Client ID é obrigatório para validação estrita da audiência (aud)
        boolean hasClientId = jwtProperties.getGoogleClientId() != null && !jwtProperties.getGoogleClientId().isBlank();
        if (environment.matchesProfiles("prod") && !hasClientId) {
            log.error("Configuração insegura: google-client-id ausente no perfil de produção!");
            throw new BadResponseException("Configuração de autenticação incompleta no servidor");
        }

        try {
            var transport = new NetHttpTransport();
            var jsonFactory = GsonFactory.getDefaultInstance();

            GoogleIdTokenVerifier.Builder verifierBuilder = new GoogleIdTokenVerifier.Builder(transport, jsonFactory);

            if (hasClientId) {
                verifierBuilder.setAudience(Collections.singletonList(jwtProperties.getGoogleClientId()));
            }

            GoogleIdTokenVerifier verifier = verifierBuilder.build();
            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                log.warn("Falha na validação do Google ID Token: token inválido ou expirado");
                throw new BadResponseException("ID Token do Google inválido ou expirado");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            Boolean emailVerified = payload.getEmailVerified();

            // SEC-08 FIX: Exige explicitamente confirmação de e-mail verificado (Boolean.TRUE)
            if (email == null || !Boolean.TRUE.equals(emailVerified)) {
                throw new BadResponseException("O e-mail da conta Google informada não está verificado");
            }

            String name = (String) payload.get("name");
            if (name == null || name.isBlank()) {
                name = email.split("@")[0];
            }

            String pictureUrl = (String) payload.get("picture");
            String googleId = payload.getSubject();

            return new GoogleUserInfo(email.toLowerCase().trim(), name, pictureUrl, googleId);

        } catch (GeneralSecurityException | IOException e) {
            log.error("Erro criptográfico ao validar Google ID Token", e);
            throw new BadResponseException("Erro ao comunicar com os servidores do Google para validação do token");
        }
    }

    private GoogleUserInfo handleDevMockToken(String mockToken) {
        log.info("Processando token de mock em ambiente de desenvolvimento: {}", mockToken);
        String identifier = mockToken.replace("dev-mock-", "").trim();
        String email = identifier.contains("@") ? identifier : identifier + "@coralink.test";
        String name = identifier.split("@")[0];
        return new GoogleUserInfo(email.toLowerCase(), name, "https://avatar.example.com/" + name, "mock-google-id-" + name);
    }

    private void verifyTokenIsValid(String idTokenString) {
        if (idTokenString == null || idTokenString.isBlank()) {
            throw new BadResponseException("O token do Google não pode ser nulo ou vazio");
        }
    }
}
