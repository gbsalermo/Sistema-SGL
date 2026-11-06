#!/usr/bin/env bash
# Rehearses V47 (usuario_laboratorios) on a disposable PostgreSQL 16: migrates to V46,
# inserts synthetic rows for each case, applies V47 and asserts the result with SQL.
# The project has no Testcontainers; this replaces V47MigrationTest. Never touches a real database.
# Usage: bash scripts/verificar-v47.sh   (requires docker; exits non-zero on any failed case)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MIGRATIONS="$ROOT/src/main/resources/db/migration"
CONTAINER="sgl-verificar-v47-$$"
PG_IMAGE="postgres:16-alpine"
FLYWAY_IMAGE="flyway/flyway:latest"

cleanup() { docker rm -f "$CONTAINER" >/dev/null 2>&1 || true; }
trap cleanup EXIT

psql_exec() { docker exec -i "$CONTAINER" psql -v ON_ERROR_STOP=1 -q -U postgres -d sgl "$@"; }
flyway() {
    docker run --rm --network "container:$CONTAINER" -v "$MIGRATIONS:/flyway/sql:ro" "$FLYWAY_IMAGE" \
        -url=jdbc:postgresql://localhost:5432/sgl -user=postgres -password=postgres "$@"
}

echo "== Subindo $PG_IMAGE descartável ($CONTAINER)"
docker run -d --name "$CONTAINER" -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=sgl "$PG_IMAGE" >/dev/null
for _ in $(seq 60); do
    docker exec "$CONTAINER" pg_isready -U postgres -d sgl -q 2>/dev/null && break
    sleep 1
done
# pg_isready can answer during the init restart; wait for a real query.
for _ in $(seq 30); do psql_exec -c "SELECT 1" >/dev/null 2>&1 && break; sleep 1; done

echo "== Flyway até V46"
flyway -target=46 migrate | tail -1

echo "== Linhas sintéticas"
psql_exec <<'SQL'
-- Unidade 9001 is the people's unit; 9002 is another unit.
INSERT INTO unidades (id, public_id, nome, sigla) VALUES
 (9001, gen_random_uuid(), 'Unidade V47', 'UV47'),
 (9002, gen_random_uuid(), 'Outra Unidade V47', 'OV47');

INSERT INTO laboratorios (id, public_id, unidade_id, nome, ativo) VALUES
 (9101, gen_random_uuid(), 9001, 'L1 base', TRUE),
 (9102, gen_random_uuid(), 9001, 'L2 projeto ativo', TRUE),
 (9103, gen_random_uuid(), 9001, 'L3 inativo', FALSE),
 (9104, gen_random_uuid(), 9002, 'L4 outra unidade', TRUE),
 (9105, gen_random_uuid(), 9001, 'L5 vinculo generico', TRUE),
 (9106, gen_random_uuid(), 9001, 'L6 vinculo restrito', TRUE),
 (9107, gen_random_uuid(), 9001, 'L7 vinculo com prazo', TRUE),
 (9108, gen_random_uuid(), 9001, 'L8 participacao encerrada', TRUE),
 (9109, gen_random_uuid(), 9001, 'L9 estagio finalizado', TRUE),
 (9110, gen_random_uuid(), 9001, 'L10 participacao com fim futuro', TRUE);

INSERT INTO usuarios (id, public_id, nome, perfil, unidade_id, laboratorio_id, ativo) VALUES
 (9201, gen_random_uuid(), 'Analista com base', 'ANALISTA', 9001, 9101, TRUE),
 (9202, gen_random_uuid(), 'Gestor sem base', 'GESTOR', 9001, NULL, TRUE),
 (9203, gen_random_uuid(), 'Tecnico com vinculos', 'TECNICO', 9001, NULL, TRUE),
 (9301, gen_random_uuid(), 'Estagiario V47', 'ESTAGIARIO', 9001, 9101, TRUE);

INSERT INTO estagiarios (id, data_inicio_estagio, tipo_bolsa, situacao_estagio)
VALUES (9301, CURRENT_DATE - 200, 'BOLSA_CNPQ', 'EM_ANDAMENTO');

-- P1: L2 (copied), L3 inactive and L4 of another unit (not copied). P2..P4 one lab each.
INSERT INTO projetos (id, public_id, unidade_id, nome, ativo) VALUES
 (9401, gen_random_uuid(), 9001, 'P1 ativo', TRUE),
 (9402, gen_random_uuid(), 9001, 'P2 participacao encerrada', TRUE),
 (9403, gen_random_uuid(), 9001, 'P3 estagio finalizado', TRUE),
 (9404, gen_random_uuid(), 9001, 'P4 fim futuro', TRUE);
INSERT INTO projeto_laboratorios (projeto_id, laboratorio_id) VALUES
 (9401, 9102), (9401, 9103), (9401, 9104), (9402, 9108), (9403, 9109), (9404, 9110);

INSERT INTO scis (id, public_id, projeto_id, codigo_seg, nome, data_inicio) VALUES
 (9501, gen_random_uuid(), 9401, 'V47.S1', 'S1', CURRENT_DATE - 300),
 (9502, gen_random_uuid(), 9402, 'V47.S2', 'S2', CURRENT_DATE - 300),
 (9503, gen_random_uuid(), 9403, 'V47.S3', 'S3', CURRENT_DATE - 300),
 (9504, gen_random_uuid(), 9404, 'V47.S4', 'S4', CURRENT_DATE - 300);
INSERT INTO atividades (id, public_id, sci_id, codigo_seg, nome, data_inicio) VALUES
 (9601, gen_random_uuid(), 9501, 'V47.A1', 'A1', CURRENT_DATE - 300),
 (9602, gen_random_uuid(), 9502, 'V47.A2', 'A2', CURRENT_DATE - 300),
 (9603, gen_random_uuid(), 9503, 'V47.A3', 'A3', CURRENT_DATE - 300),
 (9604, gen_random_uuid(), 9504, 'V47.A4', 'A4', CURRENT_DATE - 300);

-- VE1 in progress; VE2 finished.
INSERT INTO vinculos_estagio (id, estagiario_id, data_inicio, data_fim_prevista, data_fim_efetiva, tipo_bolsa, situacao) VALUES
 (9701, 9301, CURRENT_DATE - 100, CURRENT_DATE + 100, NULL, 'BOLSA_CNPQ', 'EM_ANDAMENTO'),
 (9702, 9301, CURRENT_DATE - 200, CURRENT_DATE - 101, CURRENT_DATE - 101, 'BOLSA_CNPQ', 'FINALIZADO');

INSERT INTO vinculo_estagio_atividade (id, vinculo_estagio_id, atividade_id, data_inicio_participacao, data_fim_participacao) VALUES
 (9801, 9701, 9601, CURRENT_DATE - 60, NULL),               -- active -> L2
 (9802, 9701, 9602, CURRENT_DATE - 60, CURRENT_DATE - 1),   -- ended  -> not L8
 (9803, 9702, 9603, CURRENT_DATE - 190, NULL),              -- finished link -> not L9
 (9804, 9701, 9604, CURRENT_DATE - 60, CURRENT_DATE + 10);  -- ends in the future -> L10

INSERT INTO vinculos_operacionais_usuario
 (id, public_id, usuario_id, laboratorio_id, projeto_id, sci_id, atividade_id, data_inicio, data_fim, encerrado, observacao) VALUES
 (9901, gen_random_uuid(), 9203, 9105, NULL, NULL, NULL, CURRENT_DATE - 30, NULL, FALSE, 'Obs antiga'),           -- generic -> converted
 (9902, gen_random_uuid(), 9201, 9101, NULL, NULL, NULL, CURRENT_DATE - 30, NULL, FALSE, NULL),                   -- generic, same as base -> no duplicate
 (9903, gen_random_uuid(), 9203, 9107, NULL, NULL, NULL, CURRENT_DATE - 30, CURRENT_DATE + 30, FALSE, 'Prazo'),  -- has end date -> kept
 (9904, gen_random_uuid(), 9203, 9106, 9401, NULL, NULL, CURRENT_DATE - 30, NULL, FALSE, 'Restrito'),           -- restricted -> kept
 (9905, gen_random_uuid(), 9203, 9109, NULL, NULL, NULL, CURRENT_DATE - 30, NULL, TRUE, 'Ja encerrado');        -- closed -> ignored
SQL

echo "== Flyway V47"
flyway migrate | tail -1

echo "== Casos"
psql_exec <<'SQL'
\set QUIET on
CREATE FUNCTION pg_temp.labs(uid BIGINT) RETURNS BIGINT[] AS $$
    SELECT COALESCE(array_agg(laboratorio_id ORDER BY laboratorio_id), '{}')
    FROM usuario_laboratorios WHERE usuario_id = uid
$$ LANGUAGE sql;

DO $$ BEGIN
    ASSERT pg_temp.labs(9201) = ARRAY[9101]::BIGINT[], 'base lab must become a one-item list: ' || pg_temp.labs(9201)::TEXT;
    ASSERT pg_temp.labs(9202) = '{}'::BIGINT[], 'user without base lab must have an empty list';
    RAISE NOTICE 'PASS v47CopiaLaboratorioBase';
END $$;

DO $$ BEGIN
    ASSERT pg_temp.labs(9301) = ARRAY[9101, 9102, 9110]::BIGINT[],
        'intern must have base + active labs of active participations, got ' || pg_temp.labs(9301)::TEXT;
    RAISE NOTICE 'PASS v47CopiaLaboratorioDasParticipacoesAtivasDoEstagiario (ended participation, finished link, inactive lab and other-unit lab not copied)';
END $$;

DO $$
DECLARE v RECORD;
BEGIN
    ASSERT pg_temp.labs(9203) = ARRAY[9105]::BIGINT[], 'generic vínculo lab must be in the list, got ' || pg_temp.labs(9203)::TEXT;
    SELECT * INTO v FROM vinculos_operacionais_usuario WHERE id = 9901;
    ASSERT v.encerrado, 'generic vínculo must be closed';
    ASSERT v.observacao = 'Obs antiga Convertido em laboratório da pessoa (V47).', 'observacao: ' || v.observacao;
    SELECT * INTO v FROM vinculos_operacionais_usuario WHERE id = 9902;
    ASSERT v.encerrado AND v.observacao = 'Convertido em laboratório da pessoa (V47).', 'generic vínculo on base lab must be closed';
    ASSERT (SELECT count(*) FROM usuario_laboratorios WHERE usuario_id = 9201) = 1, 'no duplicate for base lab';
    RAISE NOTICE 'PASS v47ConverteVinculoGenericoEEncerra';
END $$;

DO $$ BEGIN
    ASSERT NOT (SELECT encerrado FROM vinculos_operacionais_usuario WHERE id = 9903), 'vínculo with end date must stay open';
    ASSERT NOT (SELECT encerrado FROM vinculos_operacionais_usuario WHERE id = 9904), 'restricted vínculo must stay open';
    ASSERT (SELECT observacao FROM vinculos_operacionais_usuario WHERE id = 9903) = 'Prazo', 'observacao of kept vínculo unchanged';
    ASSERT (SELECT observacao FROM vinculos_operacionais_usuario WHERE id = 9905) = 'Ja encerrado', 'already closed vínculo untouched';
    ASSERT NOT (9107 = ANY (pg_temp.labs(9203)) OR 9106 = ANY (pg_temp.labs(9203)) OR 9109 = ANY (pg_temp.labs(9203))),
        'kept/closed vínculos must not add labs';
    RAISE NOTICE 'PASS v47MantemVinculoComPrazoOuRestrito';
END $$;

DO $$ BEGIN
    ASSERT NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_name = 'usuarios' AND column_name = 'laboratorio_id'), 'usuarios.laboratorio_id must be gone';
    ASSERT NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                       WHERE table_name = 'usuarios' AND constraint_name = 'fk_usuario_laboratorio'), 'FK must be gone';
    ASSERT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_usuario_laboratorios_laboratorio_id'), 'index on laboratorio_id';
    RAISE NOTICE 'PASS v47RemoveColunaLaboratorioId';
END $$;
SQL

echo "== V47 verificada: todos os casos passaram"
