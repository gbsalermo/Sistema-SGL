UPDATE residuos
SET codigo_rastreio = CONCAT(
    'SGL-RES-',
    CAST(EXTRACT(YEAR FROM data_informacao) AS INTEGER),
    '-',
    LPAD(CAST(id AS VARCHAR), 6, '0')
)
WHERE codigo_rastreio IS NULL;
