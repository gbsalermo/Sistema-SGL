ALTER TABLE itens_pedido
    ADD COLUMN tipo_embalagem_solicitada VARCHAR(30),
    ADD COLUMN quantidade_embalagens_solicitada INTEGER,
    ADD COLUMN multiplicador_solicitado INTEGER;

UPDATE itens_pedido
SET tipo_embalagem_solicitada = 'UNITARIO',
    quantidade_embalagens_solicitada = quantidade_solicitada,
    multiplicador_solicitado = 1
WHERE tipo_embalagem_solicitada IS NULL;

ALTER TABLE itens_pedido
    ALTER COLUMN tipo_embalagem_solicitada SET NOT NULL,
    ALTER COLUMN quantidade_embalagens_solicitada SET NOT NULL,
    ALTER COLUMN multiplicador_solicitado SET NOT NULL;

ALTER TABLE itens_pedido
    ADD CONSTRAINT ck_itens_pedido_quantidade_embalagens_positiva
        CHECK (quantidade_embalagens_solicitada > 0),
    ADD CONSTRAINT ck_itens_pedido_multiplicador_positivo
        CHECK (multiplicador_solicitado > 0);
