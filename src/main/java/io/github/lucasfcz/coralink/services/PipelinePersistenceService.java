package io.github.lucasfcz.coralink.services;

import io.github.lucasfcz.coralink.dto.DetailedContent;
import io.github.lucasfcz.coralink.dto.ExtractionResult;
import io.github.lucasfcz.coralink.dto.ScreeningResult;
import io.github.lucasfcz.coralink.mappers.OpportunityMapper;
import io.github.lucasfcz.coralink.model.RawOpportunity;
import io.github.lucasfcz.coralink.repositories.OpportunityRepository;
import io.github.lucasfcz.coralink.repositories.RawOpportunityRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

//this class is separated from Pipeline Service to use @Transational in methods
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
    public void saveOpportunity(RawOpportunity raw, ExtractionResult result, DetailedContent content) {
        opportunityRepository.save(opportunityMapper.toEntity(raw, result, content.imageUrl()));
        raw.markAsOpportunity();
        rawOpportunityRepository.save(raw);
    }
}
