CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE usuarios ADD COLUMN public_id UUID;
ALTER TABLE unidades ADD COLUMN public_id UUID;
ALTER TABLE	laboratorios ADD COLUMN public_id UUID;
ALTER TABLE produtos ADD COLUMN public_id UUID;
ALTER TABLE estoque_central ADD COLUMN public_id UUID;
ALTER TABLE lote ADD COLUMN public_id UUID;
ALTER TABLE projetos ADD COLUMN public_id UUID;
ALTER TABLE pedidos ADD COLUMN public_id UUID;
ALTER TABLE itens_pedido ADD COLUMN public_id UUID;
ALTER TABLE movimentacao_estoque ADD COLUMN public_id UUID;
ALTER TABLE historico_laboratorio ADD COLUMN public_id UUID;


UPDATE usuarios SET public_id = gen_random_uuid() WHERE public_id IS NULL;
UPDATE unidades SET public_id = gen_random_uuid() WHERE public_id IS NULL;
UPDATE laboratorios SET public_id = gen_random_uuid() WHERE public_id IS NULL;
UPDATE produtos SET public_id = gen_random_uuid() WHERE public_id IS NULL;
UPDATE estoque_central SET public_id = gen_random_uuid() WHERE public_id IS NULL;
UPDATE lote SET public_id = gen_random_uuid() WHERE public_id IS NULL;
UPDATE projetos SET public_id = gen_random_uuid() WHERE public_id IS NULL;
UPDATE pedidos SET public_id = gen_random_uuid() WHERE public_id IS NULL;
UPDATE itens_pedido SET public_id = gen_random_uuid() WHERE public_id IS NULL;
UPDATE movimentacao_estoque SET public_id = gen_random_uuid() WHERE public_id IS NULL;
UPDATE historico_laboratorio SET public_id = gen_random_uuid() WHERE public_id IS NULL;

ALTER TABLE usuarios ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE unidades ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE laboratorios ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE produtos ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE estoque_central ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE lote ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE projetos ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE pedidos ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE itens_pedido ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE movimentacao_estoque ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE historico_laboratorio ALTER COLUMN public_id SET NOT NULL;

ALTER TABLE usuarios ADD CONSTRAINT uk_usuarios_public_id UNIQUE (public_id);
ALTER TABLE unidades ADD CONSTRAINT uk_unidades_public_id UNIQUE (public_id);
ALTER TABLE laboratorios ADD CONSTRAINT uk_laboratorios_public_id UNIQUE (public_id);
ALTER TABLE produtos ADD CONSTRAINT uk_produtos_public_id UNIQUE (public_id);
ALTER TABLE estoque_central ADD CONSTRAINT uk_estoque_central_public_id UNIQUE (public_id);
ALTER TABLE lote ADD CONSTRAINT uk_lote_public_id UNIQUE (public_id);
ALTER TABLE projetos ADD CONSTRAINT uk_projetos_public_id UNIQUE (public_id);
ALTER TABLE pedidos ADD CONSTRAINT uk_pedidos_public_id UNIQUE (public_id);
ALTER TABLE itens_pedido ADD CONSTRAINT uk_itens_pedido_public_id UNIQUE (public_id);
ALTER TABLE movimentacao_estoque ADD CONSTRAINT uk_movimentacao_estoque_public_id UNIQUE (public_id);
ALTER TABLE historico_laboratorio ADD CONSTRAINT uk_historico_laboratorio_public_id UNIQUE (public_id);



