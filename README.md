<a id="readme-top"></a>

<div align="center">
  <img src="docs/LOGO.png" alt="SGL Logo" width="400" height="auto">

# SGL — Sistema de Gestão de Laboratórios

**Backend corporativo para pedidos, estoque por lotes, rastreabilidade, resíduos, fiscalização, vínculos institucionais, cadastros e relatórios laboratoriais.**

`Java 17` · `Spring Boot 4.1` · `PostgreSQL` · `Flyway` · `Swagger/OpenAPI` · `PDF/XLSX`

</div>

## O que é o SGL?

**Um sistema para acompanhar a operação laboratorial desde o vínculo da pessoa e o contexto de pesquisa até a movimentação física de materiais e a destinação de resíduos.**

Em uma instituição com vários laboratórios, uma solicitação não envolve apenas um produto e uma quantidade: é necessário saber **quem solicitou, para qual laboratório, em qual Projeto/SCI/Atividade, com qual autorização e de qual lote/recipiente o material saiu**. Planilhas e processos isolados não oferecem essa rastreabilidade de forma integrada.

O SGL reúne esses contextos em um único domínio operacional, com trilhas de movimentação, dados corporativos, controle de acesso e relatórios. A interface web está no repositório [SGL-FRONTEND](https://github.com/gbsalermo/SGL-FRONTEND); este repositório contém a **API, regras de domínio, persistência e migrations**.

**Quem utiliza:** solicitantes (empregados e estagiários), orientadores e equipes de Gestão/Administração. O acesso e as ações disponíveis dependem do perfil, da Unidade e dos vínculos.

| Problema real | Resposta do SGL |
|---|---|
| Pessoas e laboratórios ligados a múltiplos Projetos/Atividades | Contexto operacional e autorizações derivados de participações e vínculos |
| Material armazenado em lotes e recipientes, nem sempre totalmente fechados | Estoque físico com FEFO/FIFO, fracionamento controlado, origem de cada baixa e estornos |
| Pedido, preparo, resíduo e fiscalização desconectados | Fluxos transacionais com histórico e rastreabilidade |
| Unidades e perfis com responsabilidades diferentes | Identidade corporativa, JWT, isolamento por Unidade e controle de ações |
| Conferência manual de dados e emissão de documentos | Painéis, relatórios PDF/XLSX, rótulos e documento de entrada |

**Estado verificável — novembro de 2026:** os fluxos principais, JWT corporativo, relatórios, rótulos e vínculos multilaboratório estão no código `release`. A auditoria documental foi consolidada; **testes integrados em PostgreSQL, autorização detalhada de consultas, carga, impressão física e uso Android/iOS ainda precisam de homologação**. Não há alegação aqui de certificação de segurança ou desempenho. Veja [estado e riscos](docs/REGISTRO_PENDENCIAS_REVISAO_SGL_2026-11.md) e o [plano cloud/mobile](docs/PLANO_HOMOLOGACAO_NUVEM_MOBILE_2026-11-17.md).

## Objetivo

O SGL representa o ciclo operacional dos laboratórios de forma rastreável:

```text
estrutura institucional
→ catálogo de produtos
→ estoque central por Unidade
→ entrada de lotes
→ solicitação de material
→ aprovação
→ baixa física FIFO/FEFO
→ entrega
→ movimentações auditáveis
→ geração e destinação de resíduos
→ relatórios/fiscalização
→ administração de dados-base
```

As regras de estoque (FEFO/FIFO), as transições operacionais e os cálculos oficiais são executados pelo backend. O frontend apresenta e conduz as operações usando os contratos da API.

---

## Um fluxo completo: da pesquisa ao consumo físico

Este exemplo resume **regras existentes**, sem representar uma transação executada nesta auditoria:

1. Um **estagiário** acessa o sistema e seleciona uma **participação elegível** em uma Atividade (associada a SCI e Projeto). O laboratório precisa estar autorizado **tanto no contexto do Projeto quanto nos vínculos da pessoa**.
2. Ele solicita um material ou uma Solução. A Gestão analisa o pedido; **criação não reduz saldo**.
3. Quando um pedido de material é aprovado, o backend identifica **lote(s) e recipiente(s)** elegíveis, aplica FEFO aos perecíveis e FIFO aos demais e realiza a baixa física. Recipientes abertos e fechados têm tratamento específico para evitar abertura desnecessária.
4. A entrega conclui o atendimento **sem descontar de novo**. No cancelamento permitido, a devolução é vinculada às saídas originais.
5. Resíduos gerados são **registros próprios**: podem referenciar produtos da composição, mas não são automaticamente uma nova baixa de estoque. Relatórios e rótulos apoiam fiscalização e rastreabilidade.

Um **empregado** segue regras de autorização diferentes das de um estagiário: pode solicitar a laboratórios ativos da Unidade, com sinalização quando o destino não pertence aos seus vínculos. **O SGL não pressupõe que todos os perfis possuam a mesma liberdade operacional.**

---

## Decisões de engenharia que caracterizam o projeto

- **Contexto antes da operação:** a hierarquia Projeto → SCI → Atividade e os vínculos pessoais determinam a elegibilidade da solicitação. Não basta selecionar um laboratório no frontend.
- **Estoque físico, não apenas um saldo numérico:** lotes, recipientes, apresentação, medida, fracionamento, validade e movimentações compõem a regra de retirada e devolução.
- **Consistência por transação:** aprovação realiza a baixa; entrega não a repete; cancelamento permitido reverte as quantidades e a origem física rastreadas.
- **Domínios separados:** Produto, Solução e Resíduo possuem semânticas distintas; registrar a composição de um Resíduo não gera automaticamente movimento de estoque.
- **Identidade e integração institucional:** empregados e colaboradores têm políticas diferentes de primeiro acesso; JWT, Unidade e permissões são resolvidos no servidor, não por filtros de interface.
- **Operação e manutenção:** Flyway versiona a evolução relacional; separação Controller/Service/Repository/DTO torna as regras testáveis; relatórios e impressão permitem uso fora da tela.

**Esses são diferenciais da modelagem adotada no SGL**, não uma afirmação de exclusividade absoluta no mercado. Algumas verificações de segurança, desempenho e integração ainda estão no [backlog](docs/REGISTRO_PENDENCIAS_REVISAO_SGL_2026-11.md).

---

## Arquitetura

```text
HTTP / REST
     ↓
Controller
     ↓ RequestDTO / ResponseDTO
Service
     ↓
Repository
     ↓
PostgreSQL + Flyway
```

Responsabilidades:

```text
Controller  → contrato HTTP e validação de entrada
Service     → regra de negócio, transação e orquestração
Repository  → persistência e consultas
Model       → estado do domínio
DTO         → contrato público da API
```

### Identificadores

```text
Long id
→ interno: banco, JPA, FKs e locks

UUID publicId
→ público: DTOs, endpoints e frontend
```

Os contratos públicos da API utilizam UUID como identificador das entidades; os IDs numéricos internos permanecem na persistência.

---

### Como backend e frontend se conectam

```text
Solicitante / Orientador / Gestão / Administração
                    │ navegador
                    ▼
        Vue 3 + TypeScript (SGL-FRONTEND)
                    │ HTTPS, mesmo host
             Nginx: /api → sgl-api
                    │
                    ▼
      Spring Boot REST + JWT + TenantContext
          │                   │
  regras de negócio     gateway corporativo
          │             (integração institucional)
          ▼
    JPA + PostgreSQL 16
      │
    Flyway (V1–V48)
```

A infraestrutura de implantação versionada utiliza **containers separados para web e API**, Nginx servindo a SPA e encaminhando `/api` ao backend, além do PostgreSQL. A interface, a API e os vídeos da Ajuda têm ciclos de implantação distintos. O desenho de staging deve usar redes, volumes, banco e credenciais **isolados**, sem reutilizar a instalação institucional.

---

## Multitenancy por Unidade

A Unidade do SGL é delimitada pela **identidade autenticada e pelo JWT**, com políticas de `TenantRequestFilter`, `TenantContext` e services/repositories no backend.

```text
Login corporativo / JWT
→ usuário ativo e Unidade corporativa habilitada
→ TenantRequestFilter obtém a Unidade da sessão
→ TenantContext restringe consultas e operações por Unidade
```

O header `X-SGL-Unidade-Id` **não identifica livremente o tenant**: ele só é admitido para `ADMINISTRADOR_GERAL` selecionar outra Unidade autorizada. Fluxos `dev/demo` são exclusivos desses profiles e não constituem autenticação de produção.

**Auditoria de autorização ainda aberta:** filtragem por Unidade não substitui autorização por objeto para dados de outro usuário da mesma Unidade. Consultas de Pedidos, Estagiários, Resíduos, Usuários e Relatórios precisam de testes de acesso por titular/perfil, conforme [registro único de pendências](docs/REGISTRO_PENDENCIAS_REVISAO_SGL_2026-11.md).
---

## Modelo de domínio — relações fundamentais

O domínio possui **relações hierárquicas e associações multilaboratório**; por isso, não é fiel reduzi-lo a uma árvore onde todo Projeto pertence necessariamente a um único Laboratório.

```text
Unidade
  ├─ Laboratórios                 ← mais de um por Projeto/pessoa, conforme vínculo
  ├─ Pessoas / perfis / vínculos  ← autorização e histórico
  └─ Estoque Central
       └─ Produto → Lote → Recipiente → Movimentação

Projeto ↔ Laboratório(s)
   └─ SCI
       └─ Atividade ← Participação do estagiário

Pedido de material / Pedido de solução
   ├─ solicitante + Laboratório de destino
   ├─ contexto Projeto/SCI/Atividade quando aplicável
   └─ itens e transições (aprovação, preparo/entrega/cancelamento)

Resíduo
   ├─ gerador + Laboratório + contexto quando aplicável
   ├─ composição de catálogo e/ou livre
   └─ análise, armazenamento e destinação
```

| Entidade/conceito | Significado |
|---|---|
| **Unidade e Laboratório** | Tenant institucional e locais operacionais, com laboratórios múltiplos autorizados |
| **Projeto / SCI / Atividade** | Contexto de pesquisa hierárquico; pode relacionar diferentes laboratórios |
| **Vínculo / Participação** | Período institucional e elegibilidade operacional da pessoa, especialmente do estagiário |
| **EstoqueCentral / Produto** | Saldo por Unidade e item de catálogo com unidade de controle |
| **Lote / Recipiente** | Validade, embalagem, estado físico de abertura e fração consumível |
| **Movimentação** | Histórico auditável de entradas, saídas, devoluções e descartes por origem física |
| **Pedido / Solução / Resíduo** | Processos com regras e ciclos de vida próprios; não são entidades intercambiáveis |

Na implementação, os identificadores `Long` sustentam JPA/FKs internamente e `UUID publicId` atravessa DTOs e endpoints. Este é um **mapa conceitual**: os relacionamentos e cardinalidades definitivos estão nas entidades, migrations e [dossiê técnico](docs/DOSSIE_PROJETO_SGL.md).

---

## Estoque, FIFO e FEFO

```text
produto perecível     → FEFO
produto não perecível → FIFO
```

Invariantes:

```text
EstoqueCentral.quantidadeAtual acompanha os lotes
aprovação baixa estoque
entrega não baixa novamente
cancelamento aprovado restaura os lotes exatos
lote vencido não é elegível para aprovação
movimentação identifica o lote efetivamente afetado
```

Formas de retirada atuais:

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

## Pedidos

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

A aprovação realiza a baixa física. A entrega apenas conclui o fluxo. O cancelamento de pedido aprovado restaura as quantidades dos lotes utilizados. Urgência não altera FIFO/FEFO.

---

## Resíduos

Decisão de domínio:

```text
Produto != Resíduo
```

Uma composição de Resíduo pode referenciar Produto para rastreabilidade, sem movimentar estoque automaticamente.

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

Detalhes: [`docs/MODULO_RESIDUOS.md`](docs/MODULO_RESIDUOS.md).

---

## Relatórios

Relatórios implementados e presentes na `release` (com homologação integrada e equivalência de filtros ainda pendentes):

```text
Estagiários
Produtos
Projetos — consolidação de SCI, Atividades e participações
Movimentações — Resumo / Detalhamento
Estoque e lotes
Fiscalização
Resíduos
Pessoas por laboratório
```

A Central apresenta **Movimentações** com modos Resumo (contagens de ocorrências, sem rankings) e Detalhamento (filtros por tipo, origem, contexto Projeto/SCI/Atividade, produto, apresentação/unidade e período). As quantidades físicas têm unidades identificadas e exibição opcional no Detalhamento e na exportação PDF/XLSX. **Estoque e lotes** informam apresentação, conteúdo e recipientes fechados, abertos ou esgotados. **Fiscalização** apresenta a trilha de movimentações com recipientes vinculados.

A classificação histórica de consumo como apresentação completa ou por medida é derivada das parcelas registradas, sem afirmar a intenção original da solicitação. Os endpoints anteriores de Movimentações e Resumo Operacional permanecem disponíveis.

A interface encaminha filtros para consultas e exportações PDF/XLSX. A equivalência de resultados para todas as combinações ainda deve ser validada; há riscos de desempenho e uma divergência semântica de unidades físicas em um DTO de Resumo Operacional registrados na auditoria.

Pedidos entregues continuam sendo um recorte de Movimentações, não um relatório separado.

---

## Identidade e controle de acesso

```text
login corporativo (autenticacaoapi + Empregado/Colaboradorapi)  ✅
sessão JWT própria (HS256, 5 h, só publicId)                     ✅
usuário recarregado do banco a cada requisição                   ✅
Unidade derivada da sessão                                       ✅
X-SGL-Unidade-Id aceito só do ADMINISTRADOR_GERAL                ✅
pendentes, aprovação, recusa e importação pela gestão            ✅
regras de concessão de perfil (PerfilPolicy)                     ✅
log de auditoria (logger sgl.auditoria)                          ✅
matriz completa de permissões por módulo                         ⏳ roadmap item 1
```

O fluxo atual de autenticação e seus testes estão mapeados no [dossiê técnico](docs/DOSSIE_PROJETO_SGL.md). Planos de autenticação datados permanecem como contexto histórico, não substituem o código da `release`.

### Autenticação corporativa

- `POST /api/v1/auth/login` valida login de rede (ou CPF, para colaboradores) e senha na `autenticacaoapi`, resolve a pessoa na Empregadoapi/Colaboradorapi, sincroniza o `Usuario` e devolve o JWT. **Empregado novo** pode ser registrado como `ATIVO` com perfil sugerido; **colaborador/estagiário novo** recebe `PENDENTE_VINCULACAO` até a regularização pela Gestão. Pessoa importada pela Gestão pode ser criada ativa, respeitando concessão de perfis e vínculos.
- `GET /api/v1/auth/me` devolve o usuário da sessão; `GET /api/v1/auth/config` informa se o login DEV está ativo.
- `POST /api/v1/auth/dev-login` existe **só** nos perfis `dev` e `demo` e aceita apenas usuários de origem `DEV` (massa de exemplo).
- O CPF nunca sai em resposta HTTP do SGL; nos logs aparece sempre mascarado. Senha nunca é gravada nem logada.
- "Sair" é feito no cliente (o JWT é descartado); um token roubado vale até expirar (5 h), mas deixa de funcionar na hora se o usuário for desativado.

Variáveis de ambiente (nenhum valor real em arquivo versionado):

```text
SPRING_PROFILES_ACTIVE     sem padrão: dev/demo só em máquina de desenvolvimento ou apresentação
SGL_CORPORATE_BASE_URL     gateway das APIs corporativas (padrão dev: https://api-gateway-devhomol.nuvem.ti.embrapa.br)
SGL_CORPORATE_AMBIENTE     h (homologação) ou p (produção)
SGL_CORPORATE_USERNAME     usuário de sistema (Basic Auth)
SGL_CORPORATE_PASSWORD     senha do usuário de sistema
SGL_JWT_SECRET             segredo HS256 com 32+ bytes (o valor padrão de dev não serve para produção)
SGL_PRIMEIRO_LOGIN_ADMIN   true só no primeiro deploy: o primeiro login vira ADMINISTRADOR_GERAL (uso único; padrão false)
```

Em `dev` e `demo` a aplicação sobe sem as credenciais corporativas; nesse caso o login corporativo responde 503 e o login DEV continua disponível.

Primeiro acesso de um ambiente novo: subir com `SGL_PRIMEIRO_LOGIN_ADMIN=true`, entrar com a própria conta da Embrapa (vira Administrador geral) e desligar a variável no deploy seguinte. Depois disso, a variável não tem mais efeito enquanto existir algum `ADMINISTRADOR_GERAL`, ativo ou não.

O Administrador geral habilita novas Unidades em `POST /api/v1/unidades/habilitar` (lista em `GET /api/v1/unidades/corporativas`); login de pessoa de Unidade não habilitada é recusado com `UNIDADE_NAO_HABILITADA`.

Regras de servidor além da autenticação:

- `POST/PUT/DELETE /api/v1/unidades`: só `ADMINISTRADOR_GERAL`.
- Vínculos operacionais, laboratório-base, estágios e edição de usuários: só gestão, respeitando a regra de concessão de perfis; ninguém altera o próprio perfil, situação ou laboratório.
- Rotas que ainda recebem o usuário que age (`usuarioId` no corpo ou na query: pedidos, movimentações de estoque, prorrogações, correção de código SEG, observações de estágio) recusam com 403 qualquer valor diferente do usuário da sessão.
- A matriz completa de permissões por módulo continua sendo o item 1 do roadmap.

---

## Banco e migrations

Ambiente de desenvolvimento padrão:

```text
PostgreSQL
Hibernate ddl-auto=validate
Flyway habilitado
```

O Flyway versiona e aplica as alterações de esquema do banco. Cada evolução é registrada em uma nova migration, preservando o histórico das versões já aplicadas.

A `release` consultada contém **48 migrations SQL, V1 a V48**, em `src/main/resources/db/migration/`; V47/V48 cobrem vínculos multilaboratório e sinalização de laboratório fora dos vínculos do solicitante. A aplicação em produção usa validação de esquema e Flyway. A **presença de migration no Git não prova que foi aplicada numa instância**. O perfil `test` usa H2, `create-drop` e Flyway desabilitado; a homologação das migrations sobre PostgreSQL permanece aberta.

---

## Execução local

**A partir da raiz do repositório** (com Java 17 e variáveis de banco/credenciais apropriadas):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

No Windows, usar `mvnw.cmd` ou terminal que execute `./mvnw`. O perfil `dev` é **somente local**; configurar PostgreSQL isolado e nunca usar dados institucionais para testes destrutivos.

```text
API local:  http://localhost:8080/api/v1
Swagger:    http://localhost:8080/swagger-ui/index.html
OpenAPI:    http://localhost:8080/v3/api-docs
```

Testes existentes (não executados nesta revisão):

```bash
./mvnw test
./mvnw verify
```

Esses comandos não substituem a verificação PostgreSQL/Flyway, segurança horizontal, carga, frontend E2E e impressão em hardware real, descritas no [plano de homologação](docs/PLANO_HOMOLOGACAO_NUVEM_MOBILE_2026-11-17.md).
---

## Evolução do projeto — referência de 17/11/2026

**A branch `release` já incorpora os fluxos que antigas anotações de outubro descreviam como pendentes.** A comparação técnica está nos dossiês; o quadro não substitui testes executados.

| Ciclo | Estado verificado por leitura do código |
|---|---|
| Etapas 1–6 | Bases operacionais, Projetos/SCI/Atividades, Estagiários/vínculos e fluxos históricos incorporados |
| Etapa 7 | Estoque físico, lote/recipiente, FEFO/FIFO, fracionamento e Soluções incorporados |
| Etapa 8 | Contextos elegíveis e Pedidos com regras distintas para empregados e estagiários incorporados |
| Etapa 9 | Relatórios consolidados, filtros e exportação PDF/XLSX presentes na `release` |
| Etapa 10 | Rótulos de Produto/Resíduo/Solução e documento de entrada presentes; impressão física Zebra **não homologada** nesta auditoria |
| Estagiários multilaboratório | V47/V48 e contratos de múltiplos laboratórios presentes; regressão E2E pendente |
| Etapas 12–13 de qualidade | Auditoria documental avançada, **execução de arquitetura/carga/E2E e homologação cloud/mobile pendentes** |

**Portões em aberto:** matriz de autorização por registro, migrações reais em PostgreSQL, testes completos, carga, integração corporativa e navegação Android/iOS por estagiários. [31 itens rastreados](docs/REGISTRO_PENDENCIAS_REVISAO_SGL_2026-11.md) não equivalem a 31 defeitos confirmados: incluem riscos, decisões, divergências e testes futuros.
---

## Documentação

| Documento | Papel |
|---|---|
| [`CONTINUIDADE.md`](CONTINUIDADE.md) | Estado e regra de retomada |
| [`docs/README.md`](docs/README.md) | Índice atual e documentos especializados |
| [`docs/DOSSIE_PROJETO_SGL.md`](docs/DOSSIE_PROJETO_SGL.md) | Mapa técnico consolidado |
| [`docs/GOVERNANCA_DOCUMENTAL.md`](docs/GOVERNANCA_DOCUMENTAL.md) | Precedência de documentos e histórico |
| [`docs/MATRIZ_COERENCIA_FRONTEND_BACKEND_2026-11-12.md`](docs/MATRIZ_COERENCIA_FRONTEND_BACKEND_2026-11-12.md) | Evidências estáticas frontend/backend |
| [`docs/REGISTRO_PENDENCIAS_REVISAO_SGL_2026-11.md`](docs/REGISTRO_PENDENCIAS_REVISAO_SGL_2026-11.md) | **Fila única** de riscos, decisões e correções posteriores |
| [`docs/ETAPA_12_PLANO_QUALIDADE_ARQUITETURA_CARGA_E2E.md`](docs/ETAPA_12_PLANO_QUALIDADE_ARQUITETURA_CARGA_E2E.md) | Testes transversais planejados |
| [`docs/PLANO_HOMOLOGACAO_NUVEM_MOBILE_2026-11-17.md`](docs/PLANO_HOMOLOGACAO_NUVEM_MOBILE_2026-11-17.md) | Ensaios CL-01–11 e MB-01–12, **não executados** |
| [`docs/AUDITORIA_REFERENCIAS_E_SINCRONIZACAO_2026-11-17.md`](docs/AUDITORIA_REFERENCIAS_E_SINCRONIZACAO_2026-11-17.md) | Links, workflows e checklist de MR |
| [`docs/SINCRONIZACAO_GITLAB_GITHUB.md`](docs/SINCRONIZACAO_GITLAB_GITHUB.md) | GitLab `release`, GitHub espelho, sincronizações e MRs |

Os documentos de etapa e exemplos antigos são **registros históricos**. Quando houver conflito, prevalecem o **código `release` verificado**, contratos OpenAPI e governança atuais. Nenhuma mudança funcional foi feita nesta revisão documental.
