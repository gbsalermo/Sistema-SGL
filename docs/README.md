# Documentação — SGL Backend

**Atualizado em:** 18/09/2026

Este diretório reúne documentação vigente, decisões de domínio, material auxiliar e registros históricos. O objetivo deste índice é impedir que documentos antigos sejam interpretados como estado atual do projeto.

---

## Ordem de leitura para retomada

```text
1. ../CONTINUIDADE.md
2. PLANO_PRE_PRODUCAO.md
3. CONTINUIDADE_ETAPA_4_2026-09-17.md
4. ETAPA_4_2_MODELO_RESIDUO.md
4. MODULO_RESIDUOS.md
5. DOSSIE_PROJETO_SGL.md
6. FLUXO_DO_SISTEMA.md
7. Swagger/OpenAPI em execução
8. documento específico da área em trabalho
```

O arquivo `CONTINUIDADE_ETAPA_3_2026-09-11.md` permanece como registro histórico de fechamento da Etapa 3.

---

## Fonte de verdade

```text
código da main / branch atual validada
→ Swagger/OpenAPI
→ ../CONTINUIDADE.md
→ PLANO_PRE_PRODUCAO.md
→ handoff da etapa atual
→ DOSSIE_PROJETO_SGL.md
→ documentos específicos
→ documentos históricos
```

---

## Estado do projeto

```text
Primeiro protótipo funcional                 ✅ aprovado
Pré-produção pós-aprovação                   🔧 em andamento
Limpeza/revisão documental                   ✅ concluída
Planejamento de pré-produção                 ✅ consolidado
Etapa 1 — refinamento visual global          ✅ concluída
Etapa 2 — Dark Mode definitivo               ✅ concluída
Etapa 3 — refinamentos de Resíduos           ✅ concluída e validada
Etapa 4 — expansão operacional de Resíduos   🔧 em andamento
  4.1 — locais de armazenamento              ✅ concluída
  4.2 — Modelos de Resíduo                   🔧 atual
Etapas 5 a 13                                ⏳
Matriz formal de permissões                  ⏳ após pré-produção
Congelamento/homologação final               ⏳ posterior
Autenticação/autorização definitiva          ⏳ posterior
Integração corporativa                       ⏳ posterior
```

---

## Documentos vigentes

| Documento | Papel | Estado |
|---|---|---|
| `../CONTINUIDADE.md` | checkpoint técnico e fase atual | **ATUAL — Etapa 4.2** |
| `PLANO_PRE_PRODUCAO.md` | sequência canônica, dependências e regras | **ATUAL — Etapa 4.2** |
| `CONTINUIDADE_ETAPA_4_2026-09-17.md` | handoff operacional da etapa atual | **ATUAL — Etapa 4.2** |
| `ETAPA_4_2_MODELO_RESIDUO.md` | contrato/modelagem de `ModeloResiduo` | **ATUAL — 4.2-A EM REVISÃO** |
| `CONTINUIDADE_ETAPA_3_2026-09-11.md` | fechamento detalhado da Etapa 3 | **HISTÓRICO FECHADO** |
| `DOSSIE_PROJETO_SGL.md` | visão consolidada para handoff humano/IA | **ATUAL** |
| `MODULO_RESIDUOS.md` | domínio e fluxo de Resíduos | **ATUAL — 4.2** |
| `FLUXO_DO_SISTEMA.md` | fluxo operacional de domínio | **ATUAL — 4.2** |
| `ENDPOINTS_INTERNOS.md` | inventário técnico complementar de endpoints | **VIGENTE** |
| `JSON_EXEMPLOS.md` | payloads de apoio para Postman/frontend | **VIGENTE** |
| `testes.md` | suíte automatizada e roteiros de regressão | **VIGENTE** |
| `RELATORIOS.md` | cobertura de relatórios | **VIGENTE** |
| `EXPORTACAO_RELATORIOS.md` | regras de PDF/XLSX | **VIGENTE** |
| `PENDENCIAS_POS_PROTOTIPO.md` | refactors e pendências posteriores | **REFERÊNCIA VIGENTE** |
| `GUIA_ESTRUTURAL.md` | organização arquitetural | **REFERÊNCIA** |

---

## Etapa 4.1 — concluída ✅

A modelagem de locais de armazenamento foi aprovada antes da implementação:

```text
LocalArmazenamentoResiduo
= catálogo mutável por Unidade

Residuo.localArmazenamentoResiduo
= referência opcional ao catálogo

Residuo.complementoLocalArmazenamento
= complemento opcional da ocorrência

Residuo.localArmazenamentoTemporario
= snapshot textual histórico completo
```

Regras associadas:

- catálogo por Unidade;
- ativação/inativação;
- local inativo deixa de ser elegível para novas seleções sem quebrar histórico;
- seleção por catálogo + complemento opcional;
- caminho manual continua permitido;
- catálogo e texto manual não podem ser enviados simultaneamente de forma ambígua;
- renomear o cadastro não altera `localArmazenamentoTemporario` de Resíduos antigos;
- correção física posterior deve preservar rastreabilidade;
- rótulo/relatório continuam consumindo o snapshot textual existente.

Implementação concluída:

```text
4.1-A V16 + entidade + repository ✅
4.1-B CRUD + tenant ✅
4.1-C análise/liberação ✅
4.1-D confirmação física/correção ✅
4.1-E revisão backend ✅
4.1-F Administração/Cadastros frontend ✅
4.1-G Gestão frontend ✅
4.1-H regressão e fechamento ✅
```

Próxima implementação: **aprovar a 4.2-A e então iniciar 4.2-B — V17 + entidades + repositories**.

---

## Roadmap atual de pré-produção

```text
1. padronização visual                              ✅
2. Dark Mode                                        ✅
3. refinamentos do fluxo atual de Resíduos          ✅
4. expansão de Resíduos                             🔧 atual
5. Projetos + Atividades                            ⏳
6. Estagiários + vínculos                           ⏳
7. relatórios consolidados                          ⏳
8. normalização de unidades + Soluções              ⏳
9. Pedidos + integração com Soluções                ⏳
10. rótulos + documento de lote + impressão         ⏳
11. Manual do Usuário + delete lógico               ⏳
12. testes frontend — Vitest/Vue Test Utils/Cypress ⏳
13. revisão estrutural e legibilidade               ⏳
```

Regra deste bloco: alterações funcionais de backend são implementadas manualmente pelo responsável do projeto; IA pode analisar, orientar e revisar.

---

## Migrations de Resíduos

```text
V11 — módulo base de Resíduos
V12 — Código SGL
V13 — estado físico/tratamento/responsabilidade
V14 — Classes de Resíduo
V15 — segurança/EPI
V16 — locais de armazenamento de Resíduos
```

Migrations aplicadas são imutáveis. A V16 está aplicada; nova alteração de schema deve usar V17 ou superior.

---

## Decisões que precisam ser preservadas

```text
Long interno + UUID público
Produto != Resíduo
ModeloResiduo != Residuo
perecível → FEFO
não perecível → FIFO
aprovação baixa estoque
entrega não baixa novamente
cancelamento aprovado restaura lotes utilizados
migrations Flyway aplicadas são imutáveis
Unidade não possui CRUD manual normal
snapshot histórico não depende de cadastro mutável
```

### Isolamento por Unidade

```text
frontend
→ X-SGL-Unidade-Id
→ TenantRequestFilter / TenantContext
→ services/repositories por Unidade
```

É isolamento funcional em desenvolvimento; autenticação/autorização definitiva ainda virá no roadmap formal.

---

## Documentos auxiliares e históricos

Documentos auxiliares não prevalecem sobre Swagger/OpenAPI ou código atual. Documentos de etapas já encerradas permanecem para rastreabilidade e não devem ter suas afirmações temporais reescritas apenas porque o projeto avançou.

Se um documento histórico disser que um módulo já integrado “ainda será feito”, prevalece o checkpoint atual.

---

## Regra para outra IA

Antes de alterar o sistema:

```text
1. ler ../CONTINUIDADE.md
2. ler PLANO_PRE_PRODUCAO.md
3. ler CONTINUIDADE_ETAPA_4_2026-09-17.md
4. conferir a branch atual
5. conferir Swagger/OpenAPI
6. ler o documento específico da área
7. distinguir requisito atual de registro histórico
```

Na situação atual, revisar `ETAPA_4_2_MODELO_RESIDUO.md`. Não antecipar a 4.2-B antes da aprovação do contrato e não antecipar a experiência modelo x manual da 4.3.
