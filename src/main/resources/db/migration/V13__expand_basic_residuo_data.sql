ALTER TABLE residuos
    RENAME COLUMN gestor_responsavel_id TO gestor_recebedor_inicial_id;

ALTER TABLE residuos
    RENAME CONSTRAINT fk_residuo_gestor TO fk_residuo_gestor_recebedor_inicial;


ALTER TABLE residuos
    ADD COLUMN estado_fisico VARCHAR(255);

ALTER TABLE residuos
    ADD COLUMN tratamento_realizado BOOLEAN;

ALTER TABLE residuos
    ADD COLUMN descricao_tratamento VARCHAR(1000);


UPDATE residuos r
SET gestor_recebedor_inicial_id = recebimento.usuario_id
FROM (
    SELECT DISTINCT ON (residuo_id)
        residuo_id,
        usuario_id
    FROM historico_residuo
    WHERE acao = 'RECEBIDO_PELA_GESTAO'
    ORDER BY residuo_id, data_hora ASC, id ASC
) recebimento
WHERE recebimento.residuo_id = r.id;