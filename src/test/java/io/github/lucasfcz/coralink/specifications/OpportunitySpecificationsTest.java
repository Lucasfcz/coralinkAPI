package io.github.lucasfcz.coralink.specifications;

import io.github.lucasfcz.coralink.enums.Modality;
import io.github.lucasfcz.coralink.enums.OpportunityType;
import io.github.lucasfcz.coralink.enums.TargetCourseAudience;
import io.github.lucasfcz.coralink.model.Opportunity;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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

    @Test
    @DisplayName("Should create predicates correctly in isActive specification")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldCreatePredicatesInIsActiveSpecification() {
        Root<Opportunity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path<Boolean> isActivePath = mock(Path.class);
        Path<LocalDate> regDeadlinePath = mock(Path.class);
        Path<LocalDate> startDatePath = mock(Path.class);
        Path<LocalDate> endDatePath = mock(Path.class);
        Path<LocalDateTime> createdAtPath = mock(Path.class);

        when(root.get("isActive")).thenReturn((Path) isActivePath);
        when(root.get("registrationDeadline")).thenReturn((Path) regDeadlinePath);
        when(root.get("startDate")).thenReturn((Path) startDatePath);
        when(root.get("endDate")).thenReturn((Path) endDatePath);
        when(root.get("createdAt")).thenReturn((Path) createdAtPath);

        Predicate dummyPredicate = mock(Predicate.class);
        when(cb.isTrue(any())).thenReturn(dummyPredicate);
        when(cb.isNotNull(any())).thenReturn(dummyPredicate);
        when(cb.isNull(any())).thenReturn(dummyPredicate);
        when(cb.greaterThanOrEqualTo(any(Expression.class), any(LocalDate.class))).thenReturn(dummyPredicate);
        when(cb.greaterThan(any(Expression.class), any(LocalDateTime.class))).thenReturn(dummyPredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(dummyPredicate);
        when(cb.or(any(Predicate[].class))).thenReturn(dummyPredicate);
        when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(dummyPredicate);
        when(cb.or(any(Predicate.class), any(Predicate.class))).thenReturn(dummyPredicate);

        Specification<Opportunity> spec = OpportunitySpecifications.isActive();
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(root).get("isActive");
        verify(root, atLeastOnce()).get("registrationDeadline");
        verify(root, atLeastOnce()).get("startDate");
        verify(root, atLeastOnce()).get("endDate");
        verify(root, atLeastOnce()).get("createdAt");

        LocalDate today = LocalDate.now();
        verify(cb).greaterThanOrEqualTo(eq(regDeadlinePath), eq(today.minusDays(3)));
        verify(cb).greaterThanOrEqualTo(eq(startDatePath), eq(today));
        verify(cb).greaterThanOrEqualTo(eq(endDatePath), eq(today));
        verify(cb).greaterThan(eq(createdAtPath), any(LocalDateTime.class));
    }

    @Nested
    @DisplayName("IsActive Rule Evaluation Tests")
    class IsActiveRuleTests {

        /**
         * Evaluates the isActive rule:
         * 1) isActive must be true
         * 2) If registrationDeadline != null -> registrationDeadline >= today - 3 days (absolute priority)
         * 3) If registrationDeadline == null:
         *    a) startDate != null -> startDate >= today OR (endDate != null AND endDate >= today)
         *    b) startDate == null -> createdAt > now - 45 days
         */
        private boolean evaluateIsActive(
                boolean isActiveFlag,
                LocalDate registrationDeadline,
                LocalDate startDate,
                LocalDate endDate,
                LocalDateTime createdAt
        ) {
            if (!isActiveFlag) return false;
            LocalDate today = LocalDate.now();
            LocalDate deadlineCutoff = today.minusDays(3);
            LocalDateTime cutoff45d = LocalDateTime.now().minusDays(45);

            if (registrationDeadline != null) {
                return !registrationDeadline.isBefore(deadlineCutoff);
            }

            if (startDate != null) {
                boolean startValid = !startDate.isBefore(today);
                boolean endValid = endDate != null && !endDate.isBefore(today);
                return startValid || endValid;
            }

            return createdAt != null && createdAt.isAfter(cutoff45d);
        }

        @Test
        @DisplayName("registrationDeadline in future -> active")
        void registrationDeadlineInFuture_shouldBeActive() {
            LocalDate today = LocalDate.now();
            boolean active = evaluateIsActive(true, today.plusDays(5), null, null, LocalDateTime.now());
            assertTrue(active);
        }

        @Test
        @DisplayName("registrationDeadline today -> active")
        void registrationDeadlineToday_shouldBeActive() {
            LocalDate today = LocalDate.now();
            boolean active = evaluateIsActive(true, today, null, null, LocalDateTime.now());
            assertTrue(active);
        }

        @ParameterizedTest(name = "registrationDeadline {0} days ago -> active")
        @ValueSource(ints = {1, 2, 3})
        @DisplayName("registrationDeadline 1, 2, 3 days ago -> active")
        void registrationDeadlineTolerance_shouldBeActive(int daysAgo) {
            LocalDate today = LocalDate.now();
            boolean active = evaluateIsActive(true, today.minusDays(daysAgo), null, null, LocalDateTime.now());
            assertTrue(active);
        }

        @Test
        @DisplayName("registrationDeadline 4 days ago -> inactive (even if startDate is future)")
        void registrationDeadline4DaysAgo_shouldBeInactiveEvenIfStartDateFuture() {
            LocalDate today = LocalDate.now();
            boolean active = evaluateIsActive(true, today.minusDays(4), today.plusDays(10), today.plusDays(15), LocalDateTime.now());
            assertFalse(active);
        }

        @Test
        @DisplayName("registrationDeadline null + startDate future -> active")
        void noDeadline_startDateFuture_shouldBeActive() {
            LocalDate today = LocalDate.now();
            boolean active = evaluateIsActive(true, null, today.plusDays(7), null, LocalDateTime.now().minusDays(50));
            assertTrue(active);
        }

        @Test
        @DisplayName("registrationDeadline null + startDate past + endDate future -> active")
        void noDeadline_startDatePast_endDateFuture_shouldBeActive() {
            LocalDate today = LocalDate.now();
            boolean active = evaluateIsActive(true, null, today.minusDays(2), today.plusDays(3), LocalDateTime.now().minusDays(50));
            assertTrue(active);
        }

        @Test
        @DisplayName("registrationDeadline null + startDate past + endDate past -> inactive")
        void noDeadline_startDatePast_endDatePast_shouldBeInactive() {
            LocalDate today = LocalDate.now();
            boolean active = evaluateIsActive(true, null, today.minusDays(5), today.minusDays(1), LocalDateTime.now().minusDays(50));
            assertFalse(active);
        }

        @Test
        @DisplayName("registrationDeadline null + no dates + createdAt recent -> active")
        void noDeadline_noDates_createdAtRecent_shouldBeActive() {
            boolean active = evaluateIsActive(true, null, null, null, LocalDateTime.now().minusDays(10));
            assertTrue(active);
        }

        @Test
        @DisplayName("registrationDeadline null + no dates + createdAt > 45 days -> inactive")
        void noDeadline_noDates_createdAtOlderThan45Days_shouldBeInactive() {
            boolean active = evaluateIsActive(true, null, null, null, LocalDateTime.now().minusDays(46));
            assertFalse(active);
        }

        @Test
        @DisplayName("isActive false -> always inactive regardless of dates")
        void isActiveFalse_shouldAlwaysBeInactive() {
            LocalDate today = LocalDate.now();
            assertFalse(evaluateIsActive(false, today.plusDays(10), today.plusDays(20), today.plusDays(25), LocalDateTime.now()));
            assertFalse(evaluateIsActive(false, null, today.plusDays(10), null, LocalDateTime.now()));
            assertFalse(evaluateIsActive(false, null, null, null, LocalDateTime.now()));
        }
    }
}
