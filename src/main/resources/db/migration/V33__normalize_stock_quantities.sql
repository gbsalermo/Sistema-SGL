-- ============================================================
-- V33 — Normalização das quantidades físicas para NUMERIC
-- Etapa 7 — Unidades, apresentações e estoque fracionável
-- ============================================================

-- Estoque central
ALTER TABLE estoque_central
    ALTER COLUMN quantidade_atual TYPE NUMERIC(19,6)
        USING quantidade_atual::NUMERIC(19,6),
    ALTER COLUMN quantidade_minima TYPE NUMERIC(19,6)
        USING quantidade_minima::NUMERIC(19,6);


-- Lotes
ALTER TABLE lote
    ALTER COLUMN quantidade_inicial TYPE NUMERIC(19,6)
        USING quantidade_inicial::NUMERIC(19,6),
    ALTER COLUMN quantidade_disponivel TYPE NUMERIC(19,6)
        USING quantidade_disponivel::NUMERIC(19,6),
    ALTER COLUMN conteudo_por_apresentacao TYPE NUMERIC(19,6)
        USING conteudo_por_apresentacao::NUMERIC(19,6);


-- Itens de pedido
ALTER TABLE itens_pedido
    ALTER COLUMN quantidade_solicitada TYPE NUMERIC(19,6)
        USING quantidade_solicitada::NUMERIC(19,6),
    ALTER COLUMN quantidade_aprovada TYPE NUMERIC(19,6)
        USING quantidade_aprovada::NUMERIC(19,6),
    ALTER COLUMN multiplicador_solicitado TYPE NUMERIC(19,6)
        USING multiplicador_solicitado::NUMERIC(19,6);


-- Movimentações de estoque
ALTER TABLE movimentacao_estoque
    ALTER COLUMN quantidade_movimentada TYPE NUMERIC(19,6)
        USING quantidade_movimentada::NUMERIC(19,6),
    ALTER COLUMN quantidade_anterior TYPE NUMERIC(19,6)
        USING quantidade_anterior::NUMERIC(19,6),
    ALTER COLUMN quantidade_atual TYPE NUMERIC(19,6)
        USING quantidade_atual::NUMERIC(19,6);


-- Histórico de materiais recebidos pelo laboratório
ALTER TABLE historico_laboratorio
    ALTER COLUMN quantidade TYPE NUMERIC(19,6)
        USING quantidade::NUMERIC(19,6);


-- ============================================================
-- Integridade das quantidades
-- ============================================================

ALTER TABLE estoque_central
    ADD CONSTRAINT ck_estoque_quantidade_atual_nao_negativa
        CHECK (quantidade_atual >= 0),
    ADD CONSTRAINT ck_estoque_quantidade_minima_nao_negativa
        CHECK (quantidade_minima >= 0);


ALTER TABLE lote
    ADD CONSTRAINT ck_lote_quantidade_inicial_positiva
        CHECK (quantidade_inicial > 0),
    ADD CONSTRAINT ck_lote_quantidade_disponivel_nao_negativa
        CHECK (quantidade_disponivel >= 0),
    ADD CONSTRAINT ck_lote_quantidade_disponivel_limite
        CHECK (quantidade_disponivel <= quantidade_inicial),
    ADD CONSTRAINT ck_lote_conteudo_apresentacao_positivo
        CHECK (
            conteudo_por_apresentacao IS NULL
            OR conteudo_por_apresentacao > 0
        );


ALTER TABLE itens_pedido
    ADD CONSTRAINT ck_itens_pedido_quantidade_solicitada_positiva
        CHECK (quantidade_solicitada > 0),
    ADD CONSTRAINT ck_itens_pedido_quantidade_aprovada_positiva
        CHECK (
            quantidade_aprovada IS NULL
            OR quantidade_aprovada > 0
        );


ALTER TABLE movimentacao_estoque
    ADD CONSTRAINT ck_movimentacao_quantidade_movimentada_positiva
        CHECK (quantidade_movimentada > 0),
    ADD CONSTRAINT ck_movimentacao_quantidade_anterior_nao_negativa
        CHECK (quantidade_anterior >= 0),
    ADD CONSTRAINT ck_movimentacao_quantidade_atual_nao_negativa
        CHECK (quantidade_atual >= 0);


ALTER TABLE historico_laboratorio
    ADD CONSTRAINT ck_historico_quantidade_positiva
        CHECK (quantidade > 0);