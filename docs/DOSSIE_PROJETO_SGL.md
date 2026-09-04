# Dossiê do Projeto SGL — Handoff para IA

**Projeto:** SGL — Sistema de Gestão de Laboratórios  
**Backend:** `gbsalermo/Sistema-SGL`  
**Frontend:** `gbsalermo/SGL-FRONTEND`  
**Snapshot:** 03/09/2026  
**Objetivo:** permitir que outra pessoa ou IA entenda o estado real do projeto, decisões já fechadas e a sequência oficial restante sem reconstruir o histórico.

---

# 1. Ordem de precedência

Quando houver conflito entre fontes:

```text
1. código da branch main
2. Swagger/OpenAPI do backend para contratos HTTP
3. CONTINUIDADE.md do repositório em trabalho
4. este DOSSIE_PROJETO_SGL.md
5. documentos específicos de decisão/módulo
6. documentos de etapas antigas, exemplos e roteiros históricos
```

Não apagar documentos antigos só porque registram um estado anterior. O erro a evitar é tratá-los como planejamento vigente.

---

# 2. Objetivo do SGL

O SGL é um sistema corporativo de gestão laboratorial com foco em:

- pedidos de materiais;
- estoque central por unidade;
- lotes, validade, embalagem e rastreabilidade;
- FIFO/FEFO;
- movimentações auditáveis;
- fiscalização de produtos controlados;
- resíduos laboratoriais de ponta a ponta;
- estagiários e vínculos institucionais;
- administração de dados-base;
- relatórios e exportações oficiais;
- dashboards e alertas no frontend;
- futura autenticação/autorização/auditoria corporativa.

Repos:

```text
gbsalermo/Sistema-SGL
→ API, regras de negócio, banco, Swagger, relatórios/exportações

gbsalermo/SGL-FRONTEND
→ Vue SPA, UX Solicitante, Gestão e Administração
```

---

# 3. Stack

## Backend

```text
Java 17
Spring Boot 4.1.0
Spring Data JPA / Hibernate
PostgreSQL
Flyway
Spring Validation
Spring Security / OAuth2 Client como base
SpringDoc OpenAPI / Swagger
JUnit / H2
OpenPDF 2.0.5
Apache POI 5.5.1
Maven
```

## Frontend

```text
Vue 3.5
Vite 8
TypeScript 5.9
Vue Router 5
Pinia 4
Axios
Vuetify 3
Node >= 20.19
```

---

# 4. Estado executivo em 03/09/2026

```text
BACKEND
Fundação Spring/PostgreSQL/Flyway                 ✅
UUID público + Long interno                       ✅
Pedidos / urgência                                ✅
Estoque / lotes                                   ✅
FIFO / FEFO                                       ✅
Embalagem / multiplicador / fracionamento         ✅
Movimentações / rastreabilidade                   ✅
Swagger/OpenAPI                                   ✅
Fiscalização                                      ✅
Relatórios base                                   ✅
PDF/XLSX                                          ✅
Resíduos                                          ✅ integrado
Relatório + PDF/XLSX de Resíduos                  ✅
Estagiários                                       ✅
Pessoas por laboratório + exportação              ✅
Suporte a Administração/Cadastros                 ✅
Alteração administrativa de perfil                ✅
Autenticação/autorização definitiva               ⏳
Integração corporativa                            ⏳

FRONTEND
Login visual + sessão DEV                         ✅
Expiração de sessão DEV                           ✅
Pedidos Solicitante/Gestão                        ✅
Estoque / lotes                                   ✅
Movimentações                                     ✅
Relatórios / fiscalização                         ✅
PDF/XLSX                                          ✅
Resíduos Solicitante/Gestão                       ✅
Rótulos Produto/Resíduo                           ✅
Estagiários                                       ✅
Pessoas por laboratório                           ✅
Administração/Cadastros                           ✅
Dashboard Gestão                                  ✅
Dashboard Solicitante                             ✅
Alertas operacionais                              ✅
Busca global                                      ✅
Tema claro/escuro                                 ✅
404 animada                                       ✅
Autenticação/autorização definitiva               ⏳
```

O primeiro protótipo está próximo do congelamento. A sequência oficial restante é consolidar permissões, congelar e homologar.

---

# 5. Identificadores

```text
Long id
→ somente fronteira interna do backend/banco

UUID publicId
→ endpoints
→ DTOs
→ frontend
```

Não reintroduzir IDs numéricos em contratos públicos novos.

---

# 6. Estoque e lotes

```text
Produto
→ catálogo e unidade-base

EstoqueCentral
→ saldo consolidado por produto/unidade

Lote
→ quantidade física, validade, embalagem, multiplicador e rastreabilidade

MovimentacaoEstoque
→ trilha das alterações físicas
```

Seleção:

```text
perecível     → FEFO
não perecível → FIFO
```

Invariantes:

```text
EstoqueCentral.quantidadeAtual acompanha soma operacional dos lotes
aprovação de pedido baixa estoque
entrega não baixa novamente
cancelamento aprovado restaura lotes exatos
lote vencido não é utilizado na aprovação
movimentação registra lote efetivamente afetado
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
false → true permitido
true → false proibido
```

---

# 7. Pedidos

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

Regras:

- aprovação executa baixa física;
- entrega registra conclusão sem nova baixa;
- cancelamento após aprovação restaura lotes consumidos;
- urgência não altera automaticamente FIFO/FEFO;
- `dataEntrega` registra o evento real.

---

# 8. Resíduos — estado vigente

A descrição antiga de uma branch divergente está superada.

O módulo experimental foi portado para a `main` atual e o fluxo funcional foi integrado.

Decisão:

```text
Produto != Resíduo
```

Produto representa catálogo/estoque. Resíduo representa material gerado no laboratório.

Uma composição de Resíduo pode referenciar um Produto apenas para rastreabilidade; essa referência não baixa, repõe ou movimenta estoque automaticamente.

Fluxo:

```text
laboratório informa
→ Gestão recebe
→ EM_ANALISE
→ Gestão confirma classificação/riscos
→ LIBERADO_PARA_ARMAZENAMENTO
→ armazenamento temporário
→ DESPACHADO
```

Status:

```text
INFORMADO
EM_ANALISE
LIBERADO_PARA_ARMAZENAMENTO
ARMAZENADO_TEMPORARIAMENTE
DESPACHADO
```

Duas experiências no frontend:

```text
Solicitante
→ Informar resíduo
→ Meus resíduos

Gestão
→ Resíduos
→ receber/analisar/rotular/armazenar/despachar
```

Código SGL:

```text
SGL-RES-AAAA-NNNNNN
```

O código é gerado no registro inicial. V12 realizou backfill em registros já existentes.

O QR pode permanecer como campo técnico de compatibilidade, mas não faz parte do rótulo visual do primeiro protótipo.

Migrations:

```text
V11__create_residuo_module.sql
V12__backfill_codigo_sgl_residuos.sql
```

Detalhes: `MODULO_RESIDUOS.md`.

---

# 9. Estagiários

O módulo já não é apenas consulta. Está implementado:

```text
listagem
cadastro
edição
vínculo com unidade/laboratório
período
Tipo de vínculo
encerramento com data efetiva
consulta de ativos
consulta por laboratório
```

Tipos:

```text
BOLSA_CNPQ
BOLSA_CAPES
BOLSA_INSTITUCIONAL
VOLUNTARIO
CONTRATUAL
```

Regras importantes:

- usuário precisa ter perfil `ESTAGIARIO` para vínculo de estágio;
- usuário não possui dois cadastros de estágio;
- laboratório e usuário devem pertencer à mesma unidade;
- data final não pode ser anterior à inicial;
- usuário vinculado não é trocado durante edição;
- encerramento não pode ocorrer duas vezes nem antes do início;
- encerramento grava a data efetiva.

---

# 10. Pessoas por laboratório

Relatório institucional adicionado para permitir auditoria de todas as pessoas vinculadas ao laboratório, não apenas estagiários.

```text
GET /api/v1/relatorios/pessoas-laboratorio
GET /api/v1/relatorios/pessoas-laboratorio/exportar?formato=PDF|XLSX
```

Inclui:

```text
laboratório + unidade
responsável
usuários vinculados
perfil
ativo/inativo
estagiário: tipo e período do vínculo
totais por perfil
```

---

# 11. Administração / Cadastros

A central administrativa foi concluída no frontend e o backend recebeu os contratos necessários.

Áreas atuais:

```text
Laboratórios
Projetos
Produtos
Permissões
Resíduos — opção futura/em breve para modelos pré-determinados
```

Decisões:

## Unidade

Não possui CRUD manual normal no frontend. No futuro virá/sincronizará via integração corporativa.

## Usuário

Não há criação manual de usuário na central. A futura autenticação institucional deve criar/sincronizar o usuário.

Administração pode consultar usuários existentes e alterar somente o perfil por endpoint específico.

## Produto

É catálogo-base. Estoque/lotes permanecem em telas operacionais.

## Laboratório

Responsável deve pertencer à mesma unidade.

## Modelos de Resíduo

São opção futura em estudo. O protótipo apenas sinaliza “Em breve”; não alterar validações atuais até existir decisão/contrato oficial.

---

# 12. Fiscalização

Campos de Produto:

```text
fiscalizado
orgaosFiscalizadores
observacaoFiscalizacao
```

Órgãos:

```text
POLICIA_FEDERAL
VIGILANCIA_SANITARIA
ANVISA
EXERCITO
OUTRO
```

Se fiscalizado, ao menos um órgão é obrigatório. Não inferir fiscalização por risco/perecibilidade.

---

# 13. Relatórios

Relatórios atuais:

```text
Estagiários
Produtos
Movimentações
Resumo operacional
Estoque e lotes
Fiscalização
Resíduos
Pessoas por laboratório
```

Decisão preservada:

```text
Pedidos entregues não possui relatório próprio.
```

Usar Movimentações para esse recorte.

Prévia, PDF e XLSX usam a mesma consulta e filtros.

---

# 14. Frontend atual — rotas relevantes

Fonte de verdade: `SGL-FRONTEND/src/router/index.ts`.

```text
/login

SOLICITANTE
/inicio
/meus-pedidos
/meus-residuos
/pedidos/novo
/residuos/novo

GESTÃO / ADMIN
/dashboard
/pedidos
/estoque
/estoque/lotes-vencendo
/estoque/:id
/movimentacoes
/estagiarios
/residuos
/relatorios
/relatorios/residuos
/relatorios/pessoas-laboratorio
/administracao/cadastros      ADMINISTRADOR
/solicitacoes/novo
/solicitacoes/meus-pedidos

RÓTULOS
/residuos/:id/rotulo
/produtos/:id/rotulo

SISTEMA
/:pathMatch(.*)*
```

Rota inicial:

```text
GESTOR / ADMINISTRADOR → /dashboard
solicitantes           → /inicio
```

---

# 15. Dashboard, alertas, busca e aparência

Frontend Gestão usa dados reais de:

```text
pedidos
estoque
lotes
resíduos
movimentações
laboratórios
usuários
```

Indicadores/atenções incluem:

```text
pedidos pendentes e urgentes
estoque baixo
lotes vencidos
lotes próximos do vencimento
resíduos INFORMADO/EM_ANALISE
movimentações recentes
resumo por laboratório
```

Os cards e alertas navegam para telas já filtradas quando aplicável.

Também estão integrados:

```text
busca global
alertas operacionais no shell
modo claro/escuro
persistência da preferência visual
```

Dashboard do Solicitante também existe em `/inicio`.

---

# 16. Login e segurança

O login visual está funcional, mas ainda é DEV.

```text
identificador + senha preenchida
→ frontend consulta usuários
→ usuário ativo é resolvido
→ senha NÃO é validada por endpoint definitivo
→ sessão DEV em localStorage
→ expiração automática em 5 horas
```

Separar:

```text
login visual / sessão DEV                    ✅
guardas por perfil no frontend                ✅ UX
validações pontuais no backend                ✅
autenticação segura                           ⏳
autorização global real                       ⏳
auditoria pela identidade autenticada         ⏳
SSO/API corporativa                           ⏳
```

---

# 17. Flyway

Sequência atual da `main`:

```text
V1 ... V12
```

As migrations já aplicadas não devem ser alteradas. Novas mudanças de banco recebem nova versão.

---

# 18. Primeiro protótipo — fechamento

O planejamento oficial de fechamento foi criado no frontend e deve continuar sendo respeitado.

Itens que já chegaram à `main`:

```text
Resíduos                                   ✅
Estagiários                                ✅
Administração/Cadastros                    ✅
Dashboard                                  ✅
Alertas operacionais                       ✅
Aparência claro/escuro                     ✅
```

Próximo bloco:

```text
1. consolidar diretrizes/matriz funcional de permissões
2. congelar o primeiro protótipo
3. executar docs/PLANO_TESTES_PRIMEIRO_PROTOTIPO.md
4. estabilizar apenas o que a homologação encontrar
```

Após isso:

```text
autenticação/autorização/auditoria definitiva
→ integração corporativa
→ documentos/upload quando contrato existir
→ refactor técnico para inglês
```

---

# 19. Pendências que NÃO devem ser confundidas com falha atual

```text
autenticação segura real                     ⏳
integração corporativa                        ⏳
upload/download documental definitivo         ⏳
modelos pré-determinados de resíduos           💡 futuro/em estudo
refactor técnico para inglês                  ⏳ pós-protótipo
```

A interface de usuário permanece em português mesmo após o futuro refactor técnico.

---

# 20. Regra de retomada

**Não refazer Pedidos, Estoque, Resíduos, Estagiários, Administração ou Dashboard. O projeto está no fechamento do primeiro protótipo. Toda nova intervenção deve primeiro verificar se pertence à matriz de permissões, congelamento/homologação, correção de falha ou a uma etapa posterior já documentada.**
