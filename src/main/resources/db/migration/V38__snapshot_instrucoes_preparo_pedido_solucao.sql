-- Congelar cartilha de preparo no momento da solicitacao, preservando auditoria historica.
ALTER TABLE pedidos ADD COLUMN instrucoes_preparo_solucao VARCHAR(4000);

-- Pedidos que ja possuem modelo recebendo snapshot inicial na migracao.
UPDATE pedidos p
SET instrucoes_preparo_solucao = (
  SELECT m.instrucoes_preparo FROM modelos_solucao m WHERE m.id = p.modelo_solucao_id
)
WHERE p.tipo = 'SOLUCAO' AND p.modelo_solucao_id IS NOT NULL AND p.instrucoes_preparo_solucao IS NULL;
