-- Employees may order/report in any active laboratory of their Unidade (decision 6);
-- the flag lets management see the record came from outside the requester's laboratories.
ALTER TABLE pedidos ADD COLUMN laboratorio_fora_do_solicitante BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE residuos ADD COLUMN laboratorio_fora_do_solicitante BOOLEAN NOT NULL DEFAULT FALSE;
