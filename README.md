<a id="readme-top"></a>

<div align="center">
  <img src="docs/LOGO.png" alt="SGL Logo" width="400" height="auto">

# SGL — Sistema de Gestão de Laboratórios

**Backend corporativo para pedidos, estoque por lotes, rastreabilidade, resíduos, fiscalização, vínculos institucionais, cadastros e relatórios laboratoriais.**

`Java 17` · `Spring Boot 4.1` · `PostgreSQL` · `Flyway` · `Swagger/OpenAPI` · `PDF/XLSX`

</div>

---

## Estado atual — 04/09/2026

O SGL já passou pela aprovação funcional do primeiro protótipo e entra agora em um **bloco de ajustes de pré-produção**. Esse bloco acontece **antes** do roadmap formal de matriz de permissões, congelamento, homologação final e segurança definitiva.

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

> Para retomar o projeto, começar por [`CONTINUIDADE.md`](CONTINUIDADE.md) e [`docs/DOSSIE_PROJETO_SGL.md`](docs/DOSSIE_PROJETO_SGL.md). Para contratos HTTP, o Swagger/OpenAPI em execução continua sendo a fonte viva.

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
- **Estagiário:** vínculo institucional com Unidade/Laboratório e período;
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
Spring Security como base técnica                   ✅
sessão DEV no frontend                              ✅ temporária
isolamento operacional por Unidade                  ✅ desenvolvimento
guardas de rota por perfil                          ✅ UX
autenticação definitiva                             ⏳
autorização global no servidor                      ⏳
auditoria derivada da identidade autenticada        ⏳
integração corporativa/SSO                          ⏳
```

A configuração atual ainda permite requisições sem autenticação definitiva. Não tratar sessão DEV, perfil no frontend ou `X-SGL-Unidade-Id` como segurança final de produção.

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
V1 ... V12
```

---

## Execução local

```bash
cd backend/sgl-backend
mvn spring-boot:run
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
1. limpeza e atualização documental
2. levantamento dos ajustes de pré-produção
3. implementação/refinamento dos ajustes aprovados
4. estabilização desse bloco
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
