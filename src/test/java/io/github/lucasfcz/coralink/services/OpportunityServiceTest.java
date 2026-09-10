package io.github.lucasfcz.coralink.services;

import io.github.lucasfcz.coralink.dto.OpportunityResponse;
import io.github.lucasfcz.coralink.enums.Modality;
import io.github.lucasfcz.coralink.enums.OpportunityType;
import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.enums.TargetCourseAudience;
import io.github.lucasfcz.coralink.exceptions.NotFoundException;
import io.github.lucasfcz.coralink.mappers.OpportunityMapper;
import io.github.lucasfcz.coralink.model.Opportunity;
import io.github.lucasfcz.coralink.model.RawOpportunity;
import io.github.lucasfcz.coralink.repositories.OpportunityRepository;
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
        RawOpportunity raw = RawOpportunity.builder()
                .title("Workshop de IA")
                .shortSummary("Resumo curto")
                .newsUrl("https://example.com/noticia")
                .sourceName(SourceName.CIN_UFPE)
                .becameOpportunity(false)
                .build();

        return Opportunity.builder()
                .rawOpportunity(raw)
                .summary("Resumo da oportunidade")
                .type(OpportunityType.WORKSHOP)
                .thematicArea("Inteligência Artificial")
                .targetCourseAudiences(Set.of(TargetCourseAudience.COMPUTER_SCIENCE))
                .modality(Modality.ONLINE)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(6))
                .registrationDeadline(LocalDate.now().plusDays(3))
                .location("Online")
                .officialUrl("https://example.com")
                .confidenceScoreAi(0.95)
                .imageUrl("https://example.com/banner.png")
                .isFree(true)
                .isForAll(true)
                .build();
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
                SourceName.CIN_UFPE,
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
