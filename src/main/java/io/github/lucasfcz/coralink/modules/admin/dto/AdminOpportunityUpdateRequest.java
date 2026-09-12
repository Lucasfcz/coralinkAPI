package io.github.lucasfcz.coralink.modules.admin.dto;

import io.github.lucasfcz.coralink.modules.opportunity.enums.Modality;
import io.github.lucasfcz.coralink.modules.opportunity.enums.OpportunityType;
import io.github.lucasfcz.coralink.modules.opportunity.enums.TargetCourseAudience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Set;

/**
 * Payload para atualização administrativa de uma oportunidade.
 * Permite que um administrador corrija erros ou alucinações geradas pela IA.
 */
public record AdminOpportunityUpdateRequest(
        @NotBlank(message = "O título é obrigatório")
        String title,
        @NotBlank(message = "O resumo é obrigatório")
        String summary,
        @NotNull(message = "O tipo de oportunidade é obrigatório")
        OpportunityType type,
        String thematicArea,
        Modality modality,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate registrationDeadline,
        String location,
        @NotBlank(message = "A URL oficial é obrigatória")
        String officialUrl,
        String imageUrl,
        @NotNull(message = "O campo isFree é obrigatório")
        Boolean isFree,
        @NotNull(message = "O campo isForAll é obrigatório")
        Boolean isForAll,
        Set<TargetCourseAudience> targetCourseAudiences,
        @NotNull(message = "A data de expiração (expiresAt) é obrigatória")
        LocalDate expiresAt
) {
}
