# Continuidade do Projeto SGL — Backend

**Projeto:** Sistema de Gestão de Laboratórios  
**Backend:** `gbsalermo/Sistema-SGL`  
**Frontend:** `gbsalermo/SGL-FRONTEND`  
**Última atualização:** 18/09/2026  
**Branch estável:** `main`  
**Branch atual de trabalho:** `feat/etapa-4-residuos`  
**Fase atual:** pré-produção pós-aprovação funcional  
**Etapa concluída:** Etapa 3 — refinamentos do fluxo atual de Resíduos ✅  
**Etapa atual:** Etapa 4 — expansão operacional de Resíduos 🔧  
**Subetapa atual:** 4.1 — locais de armazenamento cadastráveis  
**Próxima implementação:** 4.1-H — regressão integrada e fechamento  
**Plano oficial:** `docs/PLANO_PRE_PRODUCAO.md`  
**Handoff da etapa atual:** `docs/CONTINUIDADE_ETAPA_4_2026-09-17.md`

Este arquivo é o checkpoint principal de retomada. Para detalhes do módulo de Resíduos, usar `docs/MODULO_RESIDUOS.md`. Para contratos HTTP, confirmar sempre no Swagger/OpenAPI em execução.

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
- não antecipar 4.2, 4.3 ou 4.4 durante a 4.1.

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

# 9. Etapa 4 — em andamento

Handoff canônico:

`docs/CONTINUIDADE_ETAPA_4_2026-09-17.md`

Ordem:

```text
4.1 Locais de armazenamento cadastráveis          🔧 atual
→ 4.2 Modelos de Resíduos pré-cadastrados          ⏳
→ 4.3 Uso de modelo ou preenchimento manual        ⏳
→ 4.4 Correções administrativas do ciclo de vida   ⏳
```

## 4.1 — decisão arquitetural fechada

```text
LocalArmazenamentoResiduo
= catálogo atual/editável por Unidade

Residuo.localArmazenamentoResiduo
= referência opcional ao catálogo

Residuo.complementoLocalArmazenamento
= complemento opcional da ocorrência

Residuo.localArmazenamentoTemporario
= snapshot textual histórico completo
```

Regras aprovadas:

- local cadastrado pertence a uma Unidade;
- catálogo possui ativação/inativação;
- inativo não aparece para novas seleções, mas não quebra Resíduos antigos;
- caminho por catálogo e caminho manual coexistem;
- payload ambíguo com catálogo + texto manual deve ser rejeitado;
- nome do catálogo pode mudar no futuro sem alterar o snapshot histórico do Resíduo;
- análise define o local planejado e confirmação física pode manter ou corrigir;
- `localArmazenamentoTemporario` continua atendendo rótulo/relatório como snapshot textual;
- novos vínculos devem respeitar a Unidade do Resíduo;
- não transformar a 4.1 em refactor amplo de `Residuo`.

Plano de implementação:

```text
4.1-A fundação backend: V16 + entidade + repository ✅
4.1-B CRUD + tenant ✅
4.1-C integração com análise/liberação ✅
4.1-D confirmação física/correção estruturada ✅
4.1-E revisão e fechamento do contrato backend ✅
4.1-F frontend Administração/Cadastros ✅
4.1-G frontend Gestão ✅
4.1-H regressão integrada e fechamento da 4.1 ⏭
```

**Próximo passo real:** 4.1-H. Backend e frontend da 4.1 estão implementados; falta build, regressão integrada e validação visual/end-to-end antes do fechamento.

## 4.2 ModeloResiduo

```text
ModeloResiduo = padrão reutilizável
Residuo       = ocorrência real
```

Modelo pode sugerir descrição, procedência, composição, classes, riscos, segurança, recipiente e outros dados reutilizáveis. Alterar o modelo depois não pode modificar Resíduos históricos.

## 4.3 Solicitante

Na criação, permitir escolha entre modelo pré-cadastrado e preenchimento manual.

## 4.4 Correções administrativas

Avaliar para Administrador cancelamento/retorno para análise com justificativa e histórico. Antes de implementar, fechar regras de status, irreversibilidade de `DESPACHADO`, eventual `CANCELADO`, efeitos no rótulo e necessidade de nova liberação.

Não confundir com delete lógico. A decisão geral de delete lógico continua na Etapa 11.

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
Etapa 4 — expansão operacional de Resíduos            🔧 em andamento
  4.1 — locais de armazenamento                       🔧 atual
  4.1-A–E — backend                                  ✅ concluído
  4.1-F/G — frontend                                ✅ implementado
  4.1-H — regressão integrada                        ⏭ próxima validação
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

**A Etapa 4 segue na branch `feat/etapa-4-residuos`. Backend 4.1-A–E e frontend 4.1-F/G estão implementados; o próximo passo é a 4.1-H — regressão integrada e fechamento.**
