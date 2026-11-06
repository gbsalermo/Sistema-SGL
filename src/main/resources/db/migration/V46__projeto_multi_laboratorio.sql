ALTER TABLE projetos ADD COLUMN unidade_id BIGINT;
UPDATE projetos p SET unidade_id = l.unidade_id FROM laboratorios l WHERE l.id = p.laboratorio_id;
ALTER TABLE projetos ALTER COLUMN unidade_id SET NOT NULL;
ALTER TABLE projetos ADD CONSTRAINT fk_projeto_unidade FOREIGN KEY (unidade_id) REFERENCES unidades(id);
CREATE INDEX idx_projetos_unidade_id ON projetos(unidade_id);

CREATE TABLE projeto_laboratorios (
    projeto_id BIGINT NOT NULL REFERENCES projetos(id),
    laboratorio_id BIGINT NOT NULL REFERENCES laboratorios(id),
    CONSTRAINT pk_projeto_laboratorios PRIMARY KEY (projeto_id, laboratorio_id)
);
CREATE INDEX idx_projeto_laboratorios_laboratorio_id ON projeto_laboratorios(laboratorio_id);
INSERT INTO projeto_laboratorios (projeto_id, laboratorio_id) SELECT id, laboratorio_id FROM projetos;

ALTER TABLE projetos DROP CONSTRAINT fk_projeto_laboratorio;
ALTER TABLE projetos DROP COLUMN laboratorio_id;
