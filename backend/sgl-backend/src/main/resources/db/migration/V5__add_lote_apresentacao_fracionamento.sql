ALTER TABLE lote
    ADD COLUMN apresentacao VARCHAR(120),
    ADD COLUMN quantidade_apresentacoes INTEGER,
    ADD COLUMN conteudo_por_apresentacao INTEGER,
    ADD COLUMN fracionavel BOOLEAN;

-- Compatibilidade com lotes existentes: cada registro legado representa uma
-- apresentação unitária e continua fracionável até ser recadastrado/ajustado.
UPDATE lote
SET apresentacao = 'LEGADO',
    quantidade_apresentacoes = quantidade_inicial,
    conteudo_por_apresentacao = 1,
    fracionavel = TRUE
WHERE apresentacao IS NULL;
