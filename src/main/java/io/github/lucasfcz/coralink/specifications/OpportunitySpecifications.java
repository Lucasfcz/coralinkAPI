package io.github.lucasfcz.coralink.specifications;

import io.github.lucasfcz.coralink.enums.Modality;
import io.github.lucasfcz.coralink.enums.OpportunityType;
import io.github.lucasfcz.coralink.enums.TargetAudience;
import io.github.lucasfcz.coralink.enums.ThematicArea;
import io.github.lucasfcz.coralink.model.Opportunity;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Set;

public class OpportunitySpecifications {

    public static Specification<Opportunity> filters(
            OpportunityType type,
            Set<ThematicArea> thematicAreas,
            Set<TargetAudience> targetAudiences,
            Modality modality,
            Boolean isFree) {

        return Specification
                .where(isUpcomingOrOngoing())
                .and(hasType(type))
                .and(hasThematicAreas(thematicAreas))
                .and(hasTargetAudiences(targetAudiences))
                .and(hasModality(modality))
                .and(hasIsFree(isFree));
    }

    //this get only relevant opportunities, which are the ones that are upcoming or ongoing
    private static Specification<Opportunity> isUpcomingOrOngoing() {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("eventDate")),
                cb.greaterThanOrEqualTo(root.get("eventDate"), LocalDate.now())
        );
    }

    private static Specification<Opportunity> hasType(OpportunityType type) {
        return (root, query, cb) -> type == null
                ? null
                : cb.equal(root.get("type"), type);
    }

    private static Specification<Opportunity> hasThematicAreas(Set<ThematicArea> areas) {
        return (root, query, cb) -> {
            if (areas == null || areas.isEmpty()) {
                return null;
            }
            query.distinct(true);
            return root.join("thematicAreas").in(areas);
        };
    }

    private static Specification<Opportunity> hasTargetAudiences(Set<TargetAudience> audiences) {
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
}