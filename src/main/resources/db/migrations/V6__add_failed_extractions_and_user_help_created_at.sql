-- ==============================================================================
-- Migration V6: Adição de controle de tentativas de extração de IA (raw_opportunities)
-- e data de criação em sugestões de estudantes (user_help)
-- ==============================================================================

-- 1. Controle de resiliência e tentativas de extração na IA para oportunidades brutas
ALTER TABLE raw_opportunities
    ADD COLUMN extraction_attempts INT DEFAULT 0 NOT NULL,
    ADD COLUMN last_extraction_error TEXT;

CREATE INDEX idx_raw_opportunities_failed_extraction
    ON raw_opportunities (screened_relevant, became_opportunity, extraction_attempts);

-- 2. Registro cronológico para sugestões e reportes de ajuda de usuários
ALTER TABLE user_help
    ADD COLUMN created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;
