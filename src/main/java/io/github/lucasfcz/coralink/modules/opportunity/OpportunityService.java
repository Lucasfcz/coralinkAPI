package io.github.lucasfcz.coralink.modules.opportunity;

import io.github.lucasfcz.coralink.modules.opportunity.dto.OpportunityResponse;
import io.github.lucasfcz.coralink.modules.opportunity.enums.*;
import io.github.lucasfcz.coralink.infra.exception.NotFoundException;
import io.github.lucasfcz.coralink.modules.opportunity.model.Opportunity;
import io.github.lucasfcz.coralink.modules.opportunity.repository.OpportunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Serviço responsável pela consulta, filtragem dinâmica e paginação de oportunidades ativas para os estudantes.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final OpportunityMapper opportunityMapper;

    // Retorna apenas oportunidades ativas segundo a regra temporal de vigência, aplicando filtros dinâmicos via JPA Specification.
    public Page<OpportunityResponse> getRelevantOpportunities(
            String title,
            OpportunityType type,
            Set<TargetCourseAudience> targetCourseAudiences,
            Modality modality,
            Boolean isFree,
            Boolean isForAll,
            Pageable pageable) {
        return getRelevantOpportunities(title, type, targetCourseAudiences, modality, null, isFree, isForAll, pageable);
    }

    @Cacheable(value = "opportunities", key = "{#title, #type, #targetCourseAudiences, #modality, #sourceName, #isFree, #isForAll, #pageable}")
    public Page<OpportunityResponse> getRelevantOpportunities(
            String title,
            OpportunityType type,
            Set<TargetCourseAudience> targetCourseAudiences,
            Modality modality,
            String sourceName,
            Boolean isFree,
            Boolean isForAll,
            Pageable pageable) {

        var spec = OpportunitySpecifications.filters(title, type, targetCourseAudiences, modality, sourceName, isFree, isForAll);

        return opportunityRepository.findAll(spec, pageable).map(opportunityMapper::toResponse);
    }

    @Cacheable(value = "opportunity_detail", key = "#id")
    public OpportunityResponse getOpportunityById(Long id) {
        return opportunityRepository.findById(id)
                .map(opportunityMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Oportunidade não encontrada com o id: " + id));
    }

    @Cacheable(value = "opportunities_count")
    public int howManyOpportunitiesAreUpcoming() {
        return opportunityRepository.countActiveOpportunities();
    }

    /**
     * Atualização administrativa de oportunidade.
     * Invalida todo o cache Redis de oportunidades para que os usuários vejam as correções imediatamente.
     */
    @CacheEvict(value = {"opportunities", "opportunity_detail", "opportunities_count"}, allEntries = true)
    @Transactional
    public OpportunityResponse updateOpportunity(Long id, io.github.lucasfcz.coralink.modules.admin.dto.AdminOpportunityUpdateRequest request) {
        Opportunity opportunity = opportunityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Oportunidade não encontrada com o id: " + id));

        opportunity.updateAdminFields(
                request.title(),
                request.summary(),
                request.type(),
                request.thematicArea(),
                request.modality(),
                request.startDate(),
                request.endDate(),
                request.registrationDeadline(),
                request.location(),
                request.officialUrl(),
                request.imageUrl(),
                request.isFree(),
                request.isForAll(),
                request.targetCourseAudiences(),
                request.expiresAt()
        );

        Opportunity updated = opportunityRepository.save(opportunity);
        return opportunityMapper.toResponse(updated);
    }

    /**
     * Exclusão administrativa de oportunidade errônea ou indesejada.
     * Invalida todo o cache Redis atômica e imediatamente.
     */
    @CacheEvict(value = {"opportunities", "opportunity_detail", "opportunities_count"}, allEntries = true)
    @Transactional
    public void softDeleteOpportunity(Long id) {

        Opportunity opportunity = opportunityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Oportunidade com id: " + id + " nao encontrada"));
        opportunity.makeOpportunityExpirates();
        opportunityRepository.save(opportunity);
    }
}

