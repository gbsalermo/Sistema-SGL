ALTER TABLE lote
    ADD COLUMN tipo_embalagem VARCHAR(30);

UPDATE lote
SET tipo_embalagem = CASE
    WHEN LOWER(COALESCE(apresentacao, '')) LIKE '%kit%' THEN 'KIT'
    WHEN LOWER(COALESCE(apresentacao, '')) LIKE '%caixa%' THEN 'CAIXA'
    WHEN LOWER(COALESCE(apresentacao, '')) LIKE '%garrafa%' THEN 'GARRAFA'
    WHEN LOWER(COALESCE(apresentacao, '')) LIKE '%galão%'
      OR LOWER(COALESCE(apresentacao, '')) LIKE '%galao%' THEN 'GALAO'
    ELSE 'UNITARIO'
END;

ALTER TABLE lote
    ALTER COLUMN tipo_embalagem SET NOT NULL;
