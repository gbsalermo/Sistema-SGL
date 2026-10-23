ALTER TABLE residuos
    ADD COLUMN participacao_estagio_atividade_id BIGINT,
    ADD COLUMN atividade_origem_nome VARCHAR(255),
    ADD COLUMN sci_origem_nome VARCHAR(255),
    ADD COLUMN projeto_origem_nome VARCHAR(255),
    ADD COLUMN laboratorio_origem_nome VARCHAR(255);

ALTER TABLE residuos
    ADD CONSTRAINT fk_residuos_participacao_origem
    FOREIGN KEY (participacao_estagio_atividade_id)
    REFERENCES vinculo_estagio_atividade(id);

CREATE INDEX idx_residuos_participacao_origem
    ON residuos(participacao_estagio_atividade_id);
