package io.github.lucasfcz.coralink.specifications;

import io.github.lucasfcz.coralink.enums.Modality;
import io.github.lucasfcz.coralink.enums.OpportunityType;
import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.enums.TargetCourseAudience;
import io.github.lucasfcz.coralink.model.Opportunity;
import io.github.lucasfcz.coralink.model.RawOpportunity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpportunitySpecificationsTest {

    @Test
    @DisplayName("Should build active specifications without error")
    void shouldBuildActiveSpecifications() {
        Specification<Opportunity> spec = OpportunitySpecifications.filters(
                OpportunityType.EVENT,
                Set.of(TargetCourseAudience.ADS),
                Modality.IN_PERSON,
                true,
                true
        );
        assertNotNull(spec);

        Specification<Opportunity> titleSpec = OpportunitySpecifications.activeWithTitle("Hackathon");
        assertNotNull(titleSpec);

        Specification<Opportunity> activeSpec = OpportunitySpecifications.isActive();
        assertNotNull(activeSpec);
    }
}
