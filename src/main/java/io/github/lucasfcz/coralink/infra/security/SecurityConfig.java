package io.github.lucasfcz.coralink.infra.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configuração central da cadeia de segurança (SecurityFilterChain) do Spring Security.
 * <p>
 * PRINCÍPIOS ARQUITETURAIS:
 * 1. STATELESS: Como utilizamos JWT, nenhuma sessão de servidor (JSESSIONID) é mantida em memória.
 * 2. CSRF DESATIVADO: Para APIs REST stateless autenticadas via Bearer Token nos headers.
 * 3. AUTORIZAÇÃO POR CAMADAS (Defense in Depth):
 *    - Nível de URL: regras declarativas no {@link SecurityFilterChain}.
 *    - Nível de Método: anotações {@code @PreAuthorize("hasRole('ADMIN')")} nos controladores.
 * 4. TRATAMENTO UNIFICADO DE ERROS (401 / 403): Handlers inline definidos como beans nesta classe.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${coralink.cors.allowed-origins:}")
    private String allowedOrigins;

    @Value("${coralink.cors.frontend-url:}")
    private String frontendUrl;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());

            Map<String, Object> error = Map.of(
                    "timestamp", Instant.now().toString(),
                    "status", HttpServletResponse.SC_UNAUTHORIZED,
                    "error", "Unauthorized",
                    "message", authException.getMessage() != null ? authException.getMessage() : "Credenciais ausentes ou inválidas",
                    "path", request.getRequestURI()
            );

            response.getWriter().write(objectMapper.writeValueAsString(error));
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());

            Map<String, Object> error = Map.of(
                    "timestamp", Instant.now().toString(),
                    "status", HttpServletResponse.SC_FORBIDDEN,
                    "error", "Forbidden",
                    "message", "Acesso negado: privilégios insuficientes para acessar este recurso.",
                    "path", request.getRequestURI()
            );

            response.getWriter().write(objectMapper.writeValueAsString(error));
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        // 0. Tratamento interno de erros do Servlet/Spring MVC
                        .requestMatchers("/error").permitAll()

                        // 1. Endpoints de autenticação pública (Login Google, Registro Local, Login Local, Refresh, Logout)
                        .requestMatchers("/auth/**").permitAll()

                        // 2. Consulta pública a oportunidades (feed e buscas para estudantes sem autenticação)
                        .requestMatchers(HttpMethod.GET, "/opportunities", "/opportunities/**").permitAll()

                        // 3. Envio de sugestões/ajuda por estudantes (exige usuário autenticado)
                        .requestMatchers(HttpMethod.POST, "/suggestion/create").authenticated()

                        // 4. Documentação interativa Swagger/OpenAPI
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml"
                        ).permitAll()

                        // 5. Visualização de sugestões de estudantes (exclusivo para ADMIN)
                        .requestMatchers(HttpMethod.GET, "/suggestion/**").hasRole("ADMIN")

                        // 6. Todos os endpoints administrativos (/admin/**) exigem papel ROLE_ADMIN
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 7. Qualquer outra rota exige autenticação
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        Set<String> origins = new LinkedHashSet<>(List.of(
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "http://localhost:3001",
                "http://127.0.0.1:3001",
                "http://localhost:3002",
                "http://127.0.0.1:3002"
        ));

        if (allowedOrigins != null && !allowedOrigins.isBlank()) {
            Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(origins::add);
        }

        if (frontendUrl != null && !frontendUrl.isBlank()) {
            String cleanFrontend = frontendUrl.trim().replaceAll("/+$", "");
            if (!cleanFrontend.isEmpty()) {
                origins.add(cleanFrontend);
            }
        }

        configuration.setAllowedOriginPatterns(new ArrayList<>(origins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
