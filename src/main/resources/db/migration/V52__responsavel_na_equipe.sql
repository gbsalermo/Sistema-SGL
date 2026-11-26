-- Every laboratory's responsável belongs to its team (fase 2): include current responsáveis who are
-- not yet in their own laboratory's list. Data only; the schema is unchanged.
INSERT INTO usuario_laboratorios (usuario_id, laboratorio_id)
SELECT l.responsavel_id, l.id
FROM laboratorios l
WHERE l.responsavel_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM usuario_laboratorios ul
                  WHERE ul.usuario_id = l.responsavel_id AND ul.laboratorio_id = l.id);
