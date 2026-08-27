package io.github.lucasfcz.coralink.mappers;

import io.github.lucasfcz.coralink.dto.ExtractionResult;
import io.github.lucasfcz.coralink.enums.Modality;
import io.github.lucasfcz.coralink.enums.OpportunityType;
import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.enums.TargetCourseAudience;
import io.github.lucasfcz.coralink.model.Opportunity;
import io.github.lucasfcz.coralink.model.RawOpportunity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpportunityMapperTest {

    private final OpportunityMapper mapper = new OpportunityMapper();

    @Test
    @DisplayName("Should use AI extracted imageUrl when present")
    void shouldUseAiExtractedImageUrlWhenPresent() {
        RawOpportunity raw = RawOpportunity.builder()
                .title("Workshop AI")
                .shortSummary("Resumo")
                .newsUrl("https://example.com/workshop")
                .sourceName(SourceName.CIN_UFPE)
                .becameOpportunity(false)
                .build();

        ExtractionResult result = new ExtractionResult(
                1L,
                "Resumo extraído",
                OpportunityType.WORKSHOP,
                "Inteligência Artificial",
                Set.of(TargetCourseAudience.COMPUTER_SCIENCE),
                Modality.ONLINE,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 2),
                LocalDate.of(2026, 8, 30),
                "Online",
                true,
                true,
                "https://example.com/extracted-banner.png",
                0.95
        );

        Opportunity opportunity = mapper.toEntity(raw, result, "https://example.com/fallback.png");

        assertEquals("https://example.com/extracted-banner.png", opportunity.getImageUrl());
        assertTrue(opportunity.getIsActive());
        assertEquals("Workshop AI", opportunity.getTitle());
    }

    @Test
    @DisplayName("Should use fallback imageUrl when AI imageUrl is null or blank")
    void shouldUseFallbackImageUrlWhenAiImageUrlIsNullOrBlank() {
        RawOpportunity raw = RawOpportunity.builder()
                .title("Palestra Cloud")
                .shortSummary("Resumo")
                .newsUrl("https://example.com/palestra")
                .sourceName(SourceName.PORTO_DIGITAL)
                .becameOpportunity(false)
                .build();

        ExtractionResult resultWithNull = new ExtractionResult(
                1L,
                "Resumo extraído",
                OpportunityType.EVENT,
                "Cloud Computing",
                Set.of(TargetCourseAudience.ADS),
                Modality.IN_PERSON,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 8, 31),
                "Recife Antigo",
                true,
                true,
                null,
                0.90
        );

        Opportunity opportunity1 = mapper.toEntity(raw, resultWithNull, "https://example.com/fallback.png");
        assertEquals("https://example.com/fallback.png", opportunity1.getImageUrl());

        ExtractionResult resultWithBlank = new ExtractionResult(
                1L,
                "Resumo extraído",
                OpportunityType.EVENT,
                "Cloud Computing",
                Set.of(TargetCourseAudience.ADS),
                Modality.IN_PERSON,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 8, 31),
                "Recife Antigo",
                true,
                true,
                "   ",
                0.90
        );

        Opportunity opportunity2 = mapper.toEntity(raw, resultWithBlank, "https://example.com/fallback.png");
        assertEquals("https://example.com/fallback.png", opportunity2.getImageUrl());
    }
}
