# Dossiê do Projeto SGL — Handoff

**Projeto:** SGL — Sistema de Gestão de Laboratórios  
**Backend:** `gbsalermo/Sistema-SGL`  
**Frontend:** `gbsalermo/SGL-FRONTEND`  
**Snapshot:** 04/09/2026  
**Estado:** primeiro protótipo funcional aprovado; ajustes de pré-produção em andamento.  
**Objetivo:** permitir que outra pessoa ou IA retome o projeto pelo estado real atual, sem reconstruir o histórico nem usar um roadmap antigo como tarefa imediata.

---

# 1. Ordem de precedência

Quando houver conflito:

```text
1. código da main
2. Swagger/OpenAPI do backend para contratos HTTP
3. CONTINUIDADE.md do repositório em trabalho
4. este DOSSIE_PROJETO_SGL.md
5. documentos específicos de decisão/módulo
6. exemplos, roteiros e snapshots históricos
```

Documentação histórica pode ser mantida para rastreabilidade, mas não deve comandar o planejamento atual.

---

# 2. Fase atual

O sistema foi funcionalmente aprovado. Antes de iniciar o roadmap formal de produção, existe um bloco de **pré-produção pós-aprovação**.

Sequência atual:

```text
1. limpeza, revisão e atualização documental
2. levantamento dos ajustes de pré-produção
3. implementação/refinamento dos ajustes
4. validação e estabilização do bloco
```

Somente depois:

```text
matriz de permissões
→ congelamento funcional
→ homologação integrada final
→ autenticação/autorização/auditoria definitiva
→ integração corporativa
→ demais etapas formais de produção
```

A matriz de permissões continua válida no roadmap, mas **não é a tarefa imediata enquanto o bloco atual estiver aberto**.

---

# 3. Objetivo do SGL

O SGL é um sistema de gestão laboratorial com foco em:

- pedidos de materiais;
- estoque central por Unidade;
- lotes, validade, embalagem e rastreabilidade;
- FIFO/FEFO;
- movimentações de estoque;
- produtos fiscalizados;
- resíduos laboratoriais;
- estagiários e vínculos institucionais;
- administração de dados-base;
- relatórios e exportações;
- dashboards, alertas e busca no frontend;
- futura autenticação/autorização/auditoria corporativa.

Repos:

```text
gbsalermo/Sistema-SGL
→ API, regras de negócio, persistência, Flyway, Swagger, relatórios/exportações

gbsalermo/SGL-FRONTEND
→ Vue SPA, UX de Solicitante, Gestão e Administração
```

---

# 4. Stack

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

# 5. Estado executivo

## Backend

```text
Fundação Spring/PostgreSQL/Flyway                 ✅
UUID público + Long interno                       ✅
Pedidos / urgência                                ✅
Estoque / lotes                                   ✅
FIFO / FEFO                                       ✅
Embalagem / multiplicador / fracionamento         ✅
Movimentações / rastreabilidade                   ✅
Swagger/OpenAPI                                   ✅
Fiscalização                                      ✅
Relatórios + PDF/XLSX                             ✅
Resíduos                                          ✅
Estagiários                                       ✅
Pessoas por laboratório                           ✅
Administração/Cadastros                           ✅
Isolamento operacional por Unidade                ✅
Autenticação/autorização definitiva               ⏳
Integração corporativa                            ⏳
```

## Frontend

```text
Sessão DEV + expiração                            ✅
Pedidos Solicitante/Gestão                        ✅
Estoque / lotes                                   ✅
Movimentações                                     ✅
Relatórios / fiscalização                         ✅
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
404                                               ✅
Contexto de Unidade enviado à API                 ✅
Autenticação/autorização definitiva               ⏳
```

---

# 6. Arquitetura backend

```text
Controller
→ contrato HTTP

Service
→ regras, transações e orquestração

Repository
→ persistência/consultas

Model
→ estado de domínio

RequestDTO / ResponseDTO
→ contratos públicos
```

Fluxo típico:

```text
Controller recebe UUID
→ Service resolve publicId
→ domínio trabalha com Long internamente
→ ResponseDTO devolve UUID
```

---

# 7. Identificadores

```text
Long id
→ interno: banco, JPA, FKs e locks

UUID publicId
→ público: endpoints, DTOs e frontend
```

Não introduzir novos IDs numéricos em contratos públicos.

---

# 8. PostgreSQL e Flyway

Configuração de desenvolvimento:

```text
PostgreSQL
Hibernate ddl-auto=validate
Flyway habilitado
```

O Hibernate valida o schema; o Flyway controla a evolução.

Sequência atual:

```text
V1 ... V12
```

Migrations aplicadas são imutáveis. Novas alterações de schema devem receber nova migration.

---

# 9. Multitenancy por Unidade

Desde 04/09 a `main` possui isolamento operacional por Unidade.

## Backend

```text
X-SGL-Unidade-Id
→ TenantRequestFilter
→ valida UUID
→ TenantContext
→ services/repositories restringem dados da Unidade
→ TenantContext é limpo ao final da requisição
```

O filtro está presente em áreas centrais como:

```text
Usuários
Laboratórios
Projetos
Produtos
Estoque
Lotes
Pedidos
Movimentações
Estagiários
Resíduos
```

`TenantProvider` permite que consultas Spring Data usem a Unidade atual.

## Frontend

A sessão DEV contém o contexto institucional do usuário, incluindo:

```text
unidadeId
unidadeNome
unidadeSigla
laboratorioId
laboratorioNome
```

O interceptor de `src/services/http.ts` envia:

```text
X-SGL-Unidade-Id: <unidadeId>
```

## Limite de segurança

```text
isolamento funcional por Unidade              ✅
validação de cenários multitenant              ✅
fronteira definitiva de autorização            ❌
```

O header ainda é controlado pelo cliente e o backend ainda não possui autenticação definitiva. Na produção, tenant/Unidade deverá ser derivado de uma identidade autenticada confiável.

---

# 10. Domínio de estoque

```text
Produto
→ catálogo e unidade-base

EstoqueCentral
→ saldo consolidado por produto/Unidade

Lote
→ quantidade física, validade, embalagem, multiplicador e rastreabilidade

MovimentacaoEstoque
→ trilha das operações físicas
```

Seleção:

```text
perecível     → FEFO
não perecível → FIFO
```

Invariantes:

```text
EstoqueCentral.quantidadeAtual acompanha os lotes
aprovação de pedido baixa estoque
entrega não baixa novamente
cancelamento aprovado restaura os lotes utilizados
lote vencido não participa da aprovação
movimentação identifica o lote efetivamente afetado
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

# 11. Pedidos

Estados:

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

Regras:

- criação não baixa estoque;
- aprovação executa a baixa física;
- entrega registra conclusão sem nova baixa;
- cancelamento após aprovação restaura as quantidades dos lotes consumidos;
- urgência não altera FIFO/FEFO;
- `dataEntrega` registra o evento real.

Evitar afirmar em documentos que todo cancelamento necessariamente cria um tipo específico de movimento se isso não estiver garantido pelo fluxo atual. O comportamento garantido hoje é a restauração dos lotes utilizados.

---

# 12. Resíduos

Decisão:

```text
Produto != Resíduo
```

Produto representa catálogo/estoque. Resíduo representa material gerado no laboratório.

Uma composição de Resíduo pode referenciar Produto para rastreabilidade, sem baixar ou repor estoque automaticamente.

Fluxo:

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

V11 criou o módulo; V12 fez o backfill dos códigos para registros anteriores.

O QR pode existir tecnicamente, mas não integra o rótulo visual atual.

Detalhes: `MODULO_RESIDUOS.md`.

---

# 13. Estagiários

Cobertura:

```text
listagem
cadastro
edição
vínculo com Unidade/Laboratório
período
tipo de vínculo
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

- usuário deve possuir perfil `ESTAGIARIO`;
- não há dois cadastros de estágio para o mesmo usuário;
- laboratório e usuário devem pertencer à mesma Unidade;
- data final não pode anteceder a inicial;
- usuário vinculado não é trocado em edição;
- encerramento registra a data efetiva e não pode ser repetido.

---

# 14. Administração / Cadastros

Áreas atuais:

```text
Laboratórios
Projetos
Produtos
Permissões de usuários existentes
```

Decisões:

## Unidade

É dado institucional. Não possui CRUD manual normal no frontend.

## Usuário

Não é cadastrado manualmente pela central. A futura autenticação corporativa deverá criar/sincronizar usuários.

## Produto

Em Cadastros representa catálogo-base. Estoque e lotes permanecem na operação de estoque.

## Laboratório

Responsável deve pertencer à mesma Unidade.

## Resíduos pré-determinados

Continuam como possibilidade futura; não fazem parte do fluxo operacional obrigatório atual.

---

# 15. Fiscalização

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

Se fiscalizado, pelo menos um órgão é obrigatório. Não inferir fiscalização por risco ou perecibilidade.

---

# 16. Relatórios

Relatórios integrados:

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

Pedidos entregues são um recorte de Movimentações, não um relatório próprio.

Prévia, PDF e XLSX devem usar a mesma consulta e os mesmos filtros.

---

# 17. Frontend — rotas principais

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
/administracao/cadastros
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
TECNICO / ANALISTA / PESQUISADOR / ESTAGIARIO → /inicio
```

---

# 18. Dashboard, alertas, busca e tema

Gestão usa dados reais de:

```text
pedidos
estoque
lotes
resíduos
movimentações
laboratórios
usuários
```

Indicadores incluem:

```text
pedidos pendentes/urgentes
estoque baixo
lotes vencidos
lotes vencendo em 7/30 dias
resíduos INFORMADO/EM_ANALISE
movimentações recentes
resumo por laboratório
```

Também estão integrados:

```text
busca global
alertas operacionais
tema claro/escuro
persistência da preferência
Dashboard do Solicitante
```

---

# 19. Sessão DEV e segurança

Login atual:

```text
identificador + senha preenchida
→ frontend consulta usuários existentes
→ resolve usuário ativo
→ senha ainda não é validada por autenticação definitiva
→ sessão em localStorage
→ expiração automática em 5 horas
```

Separação correta:

```text
sessão DEV                                  ✅ temporária
guardas por perfil frontend                 ✅ UX
isolamento por Unidade via header           ✅ desenvolvimento
Spring Security como base                   ✅
autenticação segura                         ⏳
autorização global real                     ⏳
auditoria pela identidade autenticada       ⏳
SSO/API corporativa                         ⏳
```

O backend ainda possui `permitAll()` temporário. Não tratar controles visuais, sessão local ou header de Unidade como segurança final.

---

# 20. Decisão corporativa de Unidade

Unidade não deve ser criada/selecionada livremente por usuário em produção.

Modelo futuro esperado:

```text
login/integração corporativa
→ identidade institucional
→ identificador estável da Unidade
→ backend resolve/reutiliza a Unidade
→ sessão/token recebe o tenant confiável
→ frontend apenas consome o contexto resolvido
```

A sincronização deverá ser idempotente e preservar identificador corporativo estável.

---

# 21. Pendências planejadas

Não confundir com falhas funcionais do sistema aprovado:

```text
autenticação/autorização/auditoria definitiva
integração corporativa
upload/download documental quando houver contrato
refactor técnico de nomenclatura para inglês
modelos pré-determinados de resíduos, se aprovados no futuro
```

A interface continua em português mesmo em eventual refactor técnico.

---

# 22. Regra de retomada

```text
1. ler CONTINUIDADE.md
2. conferir a main atual
3. validar contratos no Swagger
4. consultar este dossiê
5. abrir o documento específico da área
6. distinguir decisão atual de histórico
```

**O SGL está funcionalmente aprovado e em pré-produção pós-aprovação. Não tratar documentos antigos que dizem “matriz de permissões = próximo passo” como estado atual; essa etapa pertence ao roadmap formal que começará após o bloco atual.**
