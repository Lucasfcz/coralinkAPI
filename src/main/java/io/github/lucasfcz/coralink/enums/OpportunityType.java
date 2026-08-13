package io.github.lucasfcz.coralink.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum OpportunityType {
    // Eventos
    EVENT,
    LECTURE,
    WORKSHOP,
    MEETUP,
    NETWORKING,
    CONGRESS,
    SYMPOSIUM,
    SEMINAR,
    CONFERENCE,
    ACADEMIC_WEEK,

    // Competições
    HACKATHON,
    COMPETITION,
    CHALLENGE,
    OLYMPIAD,

    // Formação
    COURSE,
    BOOTCAMP,
    CERTIFICATION,
    TRAINING,
    WEBINAR,

    // Oportunidades acadêmicas
    SCHOLARSHIP,
    RESEARCH_PROGRAM,
    EXTENSION_PROGRAM,
    SCIENTIFIC_INITIATION,
    EXCHANGE_PROGRAM,
    MONITORING,

    // Mercado de trabalho
    INTERNSHIP_PROGRAM,
    TRAINEE_PROGRAM,
    JOB,
    EMPLOYABILITY_ACTION,

    // Chamadas
    EDITAL,
    EDITAL_RESULT,
    CALL_FOR_PAPERS,
    VOLUNTEERING,

    // Informativo / administrativo (categoria B do screening)
    ACADEMIC_CALENDAR,
    ADMINISTRATIVE_NOTICE,

    // Conteúdo
    ARTICLE,
    NEWS,
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