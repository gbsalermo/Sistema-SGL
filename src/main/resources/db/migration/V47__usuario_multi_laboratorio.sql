-- A person may work in several laboratories of their Unidade: the single
-- usuarios.laboratorio_id ("laboratório-base") becomes the usuario_laboratorios list.

CREATE TABLE usuario_laboratorios (
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    laboratorio_id BIGINT NOT NULL REFERENCES laboratorios(id),
    CONSTRAINT pk_usuario_laboratorios PRIMARY KEY (usuario_id, laboratorio_id)
);
CREATE INDEX idx_usuario_laboratorios_laboratorio_id ON usuario_laboratorios(laboratorio_id);

-- 1. The former laboratório-base becomes the first item of the list.
INSERT INTO usuario_laboratorios (usuario_id, laboratorio_id)
SELECT id, laboratorio_id FROM usuarios WHERE laboratorio_id IS NOT NULL;

-- 2. Interns: pedidos/resíduos are about to require the laboratory to be in the
-- intern's list, so the active laboratories of the projects they currently take part
-- in are added; otherwise requests that work today would start failing.
-- "In progress" link = same rule as ElegibilidadeContextoOperacional (not FINALIZADO,
-- no data_fim_efetiva). Only laboratories of the intern's own Unidade, so the list
-- keeps its invariant.
INSERT INTO usuario_laboratorios (usuario_id, laboratorio_id)
SELECT DISTINCT ve.estagiario_id, pl.laboratorio_id
FROM vinculo_estagio_atividade vea
JOIN vinculos_estagio ve ON ve.id = vea.vinculo_estagio_id
JOIN atividades a ON a.id = vea.atividade_id
JOIN scis s ON s.id = a.sci_id
JOIN projeto_laboratorios pl ON pl.projeto_id = s.projeto_id
JOIN laboratorios l ON l.id = pl.laboratorio_id
JOIN usuarios u ON u.id = ve.estagiario_id
WHERE (vea.data_fim_participacao IS NULL OR vea.data_fim_participacao >= CURRENT_DATE)
  AND ve.situacao <> 'FINALIZADO'
  AND ve.data_fim_efetiva IS NULL
  AND l.ativo = TRUE
  AND l.unidade_id = u.unidade_id
ON CONFLICT DO NOTHING;

-- 3. An open-ended generic vínculo operacional (no project/SCI/activity, no end date)
-- is exactly what the list now expresses: move the laboratory into the list and close
-- the vínculo, keeping it for history.
INSERT INTO usuario_laboratorios (usuario_id, laboratorio_id)
SELECT DISTINCT usuario_id, laboratorio_id
FROM vinculos_operacionais_usuario
WHERE projeto_id IS NULL AND sci_id IS NULL AND atividade_id IS NULL
  AND data_fim IS NULL AND encerrado = FALSE
ON CONFLICT DO NOTHING;

UPDATE vinculos_operacionais_usuario
SET encerrado = TRUE,
    observacao = CASE
        WHEN observacao IS NULL OR observacao = '' THEN 'Convertido em laboratório da pessoa (V47).'
        -- observacao is VARCHAR(500): trim the old text, never the conversion note.
        ELSE LEFT(observacao, 500 - LENGTH(' Convertido em laboratório da pessoa (V47).'))
             || ' Convertido em laboratório da pessoa (V47).'
    END
WHERE projeto_id IS NULL AND sci_id IS NULL AND atividade_id IS NULL
  AND data_fim IS NULL AND encerrado = FALSE;

-- 4. The single column goes away in the same migration, as in V46.
ALTER TABLE usuarios DROP CONSTRAINT fk_usuario_laboratorio;
ALTER TABLE usuarios DROP COLUMN laboratorio_id;
