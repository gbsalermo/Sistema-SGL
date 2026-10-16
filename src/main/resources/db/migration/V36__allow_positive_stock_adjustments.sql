-- ============================================================
-- V36 — Permitir ajustes positivos no saldo de Lote
-- Etapa 7.1-K — Ajustes físicos de estoque
-- ============================================================

ALTER TABLE lote
    DROP CONSTRAINT IF EXISTS ck_lote_quantidade_disponivel_limite;