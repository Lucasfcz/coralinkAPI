package io.github.lucasfcz.coralink.specifications;

import io.github.lucasfcz.coralink.enums.*;
import io.github.lucasfcz.coralink.model.Opportunity;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
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
                .where(isUpcomingOrOngoing())
                .and(hasType(type))
                .and(hasTargetAudiences(targetCourseAudiences))
                .and(hasModality(modality))
                .and(hasIsFree(isFree))
                .and(hasIsForALl(isForAll));
    }

    //this get only relevant opportunities, which are the ones that are upcoming or ongoing
    private static Specification<Opportunity> isUpcomingOrOngoing() {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("startDate")),
                cb.greaterThanOrEqualTo(root.get("startDate"), LocalDate.now())
        );
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
            return root.join("targetAudiences").in(audiences);
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

    private static Specification<Opportunity> hasIsForALl(Boolean isForAll) {
        return (root, query, cb) -> isForAll == null
                ? null
                : cb.equal(root.get("isForAll"), isForAll);
    }
}