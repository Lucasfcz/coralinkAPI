package io.github.lucasfcz.coralink.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ThematicArea {
    BACKEND,
    FRONTEND,
    WEB_DEVELOPMENT,
    DATA_SCIENCE,
    ARTIFICIAL_INTELLIGENCE,
    CLOUD_INFRA,
    CYBERSECURITY,
    UX_UI,
    MOBILE,
    CAREER_EMPLOYABILITY,
    ENTREPRENEURSHIP,
    SOFTWARE_ENGINEERING,
    GENERAL_TECH,
    GENERAL;

    @JsonCreator
    public static ThematicArea fromValue(String value) {
        try {
            return ThematicArea.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return GENERAL;
        }
    }
}