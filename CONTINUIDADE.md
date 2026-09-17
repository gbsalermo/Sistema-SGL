# Continuidade do Projeto SGL — Backend

**Projeto:** Sistema de Gestão de Laboratórios  
**Backend:** `gbsalermo/Sistema-SGL`  
**Frontend:** `gbsalermo/SGL-FRONTEND`  
**Última atualização:** 17/09/2026  
**Branch estável:** `main`  
**Branch recém-concluída:** `feat/etapa-3-residuos`  
**Fase atual:** pré-produção pós-aprovação funcional  
**Etapa concluída:** Etapa 3 — refinamentos do fluxo atual de Resíduos ✅  
**Próxima etapa:** Etapa 4 — expansão operacional de Resíduos  
**Plano oficial:** `docs/PLANO_PRE_PRODUCAO.md`  
**Handoff da próxima etapa:** `docs/CONTINUIDADE_ETAPA_4_2026-09-17.md`

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
- não antecipar etapas futuras.

Ao iniciar a Etapa 4, confirmar primeiro que a Etapa 3 foi integrada à `main` nos dois repositórios e criar uma branch nova a partir dessa `main` atualizada.

Branch sugerida:

```text
feat/etapa-4-residuos
```

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
1. código da main
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

Regra obrigatória:

```text
migration aplicada = imutável
nova alteração de schema = V16+
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

Visualização e impressão são eventos distintos.

Template definitivo/Zebra continuam na Etapa 10.

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

Checkpoint: `docs/CONTINUIDADE_ETAPA_3_2026-09-11.md`.

---

# 9. Etapa 4 — próxima etapa

Handoff canônico:

`docs/CONTINUIDADE_ETAPA_4_2026-09-17.md`

Ordem prevista:

```text
4.1 Locais de armazenamento cadastráveis
→ 4.2 Modelos de Resíduos pré-cadastrados pela Gestão
→ 4.3 Uso de modelo ou preenchimento manual pelo Solicitante
→ 4.4 Correções administrativas do ciclo de vida
```

## 4.1 Local de armazenamento

Planejar catálogo reutilizável por Unidade, mantendo possibilidade de complemento/texto manual.

Antes de codar, decidir como preservar histórico caso o local seja renomeado futuramente.

## 4.2 ModeloResiduo

```text
ModeloResiduo = padrão reutilizável
Residuo       = ocorrência real
```

Modelo pode sugerir descrição, procedência, composição, classes, riscos, segurança, recipiente e outros dados reutilizáveis.

Alterar o modelo depois não pode modificar Resíduos históricos.

## 4.3 Solicitante

Na criação, permitir escolha entre modelo pré-cadastrado e preenchimento manual.

## 4.4 Correções administrativas

Necessidade levantada ao fechar a Etapa 3:

```text
ADMINISTRADOR
→ cancelar Resíduo com justificativa
→ ou retornar para análise/liberação quando permitido
→ preservar histórico
```

Antes de implementar, fechar regras de status, irreversibilidade de `DESPACHADO`, eventual `CANCELADO`, efeitos no rótulo e necessidade de nova liberação.

Não confundir com delete lógico. A decisão geral de delete lógico continua na Etapa 11.

---

# 10. Projetos, Estagiários e Relatórios — etapas futuras

## Etapa 5 — Projetos + Atividades

Antes de modelagem definitiva, confirmar:

- Código SEG;
- se Atividade é entidade subordinada ao Projeto;
- se `SCI` é tipo de Projeto ou domínio separado;
- situações de execução.

Projeto continua N:1 com Laboratório.

## Etapa 6 — Estagiários

Planejado:

- Orientador obrigatório;
- Projeto/Atividade;
- Bolsa/vínculo separado de Curso/Formação;
- Cultura/área temática;
- treinamento inicial de segurança;
- prorrogações justificadas e históricas.

## Etapa 7 — Relatórios consolidados

Depende das Etapas 5 e 6 estabilizadas. Inclui filtros/agregações, telas, PDF/XLSX e organização estrutural do módulo de relatórios.

---

# 11. Unidades, Soluções e Pedidos

## Etapa 8

Normalizar:

```text
unidade de medida
≠
apresentação física
```

Conversões compatíveis:

```text
1 L = 1000 mL
1 kg = 1000 g
```

Não converter massa ↔ volume genericamente sem densidade.

Depois estabilizar domínio de Soluções.

## Etapa 9

Integrar Soluções aos Pedidos sem redefinir a entidade Solução. Aprovação deve validar atomicamente todos os componentes.

---

# 12. Rótulos, Manual, Testes e Refactor

## Etapa 10 — Rótulos e impressão

- padrão-base SGL;
- rótulos adaptados de Produto/Resíduo/Solução;
- documento interno de auditoria de entrada de lote;
- Zebra/ZPL/testes físicos.

## Etapa 11 — Manual + delete lógico

- Manual do Usuário;
- avaliação de delete lógico entidade por entidade;
- não substituir ciclos de vida por `ativo` indiscriminadamente.

## Etapa 12 — testes frontend

```text
Vitest + Vue Test Utils
Cypress
```

## Etapa 13 — revisão estrutural e legibilidade

Revisar classes grandes, com atenção especial a `Residuo`, Services, DTOs e Controllers.

Não fazer refactor grande agora apenas para reduzir linhas. O refactor final deve ocorrer depois da suíte da Etapa 12 e reexecutar os testes.

---

# 13. Situação da pré-produção

```text
Etapa 1 — refinamento visual global                   ✅
Etapa 2 — Dark Mode definitivo                        ✅
Etapa 3 — refinamentos do fluxo atual de Resíduos     ✅ concluída e validada
Etapa 4 — expansão operacional de Resíduos            ⏭ próxima
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

**A Etapa 3 está encerrada e validada. A próxima janela deve confirmar que `feat/etapa-3-residuos` foi integrada à `main` nos dois repositórios e, somente depois, iniciar a Etapa 4 em branch própria criada a partir da `main` atualizada. Ler `docs/CONTINUIDADE_ETAPA_4_2026-09-17.md` antes de qualquer implementação. Começar pela modelagem da 4.1 — locais de armazenamento cadastráveis — e preservar a regra de que o usuário implementa manualmente o backend funcional.**
