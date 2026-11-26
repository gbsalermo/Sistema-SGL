-- Splits the single usuarios.perfil into accumulable system roles and a job category.
-- V49/V50 are taken by the produtos + bolsas integration branch, hence V51.
-- usuarios.perfil is kept (no longer mapped) so a rollback can restore the previous release.

CREATE TABLE usuario_papeis (
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    papel VARCHAR(40) NOT NULL,
    CONSTRAINT pk_usuario_papeis PRIMARY KEY (usuario_id, papel),
    CONSTRAINT ck_usuario_papeis_papel CHECK (papel IN ('ADMINISTRADOR_GERAL', 'ADMINISTRADOR_LOCAL', 'GESTOR'))
);

INSERT INTO usuario_papeis (usuario_id, papel)
SELECT id, CASE perfil WHEN 'ADMINISTRADOR' THEN 'ADMINISTRADOR_LOCAL' ELSE perfil END
FROM usuarios
WHERE perfil IN ('ADMINISTRADOR_GERAL', 'ADMINISTRADOR', 'GESTOR');

ALTER TABLE usuarios ADD COLUMN categoria_cargo VARCHAR(20);
ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_categoria_cargo
    CHECK (categoria_cargo IN ('TECNICO', 'ANALISTA', 'PESQUISADOR', 'ESTAGIARIO'));

-- Management users get their category from the corporate cargo on their next login.
UPDATE usuarios SET categoria_cargo = perfil
WHERE perfil IN ('TECNICO', 'ANALISTA', 'PESQUISADOR', 'ESTAGIARIO');

ALTER TABLE usuarios ALTER COLUMN perfil DROP NOT NULL;
