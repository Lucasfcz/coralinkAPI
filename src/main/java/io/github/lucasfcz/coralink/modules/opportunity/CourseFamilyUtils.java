package io.github.lucasfcz.coralink.modules.opportunity;

import io.github.lucasfcz.coralink.modules.opportunity.enums.TargetCourseAudience;

import java.util.*;

public final class CourseFamilyUtils {

    private CourseFamilyUtils() {}

    public static final Map<String, Set<TargetCourseAudience>> COURSE_AREA_FAMILIES = Map.of(
            "TECHNOLOGY", Set.of(
                    TargetCourseAudience.ADS,
                    TargetCourseAudience.COMPUTER_SCIENCE,
                    TargetCourseAudience.SOFTWARE_ENGINEERING,
                    TargetCourseAudience.INFORMATION_SYSTEMS,
                    TargetCourseAudience.COMPUTER_ENGINEERING,
                    TargetCourseAudience.DATA_SCIENCE,
                    TargetCourseAudience.TECHNOLOGY_STUDENTS
            ),
            "HEALTH", Set.of(
                    TargetCourseAudience.MEDICINE,
                    TargetCourseAudience.NURSING,
                    TargetCourseAudience.PHARMACY,
                    TargetCourseAudience.PHYSICAL_THERAPY,
                    TargetCourseAudience.PSYCHOLOGY,
                    TargetCourseAudience.PHYSICAL_EDUCATION,
                    TargetCourseAudience.DENTISTRY,
                    TargetCourseAudience.BIOMEDICINE,
                    TargetCourseAudience.NUTRITION,
                    TargetCourseAudience.VETERINARY_MEDICINE,
                    TargetCourseAudience.AESTHETICS,
                    TargetCourseAudience.HEALTH_STUDENTS
            ),
            "BUSINESS", Set.of(
                    TargetCourseAudience.BUSINESS_ADMINISTRATION,
                    TargetCourseAudience.ACCOUNTING,
                    TargetCourseAudience.ECONOMICS,
                    TargetCourseAudience.MARKETING,
                    TargetCourseAudience.BUSINESS_STUDENTS
            ),
            "COMMUNICATION_DESIGN", Set.of(
                    TargetCourseAudience.DESIGN,
                    TargetCourseAudience.GRAPHIC_DESIGN,
                    TargetCourseAudience.MARKETING,
                    TargetCourseAudience.ADVERTISING,
                    TargetCourseAudience.JOURNALISM
            ),
            "ENGINEERING", Set.of(
                    TargetCourseAudience.CIVIL_ENGINEERING,
                    TargetCourseAudience.ELECTRICAL_ENGINEERING,
                    TargetCourseAudience.MECHANICAL_ENGINEERING,
                    TargetCourseAudience.PRODUCTION_ENGINEERING,
                    TargetCourseAudience.CHEMICAL_ENGINEERING,
                    TargetCourseAudience.ENGINEERING_STUDENTS
            ),
            "EXACT_SCIENCES", Set.of(
                    TargetCourseAudience.MATHEMATICS,
                    TargetCourseAudience.STATISTICS,
                    TargetCourseAudience.PHYSICS,
                    TargetCourseAudience.CHEMISTRY,
                    TargetCourseAudience.EXACT_SCIENCES_STUDENTS
            ),
            "HUMANITIES", Set.of(
                    TargetCourseAudience.LAW,
                    TargetCourseAudience.PEDAGOGY,
                    TargetCourseAudience.SOCIAL_WORK,
                    TargetCourseAudience.LANGUAGE_AND_LITERATURE,
                    TargetCourseAudience.HUMANITIES_STUDENTS
            ),
            "OTHER_APPLIED", Set.of(
                    TargetCourseAudience.ARCHITECTURE_AND_URBANISM,
                    TargetCourseAudience.TOURISM_AND_HOSPITALITY,
                    TargetCourseAudience.GASTRONOMY
            )
    );

    /**
     * Expande o conjunto de cursos selecionados pelo usuário para incluir todos os cursos
     * das famílias acadêmicas correspondentes.
     */
    public static Set<TargetCourseAudience> expandToFamilies(Set<TargetCourseAudience> selected) {
        if (selected == null || selected.isEmpty()) {
            return Collections.emptySet();
        }
        Set<TargetCourseAudience> expanded = new HashSet<>(selected);
        for (TargetCourseAudience audience : selected) {
            for (Set<TargetCourseAudience> family : COURSE_AREA_FAMILIES.values()) {
                if (family.contains(audience)) {
                    expanded.addAll(family);
                }
            }
        }
        return expanded;
    }
}
