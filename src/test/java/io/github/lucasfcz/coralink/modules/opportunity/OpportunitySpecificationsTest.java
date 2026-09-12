package io.github.lucasfcz.coralink.modules.opportunity;

import io.github.lucasfcz.coralink.modules.opportunity.enums.Modality;
import io.github.lucasfcz.coralink.modules.opportunity.enums.OpportunityType;
import io.github.lucasfcz.coralink.modules.opportunity.enums.TargetCourseAudience;
import io.github.lucasfcz.coralink.modules.opportunity.model.Opportunity;
import io.github.lucasfcz.coralink.modules.pipeline.model.RawOpportunity;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OpportunitySpecificationsTest {

    @Test
    @DisplayName("Should build filter specifications without error")
    void shouldBuildFilterSpecifications() {
        Specification<Opportunity> spec = OpportunitySpecifications.filters(
                "Hackathon",
                OpportunityType.EVENT,
                Set.of(TargetCourseAudience.ADS),
                Modality.IN_PERSON,
                true,
                true
        );
        assertNotNull(spec);

        Specification<Opportunity> nullFiltersSpec = OpportunitySpecifications.filters(
                null,
                null,
                null,
                null,
                null,
                null
        );
        assertNotNull(nullFiltersSpec);
    }

    @Test
    @DisplayName("Should create predicate correctly in isNotExpired specification")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldCreatePredicateInIsNotExpiredSpecification() {
        Root<Opportunity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path<LocalDate> expiresAtPath = mock(Path.class);
        when(root.get("expiresAt")).thenReturn((Path) expiresAtPath);

        Predicate dummyPredicate = mock(Predicate.class);
        when(cb.greaterThanOrEqualTo(any(Expression.class), any(LocalDate.class))).thenReturn(dummyPredicate);

        Specification<Opportunity> spec = OpportunitySpecifications.isNotExpired();
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(root).get("expiresAt");
        verify(cb).greaterThanOrEqualTo(eq(expiresAtPath), eq(LocalDate.now()));
    }

    @Nested
    @DisplayName("Opportunity Expiration Calculation Tests")
    class ExpirationCalculationTests {

        private RawOpportunity createRaw() {
            return new RawOpportunity(
                    "Oportunidade Teste",
                    "Resumo",
                    "https://example.com/noticia",
                    "CIN_UFPE",
                    null,
                    false
            );
        }

        @Test
        @DisplayName("Should calculate expiresAt as registrationDeadline + 3 days when deadline is present")
        void shouldExpireThreeDaysAfterRegistrationDeadline() {
            LocalDate deadline = LocalDate.now().plusDays(5);

            Opportunity opportunity = new Opportunity(
                    createRaw(),
                    "Resumo",
                    OpportunityType.EVENT,
                    null,
                    null,
                    null,
                    LocalDate.now().plusDays(10),
                    LocalDate.now().plusDays(15),
                    deadline,
                    null,
                    "https://example.com",
                    0.9,
                    null,
                    true,
                    true
            );

            assertEquals(deadline.plusDays(3), opportunity.getExpiresAt());
        }

        @Test
        @DisplayName("Should calculate expiresAt as endDate when registrationDeadline is null")
        void shouldExpireOnEndDateWhenNoDeadline() {
            LocalDate endDate = LocalDate.now().plusDays(12);

            Opportunity opportunity = new Opportunity(
                    createRaw(),
                    "Resumo",
                    OpportunityType.EVENT,
                    null,
                    null,
                    null,
                    LocalDate.now().plusDays(2),
                    endDate,
                    null,
                    null,
                    "https://example.com",
                    0.9,
                    null,
                    true,
                    true
            );

            assertEquals(endDate, opportunity.getExpiresAt());
        }

        @Test
        @DisplayName("Should calculate expiresAt as startDate when deadline and endDate are null")
        void shouldExpireOnStartDateWhenOnlyStartDatePresent() {
            LocalDate startDate = LocalDate.now().plusDays(7);

            Opportunity opportunity = new Opportunity(
                    createRaw(),
                    "Resumo",
                    OpportunityType.EVENT,
                    null,
                    null,
                    null,
                    startDate,
                    null,
                    null,
                    null,
                    "https://example.com",
                    0.9,
                    null,
                    true,
                    true
            );

            assertEquals(startDate, opportunity.getExpiresAt());
        }

        @Test
        @DisplayName("Should calculate expiresAt as 30 days from creation when no dates are provided")
        void shouldExpireIn30DaysWhenNoDatesProvided() {
            Opportunity opportunity = new Opportunity(
                    createRaw(),
                    "Resumo",
                    OpportunityType.COMPETITION,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "https://example.com",
                    0.9,
                    null,
                    true,
                    true
            );

            assertEquals(LocalDate.now().plusDays(30), opportunity.getExpiresAt());
        }
    }
}
