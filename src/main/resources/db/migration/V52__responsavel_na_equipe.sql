-- Every laboratory's responsável belongs to its team (fase 2): include current responsáveis who are
-- not yet in their own laboratory's list. Data only; the schema is unchanged.
-- A responsável from another Unidade is a data error already listed in "Cargos a substituir";
-- adding that laboratory to their list would break later edits of that person, so it is skipped.
INSERT INTO usuario_laboratorios (usuario_id, laboratorio_id)
SELECT l.responsavel_id, l.id
FROM laboratorios l
JOIN usuarios u ON u.id = l.responsavel_id AND u.unidade_id = l.unidade_id
WHERE NOT EXISTS (SELECT 1 FROM usuario_laboratorios ul
                  WHERE ul.usuario_id = l.responsavel_id AND ul.laboratorio_id = l.id);
