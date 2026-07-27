package io.github.lucasfcz.coralink.controller;

import io.github.lucasfcz.coralink.config.AdminApiKeyProperties;
import io.github.lucasfcz.coralink.dto.PipelineRunResult;
import io.github.lucasfcz.coralink.exceptions.GlobalExceptionHandler;
import io.github.lucasfcz.coralink.exceptions.InvalidAdminApiKeyException;
import io.github.lucasfcz.coralink.exceptions.PipelineAlreadyRunningException;
import io.github.lucasfcz.coralink.services.PipelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerTest {

    private PipelineService pipelineService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        pipelineService = mock(PipelineService.class);
        AdminController controller = new AdminController(pipelineService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsOkWhenApiKeyIsValid() throws Exception {
        when(pipelineService.runFullPipeline("secret-key"))
                .thenReturn(new PipelineRunResult(5, 3, 2, 0, 0));

        mockMvc.perform(post("/admin/pipeline/run")
                        .header("X-Admin-Api-Key", "secret-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collected").value(5))
                .andExpect(jsonPath("$.screenedRelevant").value(3))
                .andExpect(jsonPath("$.createdOpportunities").value(2));
    }

    @Test
    void returnsUnauthorizedWhenApiKeyIsMissingOrInvalid() throws Exception {
        when(pipelineService.runFullPipeline(any()))
                .thenThrow(new InvalidAdminApiKeyException());

        mockMvc.perform(post("/admin/pipeline/run"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid admin API key"));
    }

    @Test
    void returnsConflictWhenPipelineIsAlreadyRunning() throws Exception {
        when(pipelineService.runFullPipeline("secret-key"))
                .thenThrow(new PipelineAlreadyRunningException());

        mockMvc.perform(post("/admin/pipeline/run")
                        .header("X-Admin-Api-Key", "secret-key"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Pipeline is already running"));
    }
}
