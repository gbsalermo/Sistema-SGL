ALTER TABLE lote
    ADD COLUMN codigo_interno VARCHAR(160),
    ADD COLUMN sequencial_interno INTEGER;

WITH lotes_ordenados AS (
    SELECT
        l.id AS lote_id,
        ec.produto_id,
        ROW_NUMBER() OVER (
            PARTITION BY ec.produto_id
            ORDER BY l.id
        )::INTEGER AS sequencial,
        TRIM(BOTH '-' FROM UPPER(
            REGEXP_REPLACE(
                COALESCE(NULLIF(p.codigo_referencia, ''), 'PRD-' || p.id::TEXT),
                '[^A-Za-z0-9]+',
                '-',
                'g'
            )
        )) AS sigla
    FROM lote l
    JOIN estoque_central ec ON ec.id = l.estoque_central_id
    JOIN produtos p ON p.id = ec.produto_id
)
UPDATE lote l
SET
    sequencial_interno = lo.sequencial,
    codigo_interno = 'LOT-' || lo.sigla || '-' || LPAD(lo.sequencial::TEXT, 3, '0')
FROM lotes_ordenados lo
WHERE lo.lote_id = l.id;

ALTER TABLE lote
    ALTER COLUMN codigo_interno SET NOT NULL,
    ALTER COLUMN sequencial_interno SET NOT NULL;

ALTER TABLE lote
    ADD CONSTRAINT uk_lote_codigo_interno UNIQUE (codigo_interno);

CREATE INDEX idx_lote_sequencial_produto
    ON lote (estoque_central_id, sequencial_interno);
