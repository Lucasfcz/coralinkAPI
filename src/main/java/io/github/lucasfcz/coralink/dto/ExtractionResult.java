package io.github.lucasfcz.coralink.dto;

import io.github.lucasfcz.coralink.enums.Modality;
import io.github.lucasfcz.coralink.enums.OpportunityType;
import io.github.lucasfcz.coralink.enums.TargetAudience;
import io.github.lucasfcz.coralink.enums.ThematicArea;
import java.time.LocalDate;
import java.util.Set;

public record ExtractionResult(
        Long rawOpportunityId,
        String summary,
        OpportunityType type,
        Set<ThematicArea> thematicAreas,
        Set<TargetAudience> targetAudiences,
        Modality modality,
        LocalDate eventDate,
        LocalDate registrationDeadline,
        String location,
        boolean isFree,
        Double confidenceScore
) {}