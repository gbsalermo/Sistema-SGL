-- Preserva descrições técnicas completas (composição, pH, aplicação e embalagem).
ALTER TABLE produtos ALTER COLUMN descricao TYPE TEXT;

-- A Unidade de cadastro distingue itens recém-criados daqueles já vinculados a estoque.
-- Nullable para compatibilidade com produtos históricos, sem origem institucional identificável.
ALTER TABLE produtos ADD COLUMN unidade_cadastro_id BIGINT;
ALTER TABLE produtos
    ADD CONSTRAINT fk_produtos_unidade_cadastro
    FOREIGN KEY (unidade_cadastro_id) REFERENCES unidades(id);

CREATE INDEX idx_produtos_unidade_cadastro ON produtos(unidade_cadastro_id);

-- Inferência segura apenas quando existe uma única Unidade institucional.
-- Com múltiplas Unidades, produtos legados sem estoque requerem conciliação manual.
UPDATE produtos p
   SET unidade_cadastro_id = (SELECT MIN(u.id) FROM unidades u)
 WHERE p.unidade_cadastro_id IS NULL
   AND (SELECT COUNT(*) FROM unidades) = 1
   AND NOT EXISTS (
       SELECT 1 FROM estoque_central ec WHERE ec.produto_id = p.id
   );
