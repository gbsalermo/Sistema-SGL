ALTER TABLE projetos
    ADD CONSTRAINT uk_projetos_codigo_seg
        UNIQUE (codigo_seg);

ALTER TABLE scis
    ADD CONSTRAINT uk_scis_codigo_seg
        UNIQUE (codigo_seg);

ALTER TABLE atividades
    ADD CONSTRAINT uk_atividades_codigo_seg
        UNIQUE (codigo_seg);