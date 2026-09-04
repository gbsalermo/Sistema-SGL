<a id="readme-top"></a>

<div align="center">
  <img src="docs/LOGO.png" alt="SGL Logo" width="400" height="auto">

# SGL — Sistema de Gestão de Laboratórios

**Backend corporativo para pedidos, estoque por lotes, rastreabilidade, resíduos, fiscalização, cadastros e relatórios laboratoriais.**

`Java 17` · `Spring Boot 4.1` · `PostgreSQL` · `Flyway` · `Swagger/OpenAPI` · `PDF/XLSX`

</div>

---

## 📍 Estado atual — 03/09/2026

O backend do primeiro protótipo está funcionalmente amplo e integrado à `main`. Desde o último handoff foram concluídos o módulo de Resíduos, os cadastros administrativos necessários ao frontend, o fluxo completo de Estagiários e novas consultas/relatórios institucionais.

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
Relatório + PDF/XLSX de Resíduos                  ✅
Estagiários — cadastro/edição/encerramento        ✅
Pessoas por laboratório + PDF/XLSX                ✅
Suporte a Administração/Cadastros                 ✅
Alteração administrativa de perfil                ✅
Código SGL de Resíduos no registro inicial        ✅
Autenticação/autorização definitiva               ⏳
Integração corporativa/SSO                        ⏳
Refactor técnico para inglês                      ⏳ pós-protótipo
```

No produto como um todo, o frontend também concluiu **Administração/Cadastros, Resíduos, dashboards, alertas operacionais, busca global e tema claro/escuro**. O próximo bloco oficial do primeiro protótipo é consolidar as diretrizes/matriz de permissões, congelar o protótipo e executar a homologação completa.

> Para retomar o projeto, começar por [`CONTINUIDADE.md`](CONTINUIDADE.md) e [`docs/DOSSIE_PROJETO_SGL.md`](docs/DOSSIE_PROJETO_SGL.md). Para contratos HTTP, o Swagger/OpenAPI continua sendo a fonte viva.

---

## 🎯 Objetivo

O SGL representa o ciclo operacional de laboratórios de forma rastreável:

```text
estrutura institucional
→ catálogo de produtos
→ estoque central
→ entrada de lotes
→ solicitação de material
→ aprovação
→ baixa física FIFO/FEFO
→ entrega
→ movimentações auditáveis
→ geração e destinação de resíduos
→ relatórios/fiscalização
→ administração dos dados-base
```

Regras sensíveis permanecem no backend; o frontend não deve recriar FIFO/FEFO, transições de domínio ou cálculos oficiais de relatórios.

---

## 🏛️ Arquitetura

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

```text
Controller  → contrato HTTP e validação de entrada
Service     → regra de negócio, transação e orquestração
Repository  → persistência/consultas
Model       → estado e regras ligadas à entidade
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

## 🧬 Domínio principal

```text
Unidade
├── Laboratórios
│   ├── Usuários
│   ├── Estagiários
│   ├── Projetos
│   ├── Pedidos
│   └── Resíduos gerados
└── Estoque Central
    └── Produto
        └── Lotes
            └── Movimentações
```

Conceitos centrais:

- **Produto:** catálogo e unidade-base de controle;
- **EstoqueCentral:** saldo consolidado por produto/unidade;
- **Lote:** quantidade física, validade, embalagem, multiplicador e rastreabilidade;
- **MovimentacaoEstoque:** trilha das alterações de saldo;
- **Pedido:** solicitação e ciclo de aprovação/entrega;
- **Resíduo:** material gerado no laboratório e encaminhado à Gestão;
- **Fiscalização:** classificação explícita de produtos controlados;
- **Estagiário:** vínculo institucional auditável com unidade/laboratório e período.

---

## 📦 Estoque, FIFO e FEFO

```text
produto perecível     → FEFO
produto não perecível → FIFO
```

Invariantes:

```text
EstoqueCentral.quantidadeAtual = soma operacional dos lotes
aprovação baixa estoque
entrega não baixa novamente
cancelamento aprovado restaura os lotes exatos
lote vencido não é elegível para aprovação
movimentação registra o lote realmente utilizado
```

Formas de retirada atuais:

```text
UNITARIO
KIT
CAIXA
GARRAFA
GALAO
```

Fracionamento aprovado:

```text
false → true  permitido
true  → false não permitido
```

---

## 🧾 Pedidos

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

A aprovação realiza a baixa de estoque. A entrega registra a conclusão sem nova baixa. Cancelar pedido já aprovado restaura os lotes exatos consumidos. Urgência é atributo do pedido e não altera automaticamente FIFO/FEFO.

---

## ♻️ Resíduos — integrado

Decisão de domínio:

```text
Produto ≠ Resíduo
```

Um resíduo pode citar produtos/reagentes em sua composição sem alterar automaticamente EstoqueCentral, Lote ou Movimentação.

Fluxo:

```text
INFORMADO
→ EM_ANALISE
→ LIBERADO_PARA_ARMAZENAMENTO
→ ARMAZENADO_TEMPORARIAMENTE
→ DESPACHADO
```

O módulo possui:

- cadastro pelo laboratório/solicitante;
- consulta “Meus resíduos” por gerador;
- recebimento e análise pela Gestão;
- risco informado x risco confirmado;
- Código SGL `SGL-RES-AAAA-NNNNNN`;
- rótulo e histórico de transições;
- armazenamento temporário e despacho;
- relatório com filtros;
- exportação PDF/XLSX.

Migrations atuais chegam a:

```text
V11__create_residuo_module.sql
V12__backfill_codigo_sgl_residuos.sql
```

Detalhes: [`docs/MODULO_RESIDUOS.md`](docs/MODULO_RESIDUOS.md).

---

## 👥 Estagiários e estrutura institucional

O backend suporta:

```text
listar
cadastrar
editar
vincular à unidade/laboratório
registrar período e tipo de vínculo
encerrar estágio com data efetiva
consultar ativos
consultar por laboratório
```

O relatório **Pessoas por laboratório** reúne responsáveis e demais usuários vinculados, incluindo dados específicos do estágio quando aplicável, e também possui PDF/XLSX.

---

## 🛡️ Fiscalização de produtos

Produtos podem ser explicitamente classificados como fiscalizados por:

```text
POLICIA_FEDERAL
VIGILANCIA_SANITARIA
ANVISA
EXERCITO
OUTRO
```

Campos:

```text
fiscalizado
orgaosFiscalizadores
observacaoFiscalizacao
```

Não inferir fiscalização por risco químico ou perecibilidade.

---

## 📊 Relatórios

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

**Pedidos entregues não é relatório separado.** Esse recorte é obtido por Movimentações, normalmente com origem `PEDIDO` e tipo `SAIDA`.

Detalhes: [`docs/RELATORIOS.md`](docs/RELATORIOS.md) e [`docs/EXPORTACAO_RELATORIOS.md`](docs/EXPORTACAO_RELATORIOS.md).

---

## 🧑‍💼 Administração / Cadastros

O backend já oferece o suporte usado pela central administrativa do frontend para:

```text
Laboratórios
Projetos
Produtos
Permissões/perfis de usuários existentes
```

Decisões vigentes:

- **Unidade não terá CRUD manual normal no frontend**; a origem futura será corporativa;
- **Usuário não é cadastrado manualmente pela central administrativa**; a futura autenticação institucional deve criar/sincronizar esse cadastro;
- a Administração pode alterar o perfil de usuários existentes por contrato específico;
- Produto em Cadastros representa catálogo, não estoque/lotes.

---

## 🔐 Segurança — interpretação correta

O projeto possui base de Spring Security/OAuth2, mas a autenticação corporativa definitiva ainda não está concluída.

```text
base técnica de segurança                         ✅
regras de perfil em serviços específicos          ✅ parcial
sessão DEV no frontend                            ✅ temporária
autenticação definitiva                           ⏳
autorização global baseada em sessão/token        ⏳
auditoria derivada da identidade autenticada      ⏳
integração corporativa/SSO                        ⏳
```

Não tratar a sessão DEV ou validações pontuais de perfil como segurança final de produção.

---

## 🛠️ Tecnologias

| Área | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 4.1.0 |
| Persistência | Spring Data JPA / Hibernate |
| Banco | PostgreSQL |
| Migrations | Flyway |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Validação | Bean Validation |
| Segurança base | Spring Security / OAuth2 Client |
| PDF | OpenPDF 2.0.5 |
| XLSX | Apache POI 5.5.1 |
| Testes | Spring Boot Test / JUnit / H2 |
| Build | Maven |

---

## 🚀 Execução local

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

## 🗺️ Planejamento atual

Sem criar novo roadmap, o fechamento do primeiro protótipo segue:

```text
1. consolidar diretrizes/matriz de permissões
2. congelar o primeiro protótipo funcional
3. executar homologação completa integrada
4. corrigir apenas falhas encontradas na estabilização
5. fechar autenticação + autorização + auditoria definitiva
6. integrar autenticação/unidade corporativa
7. tratar documentos/upload ainda pendentes quando o contrato for definido
8. refactor pós-protótipo para nomenclatura técnica em inglês
```

Funcionalidades futuras como **modelos de resíduos pré-determinados** continuam registradas como opção em estudo e não fazem parte do fluxo atual obrigatório.

---

## 📚 Documentação

| Documento | Uso |
|---|---|
| [`CONTINUIDADE.md`](CONTINUIDADE.md) | checkpoint e próximo passo |
| [`docs/DOSSIE_PROJETO_SGL.md`](docs/DOSSIE_PROJETO_SGL.md) | handoff completo |
| [`docs/README.md`](docs/README.md) | índice documental |
| [`docs/MODULO_RESIDUOS.md`](docs/MODULO_RESIDUOS.md) | domínio e fluxo de resíduos |
| [`docs/RELATORIOS.md`](docs/RELATORIOS.md) | relatórios atuais |
| [`docs/EXPORTACAO_RELATORIOS.md`](docs/EXPORTACAO_RELATORIOS.md) | PDF/XLSX |
| [`docs/PENDENCIAS_POS_PROTOTIPO.md`](docs/PENDENCIAS_POS_PROTOTIPO.md) | refactors posteriores |

> Payloads e endpoints devem ser confirmados no Swagger antes de copiar exemplos históricos.

---

<div align="center">
  <strong>SGL — Sistema de Gestão de Laboratórios</strong><br/>
  Primeiro protótipo funcional próximo do congelamento e da homologação completa.
</div>
