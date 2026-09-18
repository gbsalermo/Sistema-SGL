CREATE TABLE produto_medidas_seguranca (
    produto_id BIGINT NOT NULL,
    medida VARCHAR(100) NOT NULL,

    CONSTRAINT pk_produto_medidas_seguranca
        PRIMARY KEY (produto_id, medida),

    CONSTRAINT fk_produto_medidas_seguranca
        FOREIGN KEY (produto_id)
        REFERENCES produtos(id)
        ON DELETE CASCADE
);


ALTER TABLE produtos
    ADD COLUMN observacao_seguranca VARCHAR(1000);


CREATE TABLE residuo_medidas_seguranca_informadas (
    residuo_id BIGINT NOT NULL,
    medida VARCHAR(100) NOT NULL,

    CONSTRAINT pk_residuo_medidas_seguranca_informadas
        PRIMARY KEY (residuo_id, medida),

    CONSTRAINT fk_residuo_medidas_seguranca_informadas
        FOREIGN KEY (residuo_id)
        REFERENCES residuos(id)
        ON DELETE CASCADE
);


CREATE TABLE residuo_medidas_seguranca_confirmadas (
    residuo_id BIGINT NOT NULL,
    medida VARCHAR(100) NOT NULL,

    CONSTRAINT pk_residuo_medidas_seguranca_confirmadas
        PRIMARY KEY (residuo_id, medida),

    CONSTRAINT fk_residuo_medidas_seguranca_confirmadas
        FOREIGN KEY (residuo_id)
        REFERENCES residuos(id)
        ON DELETE CASCADE
);


ALTER TABLE residuos
    ADD COLUMN observacao_seguranca_informada VARCHAR(1000);

ALTER TABLE residuos
    ADD COLUMN observacao_seguranca_confirmada VARCHAR(1000);