-- Corporate users have no local email/password; identity comes from the corporate directory.
ALTER TABLE usuarios ALTER COLUMN senha DROP NOT NULL;
ALTER TABLE usuarios ALTER COLUMN email DROP NOT NULL;

ALTER TABLE usuarios ADD COLUMN login VARCHAR(255);
ALTER TABLE usuarios ADD COLUMN matricula VARCHAR(255);
ALTER TABLE usuarios ADD COLUMN cpf VARCHAR(255);
ALTER TABLE usuarios ADD COLUMN cargo VARCHAR(255);
ALTER TABLE usuarios ADD COLUMN sublotacao VARCHAR(255);
ALTER TABLE usuarios ADD COLUMN origem VARCHAR(255) NOT NULL DEFAULT 'DEV';
ALTER TABLE usuarios ADD COLUMN situacao VARCHAR(255) NOT NULL DEFAULT 'ATIVO';
ALTER TABLE usuarios ADD COLUMN ultimo_login_em TIMESTAMP WITH TIME ZONE;
ALTER TABLE usuarios ADD COLUMN sincronizado_em TIMESTAMP WITH TIME ZONE;

-- Partial unique indexes: local users keep these columns null.
CREATE UNIQUE INDEX uk_usuarios_login ON usuarios (login) WHERE login IS NOT NULL;
CREATE UNIQUE INDEX uk_usuarios_matricula ON usuarios (matricula) WHERE matricula IS NOT NULL;
CREATE UNIQUE INDEX uk_usuarios_cpf ON usuarios (cpf) WHERE cpf IS NOT NULL;

UPDATE usuarios SET origem = 'DEV', situacao = 'ATIVO';

ALTER TABLE unidades ADD COLUMN codigo_corporativo INTEGER UNIQUE;

-- sigla is UNIQUE since V1, so the upsert is safe.
INSERT INTO unidades (public_id, nome, sigla, codigo_corporativo)
VALUES (gen_random_uuid(), 'Centro Nacional de Pesquisa de Mandioca e Fruticultura Tropical', 'CNPMF', 7)
ON CONFLICT (sigla) DO UPDATE SET codigo_corporativo = 7;
