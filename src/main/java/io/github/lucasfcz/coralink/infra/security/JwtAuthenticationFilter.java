package io.github.lucasfcz.coralink.infra.security;

import io.github.lucasfcz.coralink.modules.auth.model.User;
import io.github.lucasfcz.coralink.modules.auth.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de segurança executado uma única vez por requisição HTTP ({@link OncePerRequestFilter}).
 * <p>
 * FLUXO DE FUNCIONAMENTO:
 * 1. Intercepta a requisição e busca o cabeçalho 'Authorization'.
 * 2. Verifica se o valor inicia com o prefixo 'Bearer '.
 * 3. Extrai e valida o token JWT usando {@link JwtService}.
 * 4. Carrega os dados do {@link User} do banco de dados para confirmar que a conta está ativa.
 * 5. Cria um {@link UsernamePasswordAuthenticationToken} com as authorities do usuário
 *    (ex: ROLE_ADMIN, ROLE_USER) e o injeta no {@link SecurityContextHolder}.
 * 6. Se o token não for fornecido, a requisição continua como anônima, permitindo o acesso a rotas públicas.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Se não houver cabeçalho de autorização ou se não for um Bearer token, prossegue para o próximo filtro
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7).trim();

        try {
            final String userEmail = jwtService.extractEmail(jwt);

            // Se o e-mail foi extraído e ainda não há autenticação registrada nesta requisição
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = userRepository.findByEmail(userEmail).orElse(null);

                if (user != null && user.isActive() && jwtService.isTokenValid(jwt, user)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            user.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Registra o usuário autenticado no contexto do Spring Security
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Usuário '{}' autenticado com sucesso via JWT com papéis: {}", userEmail, user.getAuthorities());
                }
            }
        } catch (Exception e) {
            log.debug("Falha ao autenticar token JWT na requisição: {}", e.getMessage());
            // Não interrompemos a cadeia aqui; o SecurityFilterChain ou o AuthenticationEntryPoint
            // se encarregarão de barrar o acesso caso o endpoint exija autorização.
        }

        filterChain.doFilter(request, response);
    }
}
