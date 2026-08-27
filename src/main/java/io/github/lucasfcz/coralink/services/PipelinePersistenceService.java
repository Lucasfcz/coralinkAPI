package io.github.lucasfcz.coralink.services;

import io.github.lucasfcz.coralink.dto.ExtractionResult;
import io.github.lucasfcz.coralink.dto.ScreeningResult;
import io.github.lucasfcz.coralink.mappers.OpportunityMapper;
import io.github.lucasfcz.coralink.model.RawOpportunity;
import io.github.lucasfcz.coralink.repositories.OpportunityRepository;
import io.github.lucasfcz.coralink.repositories.RawOpportunityRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Serviço de persistência transacional do pipeline.
 * Isolado do PipelineService para permitir o controle transacional granular (@Transactional)
 * em cada etapa (triagem e persistência de oportunidade), evitando que falhas individuais abortem a esteira completa.
 */
@Service
@RequiredArgsConstructor
public class PipelinePersistenceService {


    private final RawOpportunityRepository rawOpportunityRepository;
    private final OpportunityRepository opportunityRepository;
    private final OpportunityMapper opportunityMapper;

    @Transactional
    public void saveScreening(RawOpportunity raw, ScreeningResult result) {
        raw.applyScreening(result.isRelevant());
        rawOpportunityRepository.save(raw);
    }

    @Transactional
    public void saveOpportunity(RawOpportunity raw, ExtractionResult result, String fallbackImageUrl) {
        opportunityRepository.save(opportunityMapper.toEntity(raw, result, fallbackImageUrl));
        raw.markAsOpportunity();
        rawOpportunityRepository.save(raw);
    }
}
