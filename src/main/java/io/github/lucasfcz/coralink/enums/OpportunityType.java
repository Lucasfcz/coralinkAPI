package io.github.lucasfcz.coralink.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum OpportunityType {
    EVENT,
    HACKATHON,
    EDITAL,
    ARTICLE,
    BOOTCAMP,
    CERTIFICATION,
    COURSE,
    INTERNSHIP_PROGRAM,
    SCHOLARSHIP,
    JOB,
    COMPETITION,
    LECTURE,
    NETWORKING,
    WORKSHOP,
    MEETUP,
    OTHER;

    @JsonCreator
    public static OpportunityType fromValue(String value) {
        try {
            return OpportunityType.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return OTHER;
        }
    }
}