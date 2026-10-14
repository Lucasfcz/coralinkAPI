package io.github.lucasfcz.coralink.modules.opportunity;

import io.github.lucasfcz.coralink.modules.opportunity.enums.*;
import io.github.lucasfcz.coralink.modules.opportunity.model.Opportunity;
import io.github.lucasfcz.coralink.modules.preferences.model.UserPreferences;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
        return filters(title, type != null ? Set.of(type) : null, targetCourseAudiences, modality, (Set<String>) null, isFree, isForAll);
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
        Set<String> sources = (sourceName != null && !sourceName.isBlank()) ? Set.of(sourceName) : null;
        return filters(title, type != null ? Set.of(type) : null, targetCourseAudiences, modality, sources, isFree, isForAll);
    }

    public static Specification<Opportunity> filters(
            String title,
            Set<OpportunityType> types,
            Set<TargetCourseAudience> targetCourseAudiences,
            Modality modality,
            Set<String> sourceNames,
            Boolean isFree,
            Boolean isForAll
    ) {
        return Specification
                .where(isNotExpired())
                .and(hasTitle(title))
                .and(hasTypes(types))
                .and(hasTargetAudiences(targetCourseAudiences))
                .and(hasModality(modality))
                .and(hasSourceNames(sourceNames))
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

    private static Specification<Opportunity> hasTypes(Set<OpportunityType> types) {
        return (root, query, cb) -> {
            if (types == null || types.isEmpty()) {
                return null;
            }
            return root.get("type").in(types);
        };
    }

    private static Specification<Opportunity> hasTargetAudiences(Set<TargetCourseAudience> audiences) {
        return (root, query, cb) -> {
            if (audiences == null || audiences.isEmpty()) {
                return null;
            }
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Opportunity> subRoot = subquery.from(Opportunity.class);
            Join<Opportunity, TargetCourseAudience> join = subRoot.join("targetCourseAudiences");
            subquery.select(subRoot.get("id"))
                    .where(
                            cb.equal(subRoot.get("id"), root.get("id")),
                            join.in(audiences)
                    );
            return cb.exists(subquery);
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

    private static Specification<Opportunity> hasSourceNames(Set<String> sourceNames) {
        return (root, query, cb) -> {
            if (sourceNames == null || sourceNames.isEmpty()) {
                return null;
            }
            CriteriaBuilder.In<String> inClause = cb.in(cb.upper(root.get("sourceName")));
            for (String source : sourceNames) {
                if (source != null && !source.isBlank()) {
                    inClause.value(source.trim().toUpperCase());
                }
            }
            return inClause;
        };
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

    /**
     * Ordenação calculada diretamente na query SQL do banco de dados com base nas preferências do usuário:
     * - Família / Cursos de Interesse: 100 pontos (ou 35 para oportunidades abertas a universitários gerais);
     * - Tipo de oportunidade selecionado: 35 pontos;
     * - Instituição de preferência: 25 pontos (ou 35/30 se não estiver na faculdade);
     * - Gratuidade: 20 pontos se isFree = true;
     * - Prazo de inscrição iminente: 15 pontos se até 5 dias, 10 pontos entre 6 e 15 dias.
     */
    public static Specification<Opportunity> scoringOrder(UserPreferences prefs) {
        return (root, query, cb) -> {
            if (query == null || Long.class.equals(query.getResultType()) || long.class.equals(query.getResultType())) {
                return null;
            }

            LocalDate today = LocalDate.now();
            LocalDate in5Days = today.plusDays(5);
            LocalDate in15Days = today.plusDays(15);

            // 1. Cursos de Interesse e Grande Área (100 pontos para área, 35 para oportunidade universal)
            Expression<Integer> courseScore;
            Set<TargetCourseAudience> targetCourses = prefs != null ? prefs.getTargetCourses() : null;

            Subquery<Long> uniSubquery = query.subquery(Long.class);
            Root<Opportunity> uniSubRoot = uniSubquery.from(Opportunity.class);
            Join<Opportunity, TargetCourseAudience> uniJoin = uniSubRoot.join("targetCourseAudiences");
            uniSubquery.select(uniSubRoot.get("id"))
                    .where(
                            cb.equal(uniSubRoot.get("id"), root.get("id")),
                            cb.equal(uniJoin, TargetCourseAudience.UNIVERSITY_STUDENTS)
                    );
            Predicate uniMatch = cb.or(cb.isTrue(root.get("isForAll")), cb.exists(uniSubquery));

            if (targetCourses != null && !targetCourses.isEmpty()) {
                Set<TargetCourseAudience> expandedCourses = CourseFamilyUtils.expandToFamilies(targetCourses);

                Subquery<Long> courseSubquery = query.subquery(Long.class);
                Root<Opportunity> courseSubRoot = courseSubquery.from(Opportunity.class);
                Join<Opportunity, TargetCourseAudience> courseJoin = courseSubRoot.join("targetCourseAudiences");
                courseSubquery.select(courseSubRoot.get("id"))
                        .where(
                                cb.equal(courseSubRoot.get("id"), root.get("id")),
                                courseJoin.in(expandedCourses)
                        );
                Predicate courseMatch = cb.exists(courseSubquery);

                courseScore = cb.<Integer>selectCase()
                        .when(courseMatch, 100)
                        .when(uniMatch, 35)
                        .otherwise(0);
            } else {
                courseScore = cb.<Integer>selectCase()
                        .when(uniMatch, 35)
                        .otherwise(0);
            }

            // 2. Tipo de Oportunidade (35 pontos se coincidir)
            Expression<Integer> typeScore;
            Set<OpportunityType> selectedTypes = prefs != null ? prefs.getSelectedTypes() : null;
            if (selectedTypes != null && !selectedTypes.isEmpty()) {
                typeScore = cb.<Integer>selectCase()
                        .when(root.get("type").in(selectedTypes), 35)
                        .otherwise(0);
            } else {
                typeScore = cb.literal(0);
            }

            // 3. Instituição / Situação Acadêmica (Até 35 pontos)
            Expression<Integer> instScore;
            if (prefs != null && prefs.isNotInCollege()) {
                instScore = cb.<Integer>selectCase()
                        .when(cb.and(cb.equal(root.get("type"), OpportunityType.GRADUATION), cb.isTrue(root.get("isForAll"))), 65)
                        .when(cb.equal(root.get("type"), OpportunityType.GRADUATION), 35)
                        .when(cb.isTrue(root.get("isForAll")), 30)
                        .otherwise(0);
            } else {
                Set<String> institutions = prefs != null ? prefs.getInstitutions() : null;
                if (institutions != null && !institutions.isEmpty()) {
                    List<Predicate> instPredicates = new ArrayList<>();
                    for (String inst : institutions) {
                        if (inst != null && !inst.isBlank()) {
                            instPredicates.add(cb.like(cb.upper(root.get("sourceName")), "%" + inst.trim().toUpperCase() + "%"));
                        }
                    }
                    if (!instPredicates.isEmpty()) {
                        instScore = cb.<Integer>selectCase()
                                .when(cb.or(instPredicates.toArray(new Predicate[0])), 25)
                                .otherwise(0);
                    } else {
                        instScore = cb.literal(0);
                    }
                } else {
                    instScore = cb.literal(0);
                }
            }

            // 4. Gratuidade (20 pontos) e Urgência do Prazo (15 pontos até 5 dias, 10 pontos entre 6 e 15 dias)
            Expression<Integer> freeScore = cb.<Integer>selectCase()
                    .when(cb.isTrue(root.get("isFree")), 20)
                    .otherwise(0);

            Expression<Integer> urgencyScore = cb.<Integer>selectCase()
                    .when(cb.and(
                            cb.greaterThanOrEqualTo(root.get("registrationDeadline"), today),
                            cb.lessThanOrEqualTo(root.get("registrationDeadline"), in5Days)
                    ), 15)
                    .when(cb.and(
                            cb.greaterThan(root.get("registrationDeadline"), in5Days),
                            cb.lessThanOrEqualTo(root.get("registrationDeadline"), in15Days)
                    ), 10)
                    .otherwise(0);

            // Soma total dos scores
            Expression<Integer> totalScore = cb.sum(courseScore, typeScore);
            totalScore = cb.sum(totalScore, instScore);
            totalScore = cb.sum(totalScore, freeScore);
            totalScore = cb.sum(totalScore, urgencyScore);

            // Desempate por urgência cronológica padrão
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
                    cb.desc(totalScore),
                    cb.asc(urgencyGroup),
                    cb.asc(activeDate),
                    cb.desc(root.get("id"))
            );

            return null;
        };
    }
}