<a id="readme-top"></a>

<div align="center">
  <img src="docs/LOGO.png" alt="SGL Logo" width="400" height="auto">

# SGL — Sistema de Gestão de Laboratórios

**Backend corporativo para pedidos, estoque por lotes, rastreabilidade, resíduos, fiscalização, vínculos institucionais, cadastros e relatórios laboratoriais.**

`Java 17` · `Spring Boot 4.1` · `PostgreSQL` · `Flyway` · `Swagger/OpenAPI` · `PDF/XLSX`

</div>

---

## Estado atual — 16/10/2026

O primeiro protótipo do SGL foi aprovado funcionalmente, e as **Etapas 1–7 da pré-produção foram concluídas e validadas pelo responsável**. A Etapa 7 consolidou unidades e apresentações, recipientes físicos, medidas decimais, Soluções, Pedidos por embalagem ou medida, abertura parcial de embalagens, alertas de estoque mínimo configuráveis e integração backend/frontend. **A Etapa 8 é a próxima**, com contexto de participação dos Estagiários em Pedidos/Soluções e teste operacional completo. A homologação integral e a autenticação/autorização definitiva ainda são trabalhos posteriores.

Estado consolidado do backend:

```text
Pedidos / urgência                                ✅
Estoque / lotes                                   ✅
FIFO / FEFO                                       ✅
Embalagem / multiplicador / fracionamento         ✅
Movimentações / rastreabilidade                   ✅
Swagger / OpenAPI                                 ✅
Fiscalização de produtos                          ✅
Relatórios operacionais                           ✅
Exportação PDF/XLSX                               ✅
Resíduos — fluxo completo                         ✅
Estagiários — vínculo/edição/encerramento         ✅
Pessoas por laboratório                           ✅
Administração / Cadastros                         ✅
Isolamento operacional por Unidade                ✅
Autenticação/autorização definitiva               ⏳ etapa formal posterior
Integração corporativa/SSO                        ⏳ etapa formal posterior
```

No frontend também estão integrados dashboards, busca global, alertas, tema claro/escuro, sessão DEV, rotas por perfil e propagação do contexto de Unidade.

> Para retomar o projeto, começar por [`CONTINUIDADE.md`](CONTINUIDADE.md), [`docs/SINCRONIZACAO_GITLAB_GITHUB.md`](docs/SINCRONIZACAO_GITLAB_GITHUB.md), [`docs/PLANO_PRE_PRODUCAO.md`](docs/PLANO_PRE_PRODUCAO.md), [`docs/CONTINUIDADE_ETAPA_5_2026-09-24.md`](docs/CONTINUIDADE_ETAPA_5_2026-09-24.md) e [`docs/DOSSIE_PROJETO_SGL.md`](docs/DOSSIE_PROJETO_SGL.md). O GitLab é a fonte canônica de `main`; contratos HTTP continuam pertencendo ao Swagger/OpenAPI em execução.

---

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

Regras críticas permanecem no backend; o frontend não deve recriar FIFO/FEFO, transições de domínio, validações oficiais ou cálculos de relatórios.

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

Novos contratos públicos devem continuar usando UUID.

---

## Multitenancy por Unidade

A `main` possui isolamento operacional por Unidade.

Fluxo atual:

```text
frontend lê unidadeId da sessão DEV
→ envia X-SGL-Unidade-Id
→ TenantRequestFilter valida o UUID
→ TenantContext mantém a Unidade durante a requisição
→ services/repositories restringem os dados da Unidade
```

Esse mecanismo é importante para validar a separação entre Unidades durante o desenvolvimento, mas **ainda não é a fronteira definitiva de segurança**, porque o cabeçalho é informado pelo cliente e o backend ainda não deriva a Unidade de uma identidade autenticada confiável.

Na etapa de autenticação corporativa, o tenant deverá vir da sessão/token institucional.

---

## Domínio principal

```text
Unidade
├── Laboratórios
│   ├── Usuários
│   ├── Estagiários
│   ├── Projetos
│   ├── Pedidos
│   └── Resíduos
└── Estoque Central
    └── Produto
        └── Lotes
            └── Movimentações
```

Conceitos centrais:

- **Produto:** catálogo e unidade-base de controle;
- **EstoqueCentral:** saldo consolidado por produto/Unidade;
- **Lote:** quantidade física, validade, embalagem, multiplicador e rastreabilidade;
- **MovimentacaoEstoque:** trilha das operações físicas;
- **Pedido:** solicitação e ciclo de aprovação/entrega;
- **Resíduo:** material gerado no laboratório e encaminhado à Gestão;
- **Estagiário:** identidade institucional ligada a vínculos históricos; contexto operacional vem das participações em Atividades, que derivam Projeto/Laboratório dentro da Unidade;
- **Fiscalização:** classificação explícita de produtos controlados.

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

Relatórios atualmente integrados:

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

Prévia, PDF e XLSX devem representar a mesma consulta e os mesmos filtros.

Pedidos entregues continuam sendo um recorte de Movimentações, não um relatório separado.

---

## Segurança — estado correto

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

Spec e plano: `docs/superpowers/specs/2026-10-27-autenticacao-corporativa-design.md` e `docs/superpowers/plans/2026-10-27-autenticacao-corporativa.md` (no repositório `sistema_sgl`).

### Autenticação corporativa

- `POST /api/v1/auth/login` valida login de rede (ou CPF, para colaboradores) e senha na `autenticacaoapi`, resolve a pessoa na Empregadoapi/Colaboradorapi, sincroniza o `Usuario` e devolve o JWT. Pessoa nova entra como `PENDENTE_VINCULACAO` e só acessa `/api/v1/auth/me` até um gestor da Unidade aprová-la; pessoa importada pela gestão entra direto.
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

A evolução do schema é responsabilidade do Flyway. Migrations aplicadas são imutáveis; novas alterações de banco devem usar uma nova versão.

Sequência atual:

```text
V1 ... V45   (V45 = identidade corporativa; preenche CNPMF com codigo_corporativo = 7)
```

---

## Execução local

```bash
cd backend/sgl-backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Variáveis usuais:

```text
DB_URL=jdbc:postgresql://localhost:5432/sgl
DB_USER=postgres
DB_PASSWD=<senha>
```

Acessos locais:

```text
API        http://localhost:8080
Swagger    http://localhost:8080/swagger-ui/index.html
OpenAPI    http://localhost:8080/v3/api-docs
```

Testes:

```bash
mvn test
```

---

## Sequência de trabalho

### Agora — pré-produção pós-aprovação

```text
Etapa 1 — padrão visual global                 ✅
Etapa 2 — Dark Mode definitivo                 ✅
Etapa 3 — refinamentos do fluxo de Resíduos    ✅
Etapa 4 — expansão operacional de Resíduos     ✅
Etapa 5 — Projetos e Atividades                ✅ concluída e validada
Etapa 6 — Estagiários e vínculos               🔧 6.1–6.4 concluídos; 6.5 frontend próximo
Etapas 7 a 13                                  ⏳ sequenciais
```

### Depois — roadmap formal para produção

```text
1. matriz/diretrizes de permissões
2. congelamento funcional
3. homologação integrada final
4. correção de falhas de homologação
5. autenticação + autorização + auditoria definitiva
6. integração corporativa / SSO / resolução confiável de Unidade
7. documentos/upload quando o contrato estiver definido
8. refactors técnicos planejados
```

O roadmap formal não foi descartado; ele apenas começa **depois** do bloco atual de pré-produção.

---

## Documentação

| Documento | Uso |
|---|---|
| [`CONTINUIDADE.md`](CONTINUIDADE.md) | checkpoint atual e regra de retomada |
| [`docs/DOSSIE_PROJETO_SGL.md`](docs/DOSSIE_PROJETO_SGL.md) | visão consolidada do sistema |
| [`docs/README.md`](docs/README.md) | índice e classificação documental |
| [`docs/MODULO_RESIDUOS.md`](docs/MODULO_RESIDUOS.md) | domínio de resíduos |
| [`docs/RELATORIOS.md`](docs/RELATORIOS.md) | relatórios atuais |
| [`docs/EXPORTACAO_RELATORIOS.md`](docs/EXPORTACAO_RELATORIOS.md) | exportações PDF/XLSX |
| [`docs/PENDENCIAS_POS_PROTOTIPO.md`](docs/PENDENCIAS_POS_PROTOTIPO.md) | pendências/refactors posteriores |

> Exemplos e documentos históricos são auxiliares. Em caso de conflito, prevalecem `main`, Swagger/OpenAPI e os documentos atuais indicados acima.

---

<div align="center">
  <strong>SGL — Sistema de Gestão de Laboratórios</strong><br/>
  Sistema funcionalmente aprovado em preparação para o ciclo formal de produção.
</div>

### Estado atual do domínio de Estagiários

A Etapa 6 já consolidou:

- `Usuario → Estagiario → VinculoEstagio 1..N`;
- múltiplas participações históricas em Atividades;
- Formação como enum institucional + `OUTRO`;
- Curso como catálogo administrável por Unidade;
- treinamento de segurança no vínculo, iniciado em `false` e concluído por ação explícita;
- Cultura como catálogo por Unidade e associação N:N com `VinculoEstagioAtividade`;
- respostas de vínculo/Estagiário com Atividade → SCI → Projeto → Laboratório e Culturas;
- ausência de um único “Laboratório do Estagiário” como fonte operacional definitiva.

Checkpoint atual: `docs/VALIDACAO_ETAPA_6_3.md`.

