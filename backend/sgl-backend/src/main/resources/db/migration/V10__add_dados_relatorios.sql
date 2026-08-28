ALTER TABLE pedidos
    ADD COLUMN data_entrega TIMESTAMP;

ALTER TABLE produtos
    ADD COLUMN fiscalizado BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE produtos
    ADD COLUMN observacao_fiscalizacao VARCHAR(500);

CREATE TABLE produto_orgaos_fiscalizadores (
    produto_id BIGINT NOT NULL,
    orgao VARCHAR(50) NOT NULL,
    PRIMARY KEY (produto_id, orgao),
    CONSTRAINT fk_produto_orgaos_fiscalizadores_produto
        FOREIGN KEY (produto_id) REFERENCES produtos(id)
);
