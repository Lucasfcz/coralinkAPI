package io.github.lucasfcz.coralink.services;

import io.github.lucasfcz.coralink.exceptions.NotFoundException;
import io.github.lucasfcz.coralink.mappers.OpportunityMapper;
import io.github.lucasfcz.coralink.repositories.OpportunityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OpportunityServiceTest {

    @Test
    void throwsNotFoundExceptionWhenOpportunityDoesNotExist() {
        OpportunityRepository repository = mock(OpportunityRepository.class);
        OpportunityMapper mapper = mock(OpportunityMapper.class);
        when(repository.findById(999L)).thenReturn(Optional.empty());

        OpportunityService service = new OpportunityService(repository, mapper);

        assertThrows(NotFoundException.class, () -> service.getOpportunityById(999L));
    }

    @Test
    void countsUpcomingOpportunitiesMatchingFeedRules() {
        OpportunityRepository repository = mock(OpportunityRepository.class);
        OpportunityMapper mapper = mock(OpportunityMapper.class);
        when(repository.count(any(Specification.class))).thenReturn(7L);

        OpportunityService service = new OpportunityService(repository, mapper);

        int count = service.howManyOpportunitiesAreUpcoming();

        assertEquals(7, count);
        verify(repository, times(1)).count(any(Specification.class));
    }
}
