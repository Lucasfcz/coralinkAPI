package io.github.lucasfcz.coralink.modules.opportunity;

import io.github.lucasfcz.coralink.modules.opportunity.dto.OpportunityResponse;
import io.github.lucasfcz.coralink.modules.opportunity.enums.Modality;
import io.github.lucasfcz.coralink.modules.opportunity.enums.OpportunityType;
import io.github.lucasfcz.coralink.modules.opportunity.enums.TargetCourseAudience;
import io.github.lucasfcz.coralink.infra.exception.NotFoundException;
import io.github.lucasfcz.coralink.modules.opportunity.OpportunityMapper;
import io.github.lucasfcz.coralink.modules.opportunity.model.Opportunity;
import io.github.lucasfcz.coralink.modules.pipeline.model.RawOpportunity;
import io.github.lucasfcz.coralink.modules.opportunity.repository.OpportunityRepository;
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
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpportunityServiceTest {

    @Mock
    private OpportunityRepository opportunityRepository;

    @Mock
    private OpportunityMapper opportunityMapper;

    @InjectMocks
    private OpportunityService opportunityService;

    private Opportunity buildSampleOpportunity() {
        RawOpportunity raw = new RawOpportunity(
                "Workshop de IA",
                "Resumo curto",
                "https://example.com/noticia",
                "CIN_UFPE",
                null,
                false
        );

        return new Opportunity(
                raw,
                "Resumo da oportunidade",
                OpportunityType.WORKSHOP,
                "Inteligência Artificial",
                Set.of(TargetCourseAudience.COMPUTER_SCIENCE),
                Modality.ONLINE,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(6),
                LocalDate.now().plusDays(3),
                "Online",
                "https://example.com",
                0.95,
                "https://example.com/banner.png",
                true,
                true
        );
    }

    private OpportunityResponse buildSampleResponse() {
        return new OpportunityResponse(
                1L,
                "Workshop de IA",
                "Resumo da oportunidade",
                OpportunityType.WORKSHOP,
                "Inteligência Artificial",
                Set.of(TargetCourseAudience.COMPUTER_SCIENCE),
                Modality.ONLINE,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(6),
                LocalDate.now().plusDays(3),
                "Online",
                "https://example.com",
                "CIN_UFPE",
                "https://example.com/banner.png",
                true,
                true,
                LocalDate.now().plusDays(6)
        );
    }

    @Test
    @DisplayName("Should return paged opportunity responses with filters applied")
    @SuppressWarnings("unchecked")
    void shouldReturnPagedOpportunities() {
        Opportunity opportunity = buildSampleOpportunity();
        OpportunityResponse response = buildSampleResponse();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Opportunity> entityPage = new PageImpl<>(List.of(opportunity), pageable, 1);

        when(opportunityRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(entityPage);
        when(opportunityMapper.toResponse(opportunity)).thenReturn(response);

        Page<OpportunityResponse> result = opportunityService.getRelevantOpportunities(
                "Workshop",
                OpportunityType.WORKSHOP,
                Set.of(TargetCourseAudience.COMPUTER_SCIENCE),
                Modality.ONLINE,
                true,
                true,
                pageable
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Workshop de IA", result.getContent().getFirst().title());
        verify(opportunityRepository).findAll(any(Specification.class), eq(pageable));
        verify(opportunityMapper).toResponse(opportunity);
    }

    @Test
    @DisplayName("Should return opportunity response when found by ID")
    void shouldReturnOpportunityByIdWhenFound() {
        Opportunity opportunity = buildSampleOpportunity();
        OpportunityResponse response = buildSampleResponse();

        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(opportunity));
        when(opportunityMapper.toResponse(opportunity)).thenReturn(response);

        OpportunityResponse result = opportunityService.getOpportunityById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Workshop de IA", result.title());
        verify(opportunityRepository).findById(1L);
        verify(opportunityMapper).toResponse(opportunity);
    }

    @Test
    @DisplayName("Should throw NotFoundException when opportunity not found by ID")
    void shouldThrowNotFoundExceptionWhenNotFoundById() {
        when(opportunityRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> opportunityService.getOpportunityById(999L)
        );

        assertTrue(exception.getMessage().contains("999"));
        verify(opportunityRepository).findById(999L);
        verifyNoInteractions(opportunityMapper);
    }

    @Test
    @DisplayName("Should return count of upcoming opportunities")
    void shouldReturnUpcomingOpportunitiesCount() {
        when(opportunityRepository.countActiveOpportunities()).thenReturn(42);

        int count = opportunityService.howManyOpportunitiesAreUpcoming();

        assertEquals(42, count);
        verify(opportunityRepository).countActiveOpportunities();
    }
}
