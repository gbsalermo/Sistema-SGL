# Continuidade do Projeto SGL — Backend

**Projeto:** Sistema de Gestão de Laboratórios  
**Backend:** `gbsalermo/Sistema-SGL`  
**Frontend:** `gbsalermo/SGL-FRONTEND`  
**Última atualização:** 18/09/2026  
**Branch estável:** `main`  
**Branch atual de trabalho:** `feat/etapa-4-residuos`  
**Fase atual:** pré-produção pós-aprovação funcional  
**Etapa concluída em código:** Etapa 4 — expansão operacional de Resíduos ✅  
**Validação pendente:** regressão integrada/manual da Etapa 4  
**Próxima etapa após validação:** Etapa 5 — Projetos e Atividades  
**Plano oficial:** `docs/PLANO_PRE_PRODUCAO.md`  
**Handoff da etapa atual:** `docs/CONTINUIDADE_ETAPA_4_2026-09-17.md`

Este arquivo é o checkpoint principal de retomada. Para detalhes do módulo de Resíduos, usar `docs/MODULO_RESIDUOS.md`. Para contratos HTTP, confirmar sempre no Swagger/OpenAPI em execução.

**Sincronização documental:** atualizada em 18/09/2026 após a implementação das 4.2, 4.3 e 4.4. O roteiro de homologação está em `docs/VALIDACAO_ETAPA_4.md`.

---

# 0. Regra de trabalho

```text
branch própria
→ implementação/revisão
→ validação
→ refinamento
→ Pull Request
→ main
→ atualizar documentação
```

Regra especial do projeto:

- alterações funcionais de backend são implementadas **manualmente pelo responsável do projeto**;
- a IA deve analisar, modelar, explicar, fornecer código de referência e revisar;
- não aplicar diretamente código funcional de backend sem autorização explícita;
- frontend/documentação podem ser alterados diretamente quando autorizado;
- trabalhar em passos pequenos e commits lógicos;
- a autorização excepcional para finalizar a Etapa 4 permitiu implementação direta da 4.2-D em diante; novas etapas voltam a seguir o fluxo normal de confirmação.

A Etapa 3 já foi integrada à `main` nos dois repositórios e a branch `feat/etapa-4-residuos` já foi criada a partir da `main` atualizada.

---

# 1. Estado consolidado

## Backend

```text
Spring Boot / PostgreSQL / Flyway                     ✅
Long interno + UUID público                           ✅
DTOs request/response                                 ✅
Tratamento global de erros                            ✅
Concorrência de aprovação                             ✅
FIFO / FEFO                                           ✅
Lotes / rastreabilidade                               ✅
Embalagens / multiplicador / fracionamento            ✅
Pedidos e urgência                                    ✅
Swagger / OpenAPI                                     ✅
Movimentações                                         ✅
Relatórios consolidados                               ✅ base atual
Produtos fiscalizados                                 ✅
PDF / XLSX                                            ✅
Resíduos — fluxo atual refinado                       ✅ Etapa 3 concluída
Classes de Resíduo + snapshots                        ✅
Segurança/EPI + snapshots                             ✅
Estagiários — base atual                              ✅ evolução na Etapa 6
Pessoas por laboratório                               ✅
Administração / Cadastros                             ✅
Isolamento operacional por Unidade                    ✅
Autenticação/autorização/auditoria definitiva         ⏳ roadmap formal
Integração corporativa                                ⏳ roadmap formal
```

## Frontend integrado

```text
Login visual / sessão DEV                             ✅
Expiração automática da sessão DEV em 5h              ✅
Pedidos do solicitante                                ✅
Pedidos da gestão                                     ✅
Estoque e lotes                                       ✅
Movimentações                                         ✅
Resíduos — solicitante e gestão                       ✅ Etapa 3 concluída
Classes de Resíduo em Cadastros                       ✅
Segurança/EPI de Produto/Resíduo                      ✅
Prévia antecipada do rótulo                           ✅
Impressão bloqueada até liberação                     ✅
Estagiários                                           ✅ base atual
Relatórios + PDF/XLSX                                 ✅
Pessoas por laboratório                               ✅
Administração / Cadastros                             ✅
Dashboard Gestão                                      ✅
Dashboard Solicitante                                 ✅
Alertas operacionais                                  ✅
Busca global                                          ✅
Dark Mode definitivo                                  ✅
Página 404                                            ✅
Contexto de Unidade enviado à API                     ✅
Testes automatizados frontend                         ⏳ Etapa 12
Autenticação/autorização definitiva                   ⏳ roadmap formal
```

---

# 2. Ordem de precedência

Quando houver conflito entre documentos:

```text
1. código da main / branch atual validada
2. Swagger/OpenAPI
3. CONTINUIDADE.md do repositório em trabalho
4. docs/PLANO_PRE_PRODUCAO.md
5. handoff da etapa atual
6. docs/DOSSIE_PROJETO_SGL.md
7. documentos específicos de módulo
8. documentos históricos
```

Documentos históricos podem permanecer para rastreabilidade, mas não devem comandar a tarefa atual quando houver checkpoint mais recente.

---

# 3. Arquitetura e identificadores

```text
Controller = contrato HTTP
Service = regra/transação/orquestração
Repository = persistência
Model = estado de domínio
RequestDTO = entrada
ResponseDTO = saída
```

Identificadores:

```text
Long id
→ banco, JPA, FKs e locks

UUID publicId
→ endpoints, DTOs e frontend
```

Não introduzir ID numérico em contratos públicos sem necessidade explícita.

---

# 4. PostgreSQL e Flyway

Ambiente de desenvolvimento padrão:

```text
PostgreSQL
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

O Hibernate valida; o Flyway evolui o schema.

Migrations relevantes já aplicadas no domínio de Resíduos:

```text
V11 — módulo de Resíduos
V12 — backfill Código SGL
V13 — estado físico, tratamento e responsabilidade inicial
V14 — Classes de Resíduo
V15 — segurança/EPI
```

Migration da 4.1 aplicada:

```text
V16 — locais de armazenamento de Resíduos
```

A V16 está aplicada e é imutável. Qualquer ajuste futuro de schema deve usar V17 ou superior.

Regra obrigatória:

```text
migration aplicada = imutável
nova alteração de schema = nova migration
```

---

# 5. Multitenancy por Unidade

Backend:

```text
X-SGL-Unidade-Id
→ TenantRequestFilter
→ TenantContext
→ services/repositories restringem dados
→ contexto limpo ao final
```

Frontend:

```text
sessão DEV contém unidadeId
→ interceptor envia X-SGL-Unidade-Id
```

Interpretação correta:

```text
isolamento funcional por Unidade              ✅
segurança definitiva por identidade            ❌ ainda não
```

A autenticação futura deve derivar Unidade/tenant da identidade autenticada confiável.

---

# 6. Estoque, lotes e Pedidos

```text
Produto = catálogo
EstoqueCentral = saldo consolidado por produto/Unidade
Lote = validade + saldo + apresentação + rastreabilidade
MovimentacaoEstoque = trilha de operações físicas
```

Seleção:

```text
perecível     → FEFO
não perecível → FIFO
```

Pedidos:

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

Regras:

- criação não baixa estoque;
- aprovação executa a baixa;
- entrega não baixa novamente;
- cancelamento aprovado restaura os lotes efetivamente usados;
- urgência não altera FIFO/FEFO.

Unidades/apresentações serão refinadas na Etapa 8 antes de Soluções.

---

# 7. Resíduos — estado após a Etapa 3

Decisão central:

```text
Produto != Resíduo
```

Componente de Resíduo pode referenciar Produto para rastreabilidade e sugestão de segurança sem movimentar estoque.

Fluxo validado:

```text
INFORMADO
→ EM_ANALISE
→ LIBERADO_PARA_ARMAZENAMENTO
→ ARMAZENADO_TEMPORARIAMENTE
→ DESPACHADO
```

Código SGL:

```text
SGL-RES-AAAA-NNNNNN
```

O código e o QR técnico existem desde o registro inicial.

## Dados incorporados na Etapa 3

- Procedência/uso preservada em `processoOrigem`;
- estado físico;
- tratamento realizado + descrição;
- `gestorRecebedorInicial`;
- Classes de Resíduo informadas/confirmadas;
- snapshots de classes;
- Segurança/EPI informada/confirmada;
- recomendações de segurança em Produto;
- snapshots de segurança;
- comparação visual informado x aprovado;
- identificação do Gestor que liberou pelo histórico.

## Rótulo

```text
INFORMADO / EM_ANALISE
→ prévia disponível
→ impressão bloqueada

LIBERADO_PARA_ARMAZENAMENTO ou posterior
→ impressão liberada
```

Visualização e impressão são eventos distintos. O QR técnico faz parte da identificação/contrato; o template visual físico atual do frontend não precisa renderizá-lo. Template definitivo/Zebra continuam na Etapa 10.

## Responsabilidade

```text
usuarioGerador
→ quem informou

gestorRecebedorInicial
→ quem recebeu inicialmente e conduziu a conferência

HistoricoResiduo
→ ator real de cada transição
```

Armazenamento e despacho podem ser executados por outro Gestor sem perder o histórico anterior. Isso foi validado manualmente.

Detalhes: `docs/MODULO_RESIDUOS.md`.

---

# 8. Etapa 3 — fechamento

Fechada e validada em **17/09/2026**.

```text
3.1 redundância de análise                            ✅
3.2.1 estado físico/tratamento/responsabilidade       ✅
3.2.2 Classes de Resíduo                              ✅
3.2.3 Segurança/EPI + snapshot                        ✅
3.2.4 integração frontend                             ✅
3.3 identificação/prévia/permissão de impressão       ✅
```

A validação final cobriu criação, análise, armazenamento, despacho, Gestores diferentes, histórico, prévia, bloqueio/liberação de impressão, comparação informado/aprovado e legibilidade da tela.

Checkpoint histórico: `docs/CONTINUIDADE_ETAPA_3_2026-09-11.md`.

---

# 9. Etapa 4 — implementação concluída ✅

A Etapa 4 está implementada na branch `feat/etapa-4-residuos`. Falta apenas a validação integrada/manual descrita em `docs/VALIDACAO_ETAPA_4.md` antes de integrar à `main`.

Ordem concluída:

```text
4.1 Locais de armazenamento cadastráveis          ✅
4.2 Modelos de Resíduos pré-cadastrados            ✅
4.3 Uso de modelo ou preenchimento manual          ✅
4.4 Correções administrativas do ciclo de vida     ✅
```

## 4.1 Locais de armazenamento

`LocalArmazenamentoResiduo` permanece como catálogo mutável por Unidade, enquanto `Residuo.localArmazenamentoTemporario` preserva o snapshot textual histórico. A Gestão pode planejar e corrigir fisicamente o local sem perder rastreabilidade.

## 4.2 ModeloResiduo

Implementado:

```text
V17 + ModeloResiduo + ComponenteModeloResiduo
CRUD por Unidade
validações de classes/produtos/segurança
inativação lógica
Administração frontend
testes backend de regras centrais
```

Regra preservada:

```text
ModeloResiduo = definição reutilizável/editável
Residuo       = ocorrência real independente
```

Não existe FK `Residuo -> ModeloResiduo`; alterações futuras no modelo não reescrevem ocorrências históricas.

## 4.3 Uso pelo Solicitante

`/residuos/novo` oferece:

```text
modelo pré-cadastrado
ou
preenchimento manual
```

O modelo apenas preenche sugestões. Quantidade, projeto e demais dados da ocorrência continuam sob revisão do usuário, e o envio usa o fluxo normal de criação de `Residuo`.

## 4.4 Correções administrativas

Foi adicionado o status `CANCELADO` e a ação administrativa com justificativa obrigatória.

Retorno permitido de exatamente uma etapa:

```text
EM_ANALISE                   → INFORMADO
LIBERADO_PARA_ARMAZENAMENTO → EM_ANALISE
ARMAZENADO_TEMPORARIAMENTE  → LIBERADO_PARA_ARMAZENAMENTO
DESPACHADO                   → ARMAZENADO_TEMPORARIAMENTE
```

Cancelamento:

- permitido para Resíduos ainda não despachados;
- `DESPACHADO` precisa retornar uma etapa antes de cancelar;
- `CANCELADO` não retorna de etapa;
- ator, data e justificativa ficam no histórico;
- relatórios e filtros reconhecem cancelados;
- cancelados deixam de contar como ativos.

Isso não substitui a avaliação geral de delete lógico da Etapa 11.

**Próximo passo:** executar `docs/VALIDACAO_ETAPA_4.md`. Passando a regressão, integrar a branch e iniciar a Etapa 5.

---

# 10. Projetos, Estagiários e Relatórios — etapas futuras

## Etapa 5 — Projetos + Atividades

Antes de modelagem definitiva, confirmar Código SEG, relação de Atividade, significado de SCI e situações de execução.

## Etapa 6 — Estagiários

Planejado: Orientador obrigatório, Projeto/Atividade, Bolsa separada de Curso/Formação, Cultura/área temática, treinamento inicial de segurança e prorrogações justificadas/históricas.

## Etapa 7 — Relatórios consolidados

Depende das Etapas 5 e 6 estabilizadas.

---

# 11. Unidades, Soluções e Pedidos

## Etapa 8

Normalizar:

```text
unidade de medida
≠
apresentação física

1 L = 1000 mL
1 kg = 1000 g
```

Não converter massa ↔ volume genericamente sem densidade.

## Etapa 9

Integrar Soluções aos Pedidos com validação atômica dos componentes.

---

# 12. Rótulos, Manual, Testes e Refactor

```text
Etapa 10 — Rótulos + Zebra/ZPL + documento de lote
Etapa 11 — Manual + avaliação de delete lógico
Etapa 12 — Vitest + Vue Test Utils + Cypress
Etapa 13 — revisão estrutural e legibilidade
```

Não fazer refactor grande agora apenas para reduzir linhas. `Residuo` e classes grandes serão revisitados na Etapa 13 após a suíte da Etapa 12.

---

# 13. Situação da pré-produção

```text
Etapa 1 — refinamento visual global                   ✅
Etapa 2 — Dark Mode definitivo                        ✅
Etapa 3 — refinamentos do fluxo atual de Resíduos     ✅ concluída e validada
Etapa 4 — expansão operacional de Resíduos            ✅ implementada; validação manual pendente
  4.1 — locais de armazenamento                       ✅
  4.2 — Modelos de Resíduo                            ✅
  4.3 — modelo ou preenchimento manual                ✅
  4.4 — correções administrativas                     ✅
Etapa 5 — Projetos + Atividades                       ⏳
Etapa 6 — Estagiários + vínculos                      ⏳
Etapa 7 — relatórios consolidados                     ⏳
Etapa 8 — unidades + Soluções                         ⏳
Etapa 9 — Pedidos + Soluções                          ⏳
Etapa 10 — Rótulos + impressão operacional            ⏳
Etapa 11 — Manual + decisão delete lógico             ⏳
Etapa 12 — testes automatizados frontend              ⏳
Etapa 13 — revisão estrutural e legibilidade           ⏳
```

Matriz de permissões, congelamento funcional e autenticação definitiva continuam posteriores ao bloco atual.

---

# 14. Regra final de retomada

**A Etapa 4 segue na branch `feat/etapa-4-residuos`. A 4.1 foi concluída e validada. O próximo passo é a 4.2 — ModeloResiduo.**
