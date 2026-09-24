# Documentação — SGL Backend

**Atualizado em:** 24/09/2026

Este diretório reúne documentação vigente, decisões de domínio, material auxiliar e registros históricos. O objetivo deste índice é impedir que documentos antigos sejam interpretados como estado atual do projeto.

---

## Ordem de leitura para retomada

```text
1. ../CONTINUIDADE.md
2. SINCRONIZACAO_GITLAB_GITHUB.md
3. PLANO_PRE_PRODUCAO.md
4. CONTINUIDADE_ETAPA_4_2026-09-17.md
5. MODULO_RESIDUOS.md
6. DOSSIE_PROJETO_SGL.md
7. Swagger/OpenAPI em execução
8. documento específico da área em trabalho
```

O arquivo `CONTINUIDADE_ETAPA_3_2026-09-11.md` permanece como registro de fechamento da Etapa 3.

---

## Fonte de verdade

```text
código da main
→ Swagger/OpenAPI
→ ../CONTINUIDADE.md
→ SINCRONIZACAO_GITLAB_GITHUB.md
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
Etapa 4 — expansão operacional de Resíduos   ✅ concluída e validada
Etapa 5 — Projetos e Atividades              🔧 atual — 5.1 Projeto base
Etapas 6 a 13                                ⏳
Matriz formal de permissões                  ⏳ após pré-produção
Congelamento/homologação final               ⏳ posterior
Autenticação/autorização definitiva          ⏳ posterior
Integração corporativa                       ⏳ posterior
```

---

## Documentos vigentes

| Documento | Papel | Estado |
|---|---|---|
| `../CONTINUIDADE.md` | checkpoint técnico e fase atual | **ATUAL — 24/09** |
| `SINCRONIZACAO_GITLAB_GITHUB.md` | fonte canônica para remotes, branches, Actions, push/pull e regras de sincronização | **ATUAL — 24/09** |
| `PLANO_PRE_PRODUCAO.md` | sequência canônica, dependências e regras | **ATUAL — 24/09** |
| `CONTINUIDADE_ETAPA_4_2026-09-17.md` | fechamento e decisões da Etapa 4 | **FECHADO — 24/09** |
| `VALIDACAO_ETAPA_4.md` | bateria executada e critérios de fechamento da Etapa 4 | **FECHADO — 24/09** |
| `CONTINUIDADE_ETAPA_3_2026-09-11.md` | fechamento detalhado da Etapa 3 | **FECHADO — 17/09** |
| `DOSSIE_PROJETO_SGL.md` | visão consolidada para handoff humano/IA | **ATUAL — 24/09** |
| `MODULO_RESIDUOS.md` | domínio e fluxo de Resíduos | **ATUAL — 17/09** |
| `FLUXO_DO_SISTEMA.md` | fluxo operacional de domínio | **ATUAL — 17/09** |
| `RELATORIOS.md` | cobertura de relatórios | **VIGENTE** |
| `EXPORTACAO_RELATORIOS.md` | regras de PDF/XLSX | **VIGENTE** |
| `PENDENCIAS_POS_PROTOTIPO.md` | refactors e pendências posteriores | **REFERÊNCIA VIGENTE** |
| `GUIA_ESTRUTURAL.md` | organização arquitetural | **REFERÊNCIA** |

---

## Roadmap atual de pré-produção

```text
1. padronização visual                              ✅
2. Dark Mode                                        ✅
3. refinamentos do fluxo atual de Resíduos          ✅
4. expansão de Resíduos                             ✅ concluída e validada
5. Projetos + Atividades                            🔧 atual — 5.1
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

## Etapa 3 — estado fechado

Consolidado:

- estado físico e tratamento;
- responsabilidade inicial da Gestão;
- Classes de Resíduo por Unidade;
- snapshots de classes;
- Segurança/EPI e recomendações em Produto;
- snapshots de segurança;
- integração frontend;
- visão informado x aprovado;
- identificação do Gestor que liberou;
- Código SGL/QR desde criação;
- prévia antes da liberação;
- impressão bloqueada até liberação;
- validação de armazenamento/despacho por Gestores diferentes;
- revisão de escala visual do formulário.

Detalhes: `CONTINUIDADE_ETAPA_3_2026-09-11.md`.

---

## Etapa 4 — fechada

```text
4.1 locais de armazenamento cadastráveis             ✅
→ 4.2 Modelos de Resíduo                             ✅
→ 4.3 modelo x preenchimento manual pelo Solicitante ✅
→ 4.4 correções administrativas do ciclo             ✅
```

A Etapa 4 foi reconciliada sobre a base corrigida pelo supervisor, testada e validada funcionalmente. Detalhes: `CONTINUIDADE_ETAPA_4_2026-09-17.md`.

A etapa atual é **Etapa 5 — Projetos e Atividades**. O **5.0 — Portão de confirmação está fechado** e o bloco atual é **5.1 — Projeto base**.

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

## Contratos e material auxiliar

Documentos auxiliares não prevalecem sobre Swagger/OpenAPI ou código atual:

| Documento | Uso correto |
|---|---|
| `ENDPOINTS_INTERNOS.md` | inventário auxiliar de endpoints |
| `JSON_EXEMPLOS.md` | exemplos de payload; conferir Swagger |
| `REQUISICOES_POSTMAN_LOTES.md` | testes de lotes |
| `CODIGOS_REFERENCIA_TESTES.md` | testes de códigos/referências |
| `testes.md` | histórico e cenários de validação |

---

## Documentos históricos

Arquivos de auditoria, demonstrações e snapshots antigos são mantidos para rastreabilidade e não representam planejamento vigente.

Se um documento histórico disser que um módulo já integrado “ainda será feito”, prevalece o checkpoint atual.

---

## Regra para outra IA

Antes de alterar o sistema:

```text
1. ler ../CONTINUIDADE.md
2. ler PLANO_PRE_PRODUCAO.md
3. usar CONTINUIDADE_ETAPA_4_2026-09-17.md apenas como fechamento da Etapa 4
4. confirmar o estado da main canônica no GitLab
5. conferir Swagger/OpenAPI
6. ler CONTINUIDADE_ETAPA_5_2026-09-24.md
7. continuar pelo 5.1 — Projeto base
7. distinguir requisito atual de registro histórico
```

A branch de trabalho da Etapa 5 é `collab/etapa-5-projetos-atividades`, criada a partir da `main` contendo a Etapa 4. Não iniciar alterações funcionais fora dela.
