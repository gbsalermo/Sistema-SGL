ALTER TABLE projetos
    ADD COLUMN codigo_seg VARCHAR(18),
    ADD COLUMN status VARCHAR(64) NOT NULL DEFAULT 'ATIVO',
    ADD COLUMN situacao_execucao VARCHAR(64) NOT NULL DEFAULT 'NAO_INFORMADO',
    ADD COLUMN possui_recurso_externo BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN empresa_recurso_externo VARCHAR(255);


ALTER TABLE projetos
    ADD CONSTRAINT ck_projetos_codigo_seg_nao_vazio
        CHECK (
            codigo_seg IS NULL
            OR TRIM(codigo_seg) <> ''
        ),

    ADD CONSTRAINT ck_projetos_status
        CHECK (
            status IN (
                'ATIVO',
                'ENCERRADO_COM_AVALIACAO_PENDENTE',
                'CONCLUIDO'
            )
        ),

    ADD CONSTRAINT ck_projetos_situacao_execucao
        CHECK (
            situacao_execucao IN (
                'NAO_INFORMADO',
                'EM_ANDAMENTO_NO_PRAZO',
                'EM_ANDAMENTO_ATRASADO',
                'EXECUCAO_CANCELADA'
            )
        ),

    ADD CONSTRAINT ck_projetos_recurso_externo
        CHECK (
            (
                possui_recurso_externo = FALSE
                AND empresa_recurso_externo IS NULL
            )
            OR
            (
                possui_recurso_externo = TRUE
                AND empresa_recurso_externo IS NOT NULL
                AND TRIM(empresa_recurso_externo) <> ''
            )
        );


CREATE INDEX idx_projetos_codigo_seg
    ON projetos(codigo_seg);