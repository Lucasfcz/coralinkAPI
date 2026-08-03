package io.github.lucasfcz.coralink.services;

import io.github.lucasfcz.coralink.dto.OpportunityResponse;
import io.github.lucasfcz.coralink.enums.*;
import io.github.lucasfcz.coralink.mappers.OpportunityMapper;
import io.github.lucasfcz.coralink.repositories.OpportunityRepository;
import io.github.lucasfcz.coralink.specifications.OpportunitySpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final OpportunityMapper opportunityMapper;

    // only get relevant opportunities that are upcoming or ongoing, and apply filters if provided
    public Page<OpportunityResponse> getRelevantOpportunities(
            OpportunityType type,
            Set<ThematicArea> thematicAreas,
            Set<TargetCourseAudience> targetCourseAudiences,
            Modality modality,
            Boolean isFree,
            Pageable pageable) {

        var spec = OpportunitySpecifications.filters(type, thematicAreas, targetCourseAudiences, modality, isFree);

        return opportunityRepository.findAll(spec, pageable).map(opportunityMapper::toResponse);
    }

    public Page<OpportunityResponse> getOpportunitiesByTitle(String title, Pageable pageable) {
        return opportunityRepository.findOpportunitiesByTitleContainsIgnoreCase(title, pageable).map(opportunityMapper::toResponse);
    }

    public OpportunityResponse getOpportunityById(Long id) {
        return opportunityRepository.findById(id)
                .map(opportunityMapper::toResponse)
                .orElse(null);
    }

    public int howManyOpportunitiesAreUpcoming() {
        return opportunityRepository.countAllByStartDateAfter(LocalDate.now());
    }
}
