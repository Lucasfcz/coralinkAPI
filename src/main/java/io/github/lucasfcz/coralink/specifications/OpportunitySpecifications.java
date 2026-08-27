package io.github.lucasfcz.coralink.specifications;

import io.github.lucasfcz.coralink.enums.*;
import io.github.lucasfcz.coralink.model.Opportunity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public class OpportunitySpecifications {

    public static Specification<Opportunity> filters(
            OpportunityType type,
            Set<TargetCourseAudience> targetCourseAudiences,
            Modality modality,
            Boolean isFree,
            Boolean isForAll
    ) {

        return Specification
                .where(isActive())
                .and(hasType(type))
                .and(hasTargetAudiences(targetCourseAudiences))
                .and(hasModality(modality))
                .and(hasIsFree(isFree))
                .and(hasIsForAll(isForAll));
    }

    public static Specification<Opportunity> activeWithTitle(String title) {
        return Specification
                .where(isActive())
                .and(hasTitle(title));
    }

    /**
     * Regra de uma oportunidade ativa:
     * 1) Boolean isActive == true (controle administrativo direto)
     * 2) Se houver prazo de inscrição (registrationDeadline != null):
     *    -> Prioridade absoluta: prazo de inscrição deve ser >= hoje - 3 dias (tolerância pós-encerramento).
     * 3) Se NÃO houver prazo de inscrição (registrationDeadline == null):
     *    a) Se houver data de início (startDate != null):
     *       -> startDate >= hoje OU (endDate != null E endDate >= hoje).
     *    b) Se NÃO houver data de início (startDate == null):
     *       -> Considera-se ativa se publicada nos últimos 45 dias (createdAt > hoje - 45 dias).
     */
    public static Specification<Opportunity> isActive() {

        return (root, query, cb) -> {
            LocalDate today = LocalDate.now();
            LocalDate deadlineCutoff = today.minusDays(3);
            LocalDateTime cutoff45d = LocalDateTime.now().minusDays(45);

            Predicate isActiveFlag = cb.isTrue(root.get("isActive"));

            Predicate activeWhenHasDeadline = cb.and(
                    cb.isNotNull(root.get("registrationDeadline")),
                    cb.greaterThanOrEqualTo(root.get("registrationDeadline"), deadlineCutoff)
            );

            Predicate hasDate = cb.isNotNull(root.get("startDate"));
            Predicate dateValid = cb.or(
                    cb.greaterThanOrEqualTo(root.get("startDate"), today),
                    cb.and(
                            cb.isNotNull(root.get("endDate")),
                            cb.greaterThanOrEqualTo(root.get("endDate"), today)
                    )
            );
            Predicate activeWithDate = cb.and(hasDate, dateValid);

            Predicate activeWithoutDate = cb.and(
                    cb.isNull(root.get("startDate")),
                    cb.greaterThan(root.get("createdAt"), cutoff45d)
            );

            Predicate activeWhenNoDeadline = cb.and(
                    cb.isNull(root.get("registrationDeadline")),
                    cb.or(activeWithDate, activeWithoutDate)
            );

            return cb.and(isActiveFlag, cb.or(activeWhenHasDeadline, activeWhenNoDeadline));
        };
    }

    private static Specification<Opportunity> hasTitle(String title) {
        return (root, query, cb) -> (title == null || title.isBlank())
                ? null
                : cb.like(cb.lower(root.get("title")), "%" + title.trim().toLowerCase() + "%");
    }

    private static Specification<Opportunity> hasType(OpportunityType type) {
        return (root, query, cb) -> type == null
                ? null
                : cb.equal(root.get("type"), type);
    }

    private static Specification<Opportunity> hasTargetAudiences(Set<TargetCourseAudience> audiences) {
        return (root, query, cb) -> {
            if (audiences == null || audiences.isEmpty()) {
                return null;
            }
            query.distinct(true);
            return root.join("targetCourseAudiences").in(audiences);
        };
    }

    private static Specification<Opportunity> hasModality(Modality modality) {
        return (root, query, cb) -> modality == null
                ? null
                : cb.equal(root.get("modality"), modality);
    }

    private static Specification<Opportunity> hasIsFree(Boolean isFree) {
        return (root, query, cb) -> isFree == null
                ? null
                : cb.equal(root.get("isFree"), isFree);
    }

    private static Specification<Opportunity> hasIsForAll(Boolean isForAll) {
        return (root, query, cb) -> isForAll == null
                ? null
                : cb.equal(root.get("isForAll"), isForAll);
    }
}