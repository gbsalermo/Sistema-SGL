# Documentação — SGL Backend

**Atualizado em:** 22/09/2026

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
Etapa 4 — expansão operacional de Resíduos   🔧 reconciliação + revalidação
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
| `../CONTINUIDADE.md` | checkpoint técnico e fase atual | **ATUAL — 22/09** |
| `SINCRONIZACAO_GITLAB_GITHUB.md` | fonte canônica para remotes, branches, Actions, push/pull e regras de sincronização | **ATUAL — 22/09** |
| `PLANO_PRE_PRODUCAO.md` | sequência canônica, dependências e regras | **ATUAL — 22/09** |
| `CONTINUIDADE_ETAPA_4_2026-09-17.md` | handoff operacional da Etapa 4 em reconciliação | **ATUAL — 22/09** |
| `CONTINUIDADE_ETAPA_3_2026-09-11.md` | fechamento detalhado da Etapa 3 | **FECHADO — 17/09** |
| `DOSSIE_PROJETO_SGL.md` | visão consolidada para handoff humano/IA | **ATUAL — 22/09** |
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
4. expansão de Resíduos                             🔧 reconciliação + revalidação
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

## Etapa 4 — próxima

```text
4.1 locais de armazenamento cadastráveis
→ 4.2 Modelos de Resíduo
→ 4.3 modelo x preenchimento manual pelo Solicitante
→ 4.4 correções administrativas do ciclo
```

Na 4.4 será avaliado cancelar/retornar Resíduo com justificativa e histórico. Isso não substitui a decisão geral de delete lógico da Etapa 11.

Detalhes: `CONTINUIDADE_ETAPA_4_2026-09-17.md`.

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
3. ler CONTINUIDADE_ETAPA_4_2026-09-17.md
4. confirmar que Etapa 3 foi integrada à main
5. conferir Swagger/OpenAPI
6. ler o documento específico da área
7. distinguir requisito atual de registro histórico
```

Não iniciar a Etapa 4 sobre uma branch antiga da Etapa 3. Criar branch própria a partir da `main` atualizada.
