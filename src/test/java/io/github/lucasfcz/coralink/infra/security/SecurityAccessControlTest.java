package io.github.lucasfcz.coralink.infra.security;

import io.github.lucasfcz.coralink.modules.admin.AdminController;
import io.github.lucasfcz.coralink.modules.auth.AuthController;
import io.github.lucasfcz.coralink.modules.opportunity.OpportunityController;
import io.github.lucasfcz.coralink.modules.userhelp.UserHelpController;
import io.github.lucasfcz.coralink.modules.opportunity.dto.OpportunityResponse;
import io.github.lucasfcz.coralink.modules.pipeline.dto.PipelineStatusResponse;
import io.github.lucasfcz.coralink.modules.auth.repository.UserRepository;
import io.github.lucasfcz.coralink.modules.auth.AuthService;
import io.github.lucasfcz.coralink.modules.opportunity.OpportunityService;
import io.github.lucasfcz.coralink.modules.admin.AdminDashboardService;
import io.github.lucasfcz.coralink.modules.pipeline.PipelineService;
import io.github.lucasfcz.coralink.modules.userhelp.UserHelpService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AdminController.class, OpportunityController.class, UserHelpController.class, AuthController.class})
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
    private UserHelpService userHelpService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    @MockitoBean
    private org.springframework.cache.CacheManager cacheManager;

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
        when(opportunityService.getRelevantOpportunities(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
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
}
