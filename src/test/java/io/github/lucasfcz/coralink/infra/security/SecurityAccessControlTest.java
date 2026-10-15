package io.github.lucasfcz.coralink.infra.security;

import io.github.lucasfcz.coralink.modules.admin.AdminController;
import io.github.lucasfcz.coralink.modules.auth.AuthController;
import io.github.lucasfcz.coralink.modules.auth.AuthCookieService;
import io.github.lucasfcz.coralink.modules.opportunity.OpportunityController;
import io.github.lucasfcz.coralink.modules.usersuggestion.UserSuggestionController;
import io.github.lucasfcz.coralink.modules.opportunity.dto.OpportunityResponse;
import io.github.lucasfcz.coralink.modules.pipeline.dto.PipelineStatusResponse;
import io.github.lucasfcz.coralink.modules.auth.repository.UserRepository;
import io.github.lucasfcz.coralink.modules.auth.AuthService;
import io.github.lucasfcz.coralink.modules.opportunity.OpportunityService;
import io.github.lucasfcz.coralink.modules.admin.AdminDashboardService;
import io.github.lucasfcz.coralink.modules.pipeline.PipelineService;
import io.github.lucasfcz.coralink.modules.usersuggestion.UserSuggestionService;
import io.github.lucasfcz.coralink.infra.ratelimit.RateLimiterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.lucasfcz.coralink.modules.preferences.UserPreferencesController;
import io.github.lucasfcz.coralink.modules.preferences.UserPreferencesService;
import io.github.lucasfcz.coralink.modules.preferences.dto.UserPreferencesResponse;

@WebMvcTest(controllers = {AdminController.class, OpportunityController.class, UserSuggestionController.class, AuthController.class, UserPreferencesController.class})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        JwtProperties.class
})
class SecurityAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PipelineService pipelineService;

    @MockitoBean
    private OpportunityService opportunityService;

    @MockitoBean
    private AdminDashboardService adminDashboardService;

    @MockitoBean
    private UserSuggestionService userSuggestionService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AuthCookieService authCookieService;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    @MockitoBean
    private org.springframework.cache.CacheManager cacheManager;

    @MockitoBean
    private UserPreferencesService userPreferencesService;

    @org.junit.jupiter.api.BeforeEach
    void setUpRateLimiter() {
        io.github.bucket4j.Bucket mockBucket = org.mockito.Mockito.mock(io.github.bucket4j.Bucket.class);
        org.mockito.Mockito.when(mockBucket.tryConsume(1)).thenReturn(true);
        org.mockito.Mockito.when(rateLimiterService.resolveBucket(any())).thenReturn(mockBucket);
    }

    @Test
    @DisplayName("Public Access - GET /opportunities should be accessible anonymously (200 OK)")
    void publicEndpointsShouldBeAccessibleWithoutAuth() throws Exception {
        Page<OpportunityResponse> emptyPage = new PageImpl<>(List.of());
        when(opportunityService.getRelevantOpportunities(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/opportunities"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security Defense - Anonymous access to /admin/pipeline/status must return 401 Unauthorized")
    void adminStatusShouldBeRejectedWith401ForAnonymous() throws Exception {
        mockMvc.perform(get("/admin/pipeline/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Security Defense - USER role access to /admin/pipeline/status must return 403 Forbidden")
    @WithMockUser(username = "estudante@ufpe.br", roles = {"USER"})
    void adminStatusShouldBeRejectedWith403ForUserRole() throws Exception {
        mockMvc.perform(get("/admin/pipeline/status"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("Security Success - ADMIN role access to /admin/pipeline/status must return 200 OK")
    @WithMockUser(username = "admin@coralink.com", roles = {"ADMIN"})
    void adminStatusShouldSucceedForAdminRole() throws Exception {
        PipelineStatusResponse response = new PipelineStatusResponse(
                false, null, null, null, 1000L, "16m 40s", null
        );
        when(pipelineService.getPipelineStatus()).thenReturn(response);

        mockMvc.perform(get("/admin/pipeline/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formattedTimeRemaining").value("16m 40s"));
    }

    @Test
    @DisplayName("Security Defense - Anonymous access to trigger pipeline must return 401 Unauthorized")
    void triggerPipelineShouldBeRejectedWith401ForAnonymous() throws Exception {
        mockMvc.perform(post("/admin/pipeline/trigger"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security Defense - USER role access to trigger pipeline must return 403 Forbidden")
    @WithMockUser(username = "estudante@ufpe.br", roles = {"USER"})
    void triggerPipelineShouldBeRejectedWith403ForUserRole() throws Exception {
        mockMvc.perform(post("/admin/pipeline/trigger"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security Success - ADMIN role access to trigger pipeline must return 202 Accepted")
    @WithMockUser(username = "admin@coralink.com", roles = {"ADMIN"})
    void triggerPipelineShouldSucceedForAdminRole() throws Exception {
        mockMvc.perform(post("/admin/pipeline/trigger"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("Security Defense - Listing suggestions GET /suggestion must return 403 Forbidden for USER role")
    @WithMockUser(username = "estudante@ufpe.br", roles = {"USER"})
    void listSuggestionsShouldBeForbiddenForUserRole() throws Exception {
        mockMvc.perform(get("/suggestion"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security Defense - Anonymous POST /suggestion/create must return 401 Unauthorized")
    void createSuggestionShouldBeUnauthorizedForAnonymous() throws Exception {
        String requestJson = """
                {
                    "type": "FEATURE",
                    "suggestion": "Sugestão de nova funcionalidade",
                    "userEmail": "aluno@ufpe.br"
                }
                """;

        mockMvc.perform(post("/suggestion/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security Success - Authenticated USER role POST /suggestion/create must return 201 Created")
    @WithMockUser(username = "estudante@ufpe.br", roles = {"USER"})
    void createSuggestionShouldSucceedForAuthenticatedUser() throws Exception {
        io.github.lucasfcz.coralink.modules.auth.model.User mockUser = new io.github.lucasfcz.coralink.modules.auth.model.User(
                "estudante@ufpe.br",
                "Estudante Teste",
                null,
                null,
                io.github.lucasfcz.coralink.modules.auth.model.Role.ROLE_USER
        );
        when(authService.getCurrentAuthenticatedUser()).thenReturn(mockUser);

        io.github.lucasfcz.coralink.modules.usersuggestion.dto.UserSuggestionResponse mockResponse =
                new io.github.lucasfcz.coralink.modules.usersuggestion.dto.UserSuggestionResponse(
                        1L,
                        io.github.lucasfcz.coralink.modules.usersuggestion.model.SuggestionType.FEATURE,
                        "Sugestão de nova funcionalidade",
                        "estudante@ufpe.br"
                );
        when(userSuggestionService.createUserSuggestion(any(), any())).thenReturn(mockResponse);

        String requestJson = """
                {
                    "type": "FEATURE",
                    "suggestion": "Sugestão de nova funcionalidade",
                    "userEmail": "estudante@ufpe.br"
                }
                """;

        mockMvc.perform(post("/suggestion/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("FEATURE"));
    }

    @Test
    @DisplayName("Security Success - CORS pre-flight from http://localhost:3002 must be allowed")
    void corsShouldAllowLocalhost3002() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/opportunities/feed")
                        .header("Origin", "http://localhost:3002")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", "http://localhost:3002"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("Security Success - CORS pre-flight from https://coralink.me must be allowed")
    void corsShouldAllowVercelProduction() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/auth/google")
                        .header("Origin", "https://coralink.me")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", "https://coralink.me"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("Security Defense - USER role access to /admin/pipeline/failed-extractions must return 403 Forbidden")
    @WithMockUser(username = "estudante@ufpe.br", roles = {"USER"})
    void failedExtractionsShouldBeForbiddenForUserRole() throws Exception {
        mockMvc.perform(get("/admin/pipeline/failed-extractions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security Success - ADMIN role access to /admin/pipeline/failed-extractions must return 200 OK")
    @WithMockUser(username = "admin@coralink.com", roles = {"ADMIN"})
    void failedExtractionsShouldSucceedForAdminRole() throws Exception {
        when(adminDashboardService.getFailedExtractions(any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/admin/pipeline/failed-extractions"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security Success - ADMIN role access to DELETE /admin/opportunities/{id} triggers softDeleteOpportunity")
    @WithMockUser(username = "admin@coralink.com", roles = {"ADMIN"})
    void adminDeleteOpportunityShouldTriggerSoftDelete() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/admin/opportunities/10"))
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(opportunityService).softDeleteOpportunity(10L);
    }

    @Test
    @DisplayName("Security Defense - Unauthenticated access to GET /preferences must return 401 Unauthorized")
    void preferencesShouldBeUnauthorizedWithoutAuth() throws Exception {
        mockMvc.perform(get("/preferences"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security Success - Authenticated user access to GET /preferences must return 200 OK")
    @WithMockUser(username = "estudante@ufpe.br", roles = {"USER"})
    void preferencesShouldSucceedForAuthenticatedUser() throws Exception {
        io.github.lucasfcz.coralink.modules.auth.model.User user = new io.github.lucasfcz.coralink.modules.auth.model.User(
                "estudante@ufpe.br",
                "Estudante",
                "avatar.png",
                io.github.lucasfcz.coralink.modules.auth.model.Role.ROLE_USER
        );
        user.setId(5L);

        when(authService.getCurrentAuthenticatedUser()).thenReturn(user);
        when(userPreferencesService.getPreferencesByUserId(5L)).thenReturn(
                new UserPreferencesResponse(
                        1L,
                        5L,
                        java.util.Set.of(),
                        java.util.Set.of(),
                        java.util.Set.of(),
                        false,
                        java.time.LocalDateTime.now(),
                        java.time.LocalDateTime.now()
                )
        );

        mockMvc.perform(get("/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(5));
    }

    @Test
    @DisplayName("Security Defense - Unauthenticated access to POST /preferences must return 401 Unauthorized")
    void preferencesPostShouldBeUnauthorizedWithoutAuth() throws Exception {
        mockMvc.perform(post("/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security Defense - Unauthenticated access to PATCH /preferences must return 401 Unauthorized")
    void preferencesPatchShouldBeUnauthorizedWithoutAuth() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
