ALTER TABLE pedidos
    ADD COLUMN participacao_estagio_atividade_id BIGINT,
    ADD COLUMN atividade_origem_id BIGINT,
    ADD COLUMN sci_origem_id BIGINT,
    ADD COLUMN atividade_origem_nome VARCHAR(255),
    ADD COLUMN sci_origem_nome VARCHAR(255),
    ADD COLUMN projeto_origem_nome VARCHAR(255),
    ADD COLUMN laboratorio_origem_nome VARCHAR(255);

ALTER TABLE pedidos
    ADD CONSTRAINT fk_pedidos_participacao_origem
    FOREIGN KEY (participacao_estagio_atividade_id)
    REFERENCES vinculo_estagio_atividade(id);

ALTER TABLE pedidos
    ADD CONSTRAINT fk_pedidos_atividade_origem
    FOREIGN KEY (atividade_origem_id)
    REFERENCES atividades(id);

ALTER TABLE pedidos
    ADD CONSTRAINT fk_pedidos_sci_origem
    FOREIGN KEY (sci_origem_id)
    REFERENCES scis(id);

CREATE INDEX idx_pedidos_participacao_origem
    ON pedidos(participacao_estagio_atividade_id);