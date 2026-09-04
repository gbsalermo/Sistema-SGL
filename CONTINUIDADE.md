# Continuidade do Projeto SGL — Backend

**Projeto:** Sistema de Gestão de Laboratórios  
**Backend:** `gbsalermo/Sistema-SGL`  
**Frontend:** `gbsalermo/SGL-FRONTEND`  
**Última atualização:** 03/09/2026  
**Branch estável:** `main`  
**Fase atual:** primeiro protótipo funcional próximo do congelamento/homologação.  
**Próximo bloco oficial:** consolidar diretrizes/matriz de permissões → congelar protótipo → homologação completa.  
**Handoff completo:** `docs/DOSSIE_PROJETO_SGL.md`

Este arquivo é o checkpoint de retomada. Para contratos HTTP, usar sempre o Swagger/OpenAPI em execução.

---

# 0. Regra de trabalho

```text
branch própria
→ implementação
→ validação
→ refinamento
→ Pull Request
→ main
→ atualizar documentação afetada
```

Não reabrir etapas concluídas sem necessidade concreta e não reorganizar o roadmap silenciosamente.

---

# 1. Estado geral em 03/09/2026

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
Relatórios consolidados                               ✅
Produtos fiscalizados                                 ✅
PDF / XLSX                                            ✅
Resíduos — fluxo operacional                          ✅
Relatório + exportação de Resíduos                    ✅
Estagiários — CRUD de vínculo + encerramento           ✅
Pessoas por laboratório + exportação                  ✅
Suporte a Administração/Cadastros                     ✅
Alteração administrativa de perfil                    ✅
Código SGL de Resíduo desde o registro inicial        ✅
Autenticação/autorização/auditoria definitiva         ⏳
Integração corporativa                                ⏳
Refactor técnico para inglês                          ⏳ pós-protótipo
```

## Frontend integrado à main

```text
Login visual / sessão DEV                             ✅
Expiração automática da sessão DEV em 5h              ✅
Pedidos do solicitante                                ✅
Pedidos da gestão                                     ✅
Estoque e lotes                                       ✅
Movimentações                                         ✅
Resíduos — solicitante e gestão                       ✅
Rótulos de Produto e Resíduo                          ✅
Estagiários                                           ✅
Relatórios + PDF/XLSX                                 ✅
Pessoas por laboratório                               ✅
Administração / Cadastros                             ✅
Dashboard Gestão                                      ✅
Dashboard Solicitante                                 ✅
Alertas operacionais                                  ✅
Busca global                                          ✅
Tema claro/escuro com persistência                    ✅
Página 404 animada                                    ✅
Autenticação/autorização definitiva                   ⏳
```

O login atual continua sendo sessão de desenvolvimento. A existência de guardas de rota e validações pontuais por perfil não equivale à autorização segura de produção.

---

# 2. Identificadores e arquitetura

```text
Long id
→ banco, JPA, FKs, locks

UUID publicId
→ DTOs, endpoints, frontend
```

Fluxo padrão:

```text
Controller recebe UUID
→ Service resolve por publicId
→ domínio usa Long internamente
```

```text
Controller = contrato HTTP
Service = regra/transação/orquestração
Repository = persistência
Model = estado e regras da entidade
RequestDTO = entrada
ResponseDTO = saída
```

---

# 3. Estoque, lotes e pedidos — invariantes

```text
Produto = catálogo
EstoqueCentral = saldo consolidado
Lote = validade + saldo + embalagem + rastreabilidade
MovimentacaoEstoque = trilha das operações físicas
```

Saída:

```text
perecível     → FEFO
não perecível → FIFO
```

Regras consolidadas:

```text
EstoqueCentral.quantidadeAtual = soma operacional dos lotes
aprovação baixa estoque
entrega NÃO baixa novamente
cancelamento aprovado restaura os lotes exatos
lote vencido não participa da aprovação
movimentação registra o lote efetivamente utilizado
```

Formas de retirada:

```text
UNITARIO
KIT
CAIXA
GARRAFA
GALAO
```

Fracionamento:

```text
false → true  permitido
true  → false não permitido
```

---

# 4. Pedidos

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

```text
aprovação → baixa estoque
entrega → conclusão sem segunda baixa
cancelamento após aprovação → restaura lotes exatos
urgência → atributo do pedido; não muda FIFO/FEFO
```

`Pedido.dataEntrega` registra entrega real. Não inventar data retroativa para registros antigos.

---

# 5. Resíduos — concluído e integrado

A antiga branch `feat/gestao-residuos` deixou de ser o estado vigente. O módulo foi portado/reconciliado sobre a `main` e depois integrado pelo ciclo `feat/residuos`.

Decisão central:

```text
Produto ≠ Resíduo
```

Composição de Resíduo pode referenciar Produto sem alterar automaticamente estoque/lotes/movimentações.

Fluxo:

```text
INFORMADO
→ EM_ANALISE
→ LIBERADO_PARA_ARMAZENAMENTO
→ ARMAZENADO_TEMPORARIAMENTE
→ DESPACHADO
```

Cobertura atual:

```text
cadastro pelo laboratório                            ✅
Meus resíduos                                        ✅
recebimento pela Gestão                              ✅
análise/classificação                                ✅
riscos informados x confirmados                      ✅
Código SGL                                           ✅
rótulo                                               ✅
histórico                                             ✅
armazenamento temporário                             ✅
despacho                                             ✅
relatório                                            ✅
PDF/XLSX                                             ✅
```

Código:

```text
SGL-RES-AAAA-NNNNNN
```

O Código SGL passa a existir já no registro inicial; registros anteriores foram contemplados por backfill.

Detalhes: `docs/MODULO_RESIDUOS.md`.

---

# 6. Flyway

A `main` possui migrations de `V1` a `V12`.

Resumo recente:

```text
V5  → apresentação/fracionamento do lote
V6  → observação do lote
V7  → Código SGL do lote
V8  → tipo de embalagem do lote
V9  → forma de retirada no ItemPedido
V10 → dados usados por relatórios/fiscalização/entrega
V11 → módulo de Resíduos
V12 → backfill do Código SGL de Resíduos
```

Regra obrigatória: migration já aplicada é imutável.

---

# 7. Estagiários e vínculos institucionais

Base:

```text
/api/v1/estagiarios
```

Cobertura:

```text
listar / consultar
cadastrar
editar
ativos
por laboratório
unidade explícita no vínculo
período de estágio
tipo de vínculo
encerramento com data efetiva
```

Tipos atuais incluem:

```text
BOLSA_CNPQ
BOLSA_CAPES
BOLSA_INSTITUCIONAL
VOLUNTARIO
CONTRATUAL
```

`CONTRATUAL` representa vínculo de estágio contratual/empregatício sem bolsa.

---

# 8. Administração / Cadastros

A central administrativa do frontend foi concluída e usa suporte backend para:

```text
Laboratórios
Projetos
Produtos
Permissões de usuários existentes
```

Decisões:

- Unidade é dado institucional e não recebe CRUD manual normal no frontend;
- usuário será criado/sincronizado pelo login institucional no futuro;
- a Administração consulta usuários existentes e pode alterar somente o perfil por endpoint específico;
- Produto em Cadastros é catálogo-base; quantidades e lotes permanecem em Estoque;
- responsável de Laboratório deve pertencer à mesma Unidade;
- perfil `ESTAGIARIO` com vínculo ativo não deve ser removido antes do encerramento do estágio.

Modelos de **Resíduos pré-determinados** aparecem apenas como opção futura/em estudo; não fazem parte do contrato operacional obrigatório atual.

---

# 9. Fiscalização

Campos:

```text
fiscalizado
orgaosFiscalizadores
observacaoFiscalizacao
```

Órgãos iniciais:

```text
POLICIA_FEDERAL
VIGILANCIA_SANITARIA
ANVISA
EXERCITO
OUTRO
```

```text
fiscalizado=false → órgãos vazios + observação limpa
fiscalizado=true  → pelo menos um órgão obrigatório
```

Não inferir fiscalização a partir de risco/perecibilidade.

---

# 10. Relatórios e exportações

Relatórios atuais:

```text
1. Estagiários
2. Produtos
3. Movimentações
4. Resumo operacional
5. Estoque e lotes
6. Fiscalização
7. Resíduos
8. Pessoas por laboratório
```

Pedidos entregues continuam sendo recorte de Movimentações, não relatório próprio.

Exportações:

```text
prévia JSON
PDF
XLSX
→ mesma consulta e mesmos filtros
```

Detalhes: `docs/RELATORIOS.md` e `docs/EXPORTACAO_RELATORIOS.md`.

---

# 11. Dashboard e alertas — impacto no backend

Os dashboards do frontend foram integrados sem criar uma regra paralela de domínio. Eles compõem dados já expostos por pedidos, estoque, lotes, resíduos, movimentações, laboratórios e usuários.

Indicadores operacionais usados hoje incluem:

```text
pedidos pendentes/urgentes
estoque baixo
lotes vencidos
lotes vencendo em 7/30 dias
resíduos INFORMADO/EM_ANALISE
movimentações recentes
resumo por laboratório
```

Qualquer novo KPI que exija cálculo oficial complexo deve preferir endpoint/serviço backend próprio em vez de regra duplicada no frontend.

---

# 12. Segurança e sessão

Estado correto:

```text
Spring Security/OAuth como base técnica             ✅
validações pontuais de perfil em domínio             ✅
guardas de rota no frontend                          ✅ UX/sessão DEV
sessão DEV com expiração                             ✅ temporária
autenticação definitiva                              ⏳
autorização global real                              ⏳
auditoria baseada em identidade autenticada          ⏳
integração corporativa/SSO                           ⏳
```

A futura autenticação deve retirar dos payloads a responsabilidade de informar manualmente o usuário responsável sempre que essa identidade puder vir da sessão/token.

---

# 13. Próximos passos oficiais

Sem criar novo roadmap, o planejamento de fechamento do protótipo passa a ser:

```text
1. consolidar diretrizes/matriz de permissões        ← PRÓXIMO
2. congelar o primeiro protótipo
3. executar homologação completa integrada
4. corrigir falhas encontradas na homologação
5. autenticação + autorização + auditoria definitiva
6. integração corporativa / sincronização de Unidade
7. documentos/upload quando houver contrato definitivo
8. refactor pós-protótipo para nomenclatura técnica em inglês
```

Não reclassificar Resíduos, Administração ou Dashboard como “próxima etapa”: esses blocos já chegaram à `main`.

---

# 14. Validações consolidadas

```text
PostgreSQL + Flyway
UUID público
entrada de lote
FIFO / FEFO
lote vencido fora da aprovação
estoque utilizável insuficiente
descarte por vencimento
cancelamento restaurando lotes exatos
entrega sem segunda baixa
histórico/rastreabilidade
concorrência de aprovação
Swagger/OpenAPI
Movimentações
Relatórios base
Fiscalização
PDF/XLSX base
Resíduos — fluxo operacional
Estagiários — vínculo/encerramento
Administração — contratos principais
```

A homologação completa do protótipo congelado ainda deve executar a bateria integrada prevista no frontend, em vez de assumir que commits isolados substituem um teste de ponta a ponta.

---

# 15. Documentação de referência

Começar por:

- `docs/DOSSIE_PROJETO_SGL.md`
- `docs/README.md`
- `README.md`
- `docs/MODULO_RESIDUOS.md`
- `docs/RELATORIOS.md`
- `docs/EXPORTACAO_RELATORIOS.md`
- `docs/PENDENCIAS_POS_PROTOTIPO.md`

Para endpoints e payloads, confirmar sempre no Swagger.

---

# 16. Regra de retomada

**Não refazer os módulos principais. O produto está no fechamento do primeiro protótipo: consolidar permissões, congelar, homologar e estabilizar. Novas funcionalidades só devem entrar antes disso se corrigirem uma lacuna que impeça a homologação.**
