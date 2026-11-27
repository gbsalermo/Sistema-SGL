-- Reverts V51 (papéis acumuláveis) so the previous release (perfil único) can run again.
-- Run it ONLY together with the rollback of the API to a release without V51, after a backup.
-- usuarios.perfil stopped being written after V51, so it is rebuilt from the roles and job category,
-- with the same rule as Perfil.legadoDe: the highest role wins, otherwise the category (TECNICO if none).
-- Accumulated roles collapse into one: e.g. GESTOR + ADMINISTRADOR_LOCAL becomes ADMINISTRADOR.

BEGIN;

UPDATE usuarios u SET perfil = CASE
    WHEN EXISTS (SELECT 1 FROM usuario_papeis p WHERE p.usuario_id = u.id AND p.papel = 'ADMINISTRADOR_GERAL') THEN 'ADMINISTRADOR_GERAL'
    WHEN EXISTS (SELECT 1 FROM usuario_papeis p WHERE p.usuario_id = u.id AND p.papel = 'ADMINISTRADOR_LOCAL') THEN 'ADMINISTRADOR'
    WHEN EXISTS (SELECT 1 FROM usuario_papeis p WHERE p.usuario_id = u.id AND p.papel = 'GESTOR') THEN 'GESTOR'
    ELSE COALESCE(u.categoria_cargo, 'TECNICO')
END;

ALTER TABLE usuarios ALTER COLUMN perfil SET NOT NULL;
ALTER TABLE usuarios DROP CONSTRAINT ck_usuarios_categoria_cargo;
ALTER TABLE usuarios DROP COLUMN categoria_cargo;
DROP TABLE usuario_papeis;

-- The previous release does not ship V51; Flyway validation would refuse an applied migration it cannot find.
DELETE FROM flyway_schema_history WHERE version = '51';

COMMIT;
