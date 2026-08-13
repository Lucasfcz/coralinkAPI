package io.github.lucasfcz.coralink.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TargetCourseAudience {
    // Tecnologia
    ADS,
    COMPUTER_SCIENCE,
    SOFTWARE_ENGINEERING,
    INFORMATION_SYSTEMS,
    COMPUTER_ENGINEERING,
    DATA_SCIENCE,

    // Engenharias (não-computação)
    CIVIL_ENGINEERING,
    ELECTRICAL_ENGINEERING,
    MECHANICAL_ENGINEERING,
    PRODUCTION_ENGINEERING,
    CHEMICAL_ENGINEERING,

    // Exatas
    MATHEMATICS,
    STATISTICS,
    PHYSICS,
    CHEMISTRY,

    // Negócios
    BUSINESS_ADMINISTRATION,
    ACCOUNTING,
    ECONOMICS,

    // Comunicação e design
    DESIGN,
    GRAPHIC_DESIGN,
    MARKETING,
    ADVERTISING,
    JOURNALISM,

    // Saúde
    MEDICINE,
    NURSING,
    PHARMACY,
    PHYSICAL_THERAPY,
    PSYCHOLOGY,
    PHYSICAL_EDUCATION,
    DENTISTRY,
    BIOMEDICINE,
    NUTRITION,
    VETERINARY_MEDICINE,
    AESTHETICS,

    // Direito e humanas
    LAW,
    PEDAGOGY,
    SOCIAL_WORK,
    LANGUAGE_AND_LITERATURE,

    // Outras aplicadas
    ARCHITECTURE_AND_URBANISM,
    TOURISM_AND_HOSPITALITY,
    GASTRONOMY,

    // Categorias amplas (fallback por área)
    TECHNOLOGY_STUDENTS,
    ENGINEERING_STUDENTS,
    EXACT_SCIENCES_STUDENTS,
    HEALTH_STUDENTS,
    HUMANITIES_STUDENTS,
    BUSINESS_STUDENTS,

    // Coringa final
    UNIVERSITY_STUDENTS;

    @JsonCreator
    public static TargetCourseAudience fromValue(String value) {
        try {
            return TargetCourseAudience.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return UNIVERSITY_STUDENTS;
        }
    }
}