package io.github.lucasfcz.coralink.modules.opportunity;

import io.github.lucasfcz.coralink.modules.ai.dto.ExtractionResult;
import io.github.lucasfcz.coralink.modules.opportunity.dto.OpportunityResponse;
import io.github.lucasfcz.coralink.modules.opportunity.enums.TargetCourseAudience;
import io.github.lucasfcz.coralink.modules.opportunity.model.Opportunity;
import io.github.lucasfcz.coralink.modules.pipeline.model.RawOpportunity;
import org.springframework.stereotype.Component;

@Component
public class OpportunityMapper {
    public Opportunity toEntity(RawOpportunity raw, ExtractionResult result, String fallbackImageUrl) {
        String resolvedImageUrl = (result.imageUrl() != null && !result.imageUrl().isBlank())
                ? result.imageUrl().trim()
                : fallbackImageUrl;

        return new Opportunity(
                raw,
                result.summary(),
                result.type(),
                result.thematicArea(),
                result.targetCourseAudiences(),
                result.modality(),
                result.startDate(),
                result.endDate(),
                result.registrationDeadline(),
                result.location(),
                raw.getNewsUrl(),
                result.confidenceScore(),
                resolvedImageUrl,
                result.isFree(),
                result.isForAll()
        );
    }

    public OpportunityResponse toResponse(Opportunity op) {
        java.util.Set<TargetCourseAudience> audiences = (op.getTargetCourseAudiences() != null)
                ? new java.util.HashSet<>(op.getTargetCourseAudiences())
                : java.util.Collections.emptySet();

        return new OpportunityResponse(
                op.getId(),
                op.getTitle(),
                op.getSummary(),
                op.getType(),
                op.getThematicArea(),
                audiences,
                op.getModality(),
                op.getStartDate(),
                op.getEndDate(),
                op.getRegistrationDeadline(),
                op.getLocation(),
                op.getOfficialUrl(),
                op.getSourceName(),
                op.getImageUrl(),
                op.getIsFree(),
                op.getIsForAll(),
                op.getExpiresAt());
    }
}
