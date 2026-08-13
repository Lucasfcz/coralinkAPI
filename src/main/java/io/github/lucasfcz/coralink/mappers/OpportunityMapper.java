package io.github.lucasfcz.coralink.mappers;

import io.github.lucasfcz.coralink.dto.ExtractionResult;
import io.github.lucasfcz.coralink.dto.OpportunityResponse;
import io.github.lucasfcz.coralink.model.Opportunity;
import io.github.lucasfcz.coralink.model.RawOpportunity;
import org.springframework.stereotype.Component;

@Component
public class OpportunityMapper {
    public Opportunity toEntity(RawOpportunity raw, ExtractionResult result, String imageUrl) {
        return Opportunity.builder()
                .rawOpportunity(raw)
                .summary(result.summary())
                .type(result.type())
                .thematicArea(result.thematicArea())
                .targetCourseAudiences(result.targetCourseAudiences())
                .modality(result.modality())
                .startDate(result.startDate())
                .endDate(result.endDate())
                .registrationDeadline(result.registrationDeadline())
                .location(result.location())
                .officialUrl(raw.getNewsUrl())
                .confidenceScoreAi(result.confidenceScore())
                .imageUrl(imageUrl)
                .isFree(result.isFree())
                .isForAll(result.isForAll())
                .build();
    }

    public OpportunityResponse toResponse(Opportunity op) {
        return new OpportunityResponse(
                op.getId(),
                op.getTitle(),
                op.getSummary(),
                op.getType(),
                op.getThematicArea(),
                op.getTargetCourseAudiences(),
                op.getModality(),
                op.getStartDate(),
                op.getEndDate(),
                op.getRegistrationDeadline(),
                op.getLocation(),
                op.getOfficialUrl(),
                op.getImageUrl(),
                op.getIsFree(),
                op.getIsForAll());
    }
}
