ALTER TABLE estoque_central
    ADD COLUMN minimo_tipo_embalagem VARCHAR(30),
    ADD COLUMN minimo_multiplicador NUMERIC(19,6);

ALTER TABLE estoque_central
    ADD CONSTRAINT ck_estoque_minimo_apresentacao
    CHECK ((minimo_tipo_embalagem IS NULL AND minimo_multiplicador IS NULL)
        OR (minimo_tipo_embalagem IS NOT NULL AND minimo_multiplicador > 0));
