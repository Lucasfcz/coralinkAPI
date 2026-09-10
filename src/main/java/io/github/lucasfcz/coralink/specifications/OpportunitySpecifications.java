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
            String title,
            OpportunityType type,
            Set<TargetCourseAudience> targetCourseAudiences,
            Modality modality,
            Boolean isFree,
            Boolean isForAll
    ) {

        return Specification
                .where(isNotExpired())
                .and(hasTitle(title))
                .and(hasType(type))
                .and(hasTargetAudiences(targetCourseAudiences))
                .and(hasModality(modality))
                .and(hasIsFree(isFree))
                .and(hasIsForAll(isForAll));
    }

    public static Specification<Opportunity> isNotExpired() {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("expiresAt"), LocalDate.now());
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