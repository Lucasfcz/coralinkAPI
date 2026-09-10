package io.github.lucasfcz.coralink.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.lucasfcz.coralink.dto.OpportunityResponse;
import io.github.lucasfcz.coralink.enums.Modality;
import io.github.lucasfcz.coralink.enums.OpportunityType;
import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.enums.TargetCourseAudience;
import io.github.lucasfcz.coralink.exceptions.GlobalExceptionHandler;
import io.github.lucasfcz.coralink.exceptions.NotFoundException;
import io.github.lucasfcz.coralink.services.OpportunityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OpportunityControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OpportunityService opportunityService;

    @InjectMocks
    private OpportunityController opportunityController;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(opportunityController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private OpportunityResponse buildSampleResponse(Long id, String title) {
        return new OpportunityResponse(
                id,
                title,
                "Resumo descritivo da oportunidade",
                OpportunityType.COURSE,
                "Tecnologia",
                Set.of(TargetCourseAudience.COMPUTER_SCIENCE),
                Modality.ONLINE,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(20),
                LocalDate.now().plusDays(5),
                "Recife, PE",
                "https://example.com/inscricao",
                SourceName.CIN_UFPE,
                "https://example.com/banner.png",
                true,
                true,
                LocalDate.now().plusDays(8)
        );
    }

    @Test
    @DisplayName("GET /opportunities - Should return 200 with paged opportunities")
    void shouldReturnPagedOpportunitiesSuccessfully() throws Exception {
        OpportunityResponse response = buildSampleResponse(1L, "Curso de Java");
        Page<OpportunityResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);

        when(opportunityService.getRelevantOpportunities(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/opportunities")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Curso de Java"))
                .andExpect(jsonPath("$.content[0].type").value("COURSE"))
                .andExpect(jsonPath("$.content[0].isFree").value(true))
                .andExpect(jsonPath("$.content[0].expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /opportunities - Should pass filter query params to service")
    void shouldPassFiltersToService() throws Exception {
        Page<OpportunityResponse> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(opportunityService.getRelevantOpportunities(
                eq("Java"),
                eq(OpportunityType.COURSE),
                eq(Set.of(TargetCourseAudience.COMPUTER_SCIENCE)),
                eq(Modality.ONLINE),
                eq(true),
                eq(true),
                any(Pageable.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/opportunities")
                        .param("title", "Java")
                        .param("type", "COURSE")
                        .param("targetCourseAudience", "COMPUTER_SCIENCE")
                        .param("modality", "ONLINE")
                        .param("isFree", "true")
                        .param("isForAll", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(opportunityService).getRelevantOpportunities(
                eq("Java"),
                eq(OpportunityType.COURSE),
                eq(Set.of(TargetCourseAudience.COMPUTER_SCIENCE)),
                eq(Modality.ONLINE),
                eq(true),
                eq(true),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("GET /opportunities/search - Should search by title and return 200")
    void shouldSearchOpportunitiesByTitle() throws Exception {
        OpportunityResponse response = buildSampleResponse(2L, "Hackathon Inovação");
        Page<OpportunityResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);

        when(opportunityService.getRelevantOpportunities(
                eq("Hackathon"),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/opportunities/search")
                        .param("title", "Hackathon")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Hackathon Inovação"));

        verify(opportunityService).getRelevantOpportunities(
                eq("Hackathon"),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("GET /opportunities/{id} - Should return 200 with opportunity details")
    void shouldReturnOpportunityById() throws Exception {
        OpportunityResponse response = buildSampleResponse(42L, "Bootcamp Cloud");
        when(opportunityService.getOpportunityById(42L)).thenReturn(response);

        mockMvc.perform(get("/opportunities/{id}", 42L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.title").value("Bootcamp Cloud"))
                .andExpect(jsonPath("$.officialUrl").value("https://example.com/inscricao"))
                .andExpect(jsonPath("$.sourceName").value("CIN_UFPE"));
    }

    @Test
    @DisplayName("GET /opportunities/{id} - Should return 404 when opportunity does not exist")
    void shouldReturnNotFoundWhenOpportunityDoesNotExist() throws Exception {
        when(opportunityService.getOpportunityById(999L))
                .thenThrow(new NotFoundException("Oportunidade não encontrada com o id: 999"));

        mockMvc.perform(get("/opportunities/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Oportunidade não encontrada com o id: 999"));
    }

    @Test
    @DisplayName("GET /opportunities/quantity - Should return 200 with count of upcoming opportunities")
    void shouldReturnQuantityOfUpcomingOpportunities() throws Exception {
        when(opportunityService.howManyOpportunitiesAreUpcoming()).thenReturn(15);

        mockMvc.perform(get("/opportunities/quantity")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("15"));
    }
}
