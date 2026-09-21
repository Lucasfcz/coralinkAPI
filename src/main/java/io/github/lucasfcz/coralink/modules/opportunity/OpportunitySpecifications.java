package io.github.lucasfcz.coralink.modules.opportunity;

import io.github.lucasfcz.coralink.modules.opportunity.enums.*;
import io.github.lucasfcz.coralink.modules.opportunity.model.Opportunity;
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
        return filters(title, type, targetCourseAudiences, modality, null, isFree, isForAll);
    }

    public static Specification<Opportunity> filters(
            String title,
            OpportunityType type,
            Set<TargetCourseAudience> targetCourseAudiences,
            Modality modality,
            String sourceName,
            Boolean isFree,
            Boolean isForAll
    ) {

        return Specification
                .where(isNotExpired())
                .and(hasTitle(title))
                .and(hasType(type))
                .and(hasTargetAudiences(targetCourseAudiences))
                .and(hasModality(modality))
                .and(hasSourceName(sourceName))
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

    private static Specification<Opportunity> hasSourceName(String sourceName) {
        return (root, query, cb) -> (sourceName == null || sourceName.isBlank())
                ? null
                : cb.equal(cb.upper(root.get("sourceName")), sourceName.trim().toUpperCase());
    }

    /**
     * Ordenação hierárquica inteligente para o feed público:
     * 1. Prioriza oportunidades com prazo de inscrição ativo (da mais próxima de encerrar para a mais distante);
     * 2. Em seguida, oportunidades com data de início próxima (eventos/atividades acontecendo em breve);
     * 3. Por fim, informativos/notícias sem data definida (ordenados por criação/id decrescente).
     */
    public static Specification<Opportunity> defaultSmartUrgencyOrder() {
        return (root, query, cb) -> {
            if (query != null && !Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                LocalDate today = LocalDate.now();

                var urgencyGroup = cb.<Integer>selectCase()
                        .when(cb.greaterThanOrEqualTo(root.get("registrationDeadline"), today), 1)
                        .when(cb.greaterThanOrEqualTo(root.get("startDate"), today), 2)
                        .when(cb.greaterThanOrEqualTo(root.get("endDate"), today), 3)
                        .otherwise(4);

                var activeDate = cb.<LocalDate>selectCase()
                        .when(cb.greaterThanOrEqualTo(root.get("registrationDeadline"), today), root.get("registrationDeadline"))
                        .when(cb.greaterThanOrEqualTo(root.get("startDate"), today), root.get("startDate"))
                        .otherwise(cb.nullLiteral(LocalDate.class));

                query.orderBy(
                        cb.asc(urgencyGroup),
                        cb.asc(activeDate),
                        cb.desc(root.get("id"))
                );
            }
            return null;
        };
    }
}