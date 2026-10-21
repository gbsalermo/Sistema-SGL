CREATE TABLE vinculos_operacionais_usuario (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 public_id CHAR(36) NOT NULL UNIQUE,
 usuario_id BIGINT NOT NULL,
 laboratorio_id BIGINT NOT NULL,
 projeto_id BIGINT NULL,
 sci_id BIGINT NULL,
 atividade_id BIGINT NULL,
 data_inicio DATE NOT NULL,
 data_fim DATE NULL,
 observacao VARCHAR(500),
 CONSTRAINT fk_vinculo_oper_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
 CONSTRAINT fk_vinculo_oper_lab FOREIGN KEY (laboratorio_id) REFERENCES laboratorios(id),
 CONSTRAINT fk_vinculo_oper_projeto FOREIGN KEY (projeto_id) REFERENCES projetos(id),
 CONSTRAINT fk_vinculo_oper_sci FOREIGN KEY (sci_id) REFERENCES scis(id),
 CONSTRAINT fk_vinculo_oper_atividade FOREIGN KEY (atividade_id) REFERENCES atividades(id)
);
CREATE INDEX idx_vinculo_oper_usuario ON vinculos_operacionais_usuario(usuario_id, data_fim);
CREATE INDEX idx_vinculo_oper_laboratorio ON vinculos_operacionais_usuario(laboratorio_id);
