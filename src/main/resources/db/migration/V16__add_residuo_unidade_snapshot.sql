-- Correção de bug (rótulo de resíduo mudando de unidade sozinho):
--
-- O rótulo físico do resíduo (RotuloResiduoResponseDTO) mostrava a unidade
-- lendo o caminho residuo -> laboratorio -> unidade EM TEMPO REAL. Como um
-- laboratório pode ser transferido de unidade depois (LaboratorioService
-- permite alterar a unidade de um laboratório existente), um resíduo
-- registrado há meses passava a exibir a unidade NOVA do laboratório no
-- rótulo, sem nenhum registro no histórico explicando a mudança.
--
-- A correção é guardar uma "foto" (snapshot) da unidade no momento em que o
-- resíduo foi criado, em vez de derivar sempre do vínculo atual do
-- laboratório. Colunas nascem opcionais (NULL) porque resíduos já existentes
-- não têm esse dado; o UPDATE abaixo preenche o snapshot para eles com a
-- unidade atual do laboratório (é a melhor informação disponível nesse
-- momento) e, a partir daqui, todo resíduo novo já nasce com o snapshot
-- preenchido pelo backend (ver ResiduoService.criar()).

ALTER TABLE residuos
    ADD COLUMN unidade_id_snapshot UUID,
    ADD COLUMN unidade_nome_snapshot VARCHAR(255),
    ADD COLUMN unidade_sigla_snapshot VARCHAR(255);

UPDATE residuos r
SET unidade_id_snapshot = u.public_id,
    unidade_nome_snapshot = u.nome,
    unidade_sigla_snapshot = u.sigla
FROM laboratorios l
JOIN unidades u ON u.id = l.unidade_id
WHERE r.laboratorio_id = l.id
  AND r.unidade_id_snapshot IS NULL;
