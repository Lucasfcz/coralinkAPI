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

    // Opportunity is active if:
    // 1) date is not null: startDate >= now() OR (endDate is not null AND endDate >= now())
    // 2) date is null: activatedAt > now() - 45 days
    public static Specification<Opportunity> isActive() {
        return (root, query, cb) -> {
            LocalDate today = LocalDate.now();
            LocalDateTime cutoff45d = LocalDateTime.now().minusDays(45);

            Predicate hasDate = cb.isNotNull(root.get("startDate"));
            Predicate dateValid = cb.or(
                    cb.greaterThanOrEqualTo(root.get("startDate"), today),
                    cb.and(
                            cb.isNotNull(root.get("endDate")),
                            cb.greaterThanOrEqualTo(root.get("endDate"), today)
                    )
            );
            Predicate activeWithDate = cb.and(hasDate, dateValid);

            Predicate dateNull = cb.isNull(root.get("startDate"));
            Predicate activeWithoutDate = cb.and(
                    dateNull,
                    cb.greaterThan(root.get("activatedAt"), cutoff45d)
            );

            return cb.or(activeWithDate, activeWithoutDate);
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