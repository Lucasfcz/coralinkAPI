package io.github.lucasfcz.coralink.infra.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;



/**
 * Propriedades de configuração de segurança e JWT mapeadas do prefixo {@code coralink.security}.
 */
@Component
@ConfigurationProperties(prefix = "coralink.security")
@Getter
@Setter
public class JwtProperties {

    /**
     * Chave secreta usada para assinar digitalmente os Access Tokens via algoritmo HMAC-SHA256.
     * Deve ser fornecida via variável de ambiente JWT_SECRET e possuir no mínimo 256 bits (32 caracteres).
     */
    private String secret;

    /**
     * Tempo de vida do Access Token em milissegundos.
     * Padrão: 15 minutos (900.000 ms) para minimizar a janela de exposição de tokens interceptados.
     */
    private long accessTokenExpirationMs = 15 * 60 * 1000L;

    /**
     * Tempo de vida do Refresh Token em milissegundos.
     * Padrão: 7 dias (604.800.000 ms).
     */
    private long refreshTokenExpirationMs = 7 * 24 * 60 * 60 * 1000L;

    /**
     * Google OAuth2 Client ID registrado no Google Cloud Console.
     */
    private String googleClientId = "";


    @jakarta.annotation.PostConstruct
    public void validateConfiguration() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("O segredo JWT (coralink.security.secret) deve possuir no mínimo 256 bits (32 caracteres)");
        }
    }
}
