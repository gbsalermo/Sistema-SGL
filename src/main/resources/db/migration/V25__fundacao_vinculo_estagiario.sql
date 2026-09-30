ALTER TABLE estagiarios
    ADD COLUMN situacao_estagio VARCHAR(30);

ALTER TABLE estagiarios
    ADD COLUMN orientador_id BIGINT;

UPDATE estagiarios e
SET situacao_estagio =
    CASE
        WHEN EXISTS (
            SELECT 1
            FROM usuarios u
            WHERE u.id = e.id
              AND u.ativo = TRUE
        )
        THEN 'EM_ANDAMENTO'
        ELSE 'FINALIZADO'
    END;

ALTER TABLE estagiarios
    ALTER COLUMN situacao_estagio SET NOT NULL;

ALTER TABLE estagiarios
    ADD CONSTRAINT fk_estagiarios_orientador
        FOREIGN KEY (orientador_id)
        REFERENCES usuarios(id);

CREATE INDEX idx_estagiarios_orientador_id
    ON estagiarios(orientador_id);