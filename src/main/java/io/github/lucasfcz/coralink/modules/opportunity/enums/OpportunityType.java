package io.github.lucasfcz.coralink.modules.opportunity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum OpportunityType {
    // Eventos e Networking
    EVENT,
    WORKSHOP,

    // Formação e Capacitação
    COURSE,
    GRADUATION,

    // Competições e Maratonas
    HACKATHON,
    COMPETITION,

    // Carreira e Mercado
    INTERNSHIP,

    // Bolsas e Fomento
    SCHOLARSHIP,

    // Pesquisa e Academia
    RESEARCH,

    // Mobilidade e Extensão
    EXCHANGE_PROGRAM,
    VOLUNTEERING,

    // Outros
    OTHER;

    @JsonCreator
    public static OpportunityType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return OTHER;
        }
        try {
            return OpportunityType.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            return OTHER;
        }
    }
}