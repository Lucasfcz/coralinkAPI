package io.github.lucasfcz.coralink.services;

import io.github.lucasfcz.coralink.dto.OpportunityResponse;
import io.github.lucasfcz.coralink.enums.*;
import io.github.lucasfcz.coralink.exceptions.NotFoundException;
import io.github.lucasfcz.coralink.mappers.OpportunityMapper;
import io.github.lucasfcz.coralink.repositories.OpportunityRepository;
import io.github.lucasfcz.coralink.specifications.OpportunitySpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final OpportunityMapper opportunityMapper;

    // only get relevant opportunities that are active in real-time, and apply filters if provided
    @Cacheable(value = "opportunities", key = "{#type, #targetCourseAudiences, #modality, #isFree, #isForAll, #pageable}")
    public Page<OpportunityResponse> getRelevantOpportunities(
            OpportunityType type,
            Set<TargetCourseAudience> targetCourseAudiences,
            Modality modality,
            Boolean isFree,
            Boolean isForAll,
            Pageable pageable) {

        var spec = OpportunitySpecifications.filters(type, targetCourseAudiences, modality, isFree, isForAll);

        return opportunityRepository.findAll(spec, pageable).map(opportunityMapper::toResponse);
    }

    public Page<OpportunityResponse> getOpportunitiesByTitle(String title, Pageable pageable) {
        var spec = OpportunitySpecifications.activeWithTitle(title);
        return opportunityRepository.findAll(spec, pageable).map(opportunityMapper::toResponse);
    }

    public OpportunityResponse getOpportunityById(Long id) {
        return opportunityRepository.findById(id)
                .map(opportunityMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Not found opportunity if id: " + id));
    }

    @Cacheable(value = "opportunities_count")
    public int howManyOpportunitiesAreUpcoming() {
        return opportunityRepository.countActiveOpportunities(LocalDate.now(), LocalDateTime.now().minusDays(45));
    }
}
