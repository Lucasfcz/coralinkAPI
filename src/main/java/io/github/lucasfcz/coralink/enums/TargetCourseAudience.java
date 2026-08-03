package io.github.lucasfcz.coralink.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum TargetCourseAudience {
    ADS,
    COMPUTER_SCIENCE,
    SOFTWARE_ENGINEERING,
    INFORMATION_SYSTEMS,
    COMPUTER_ENGINEERING,
    TECH_STUDENT,
    STUDENTS_IN_GENERAL;

    @JsonCreator
    public static TargetCourseAudience fromValue(String value) {
        try {
            return TargetCourseAudience.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return STUDENTS_IN_GENERAL;
        }
    }
}