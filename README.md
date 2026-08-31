<a id="readme-top"></a>

<div align="center">
  <img src="docs/LOGO.png" alt="SGL Logo" width="400" height="auto">

# SGL — Sistema de Gestão de Laboratórios

**Backend corporativo para pedidos, estoque por lotes, rastreabilidade, fiscalização e relatórios de laboratórios.**

`Java 17` · `Spring Boot 4.1` · `PostgreSQL` · `Flyway` · `Swagger/OpenAPI` · `PDF/XLSX`

</div>

---

## 📍 Estado atual — 31/08/2026

O backend operacional do SGL está estável e os principais fluxos já estão integrados à `main`.

```text
Pedidos / urgência                                ✅
Estoque / lotes                                   ✅
FIFO / FEFO                                       ✅
Embalagem / multiplicador / fracionamento         ✅
Movimentações / rastreabilidade                   ✅
Swagger / OpenAPI                                 ✅
Fiscalização de produtos                          ✅
Relatórios                                        ✅
Exportação PDF/XLSX                               ✅
Resíduos                                          🟡 implementação em branch antiga; reconciliar
Autenticação/autorização/auditoria definitiva     ⏳ pós-frontend
```

No produto como um todo, a próxima grande etapa é **Administração/Cadastros no frontend**, começando por Produtos.

> Para outra IA ou pessoa retomar o projeto, começar por [`CONTINUIDADE.md`](CONTINUIDADE.md) e [`docs/DOSSIE_PROJETO_SGL.md`](docs/DOSSIE_PROJETO_SGL.md).

---

## 🎯 Objetivo

O SGL foi criado para substituir controles fragmentados de insumos por uma aplicação que represente o fluxo real do laboratório:

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
→ relatórios/fiscalização
```

O sistema mantém rastreabilidade do material físico por lote e evita que regras sensíveis sejam delegadas ao frontend.

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

Responsabilidades:

```text
Controller  → contrato HTTP e validação de entrada
Service     → regra de negócio, transação e orquestração
Repository  → persistência/consultas
Model       → estado e regras ligadas à entidade
DTO         → contrato público da API
```

### Identificadores

Regra consolidada:

```text
Long id
→ interno: banco, JPA, FKs, locks

UUID publicId
→ público: DTOs, endpoints e frontend
```

Novos contratos públicos não devem voltar a expor IDs sequenciais apenas porque exemplos históricos ou entidades internas ainda possuem `Long`.

---

## 🧬 Domínio principal

```text
Unidade
├── Laboratórios
│   ├── Usuários / Estagiários
│   ├── Projetos
│   └── Pedidos
└── Estoque Central
    └── Produto
        └── Lotes
            └── Movimentações
```

Conceitos importantes:

- **Produto:** catálogo e unidade-base de controle;
- **EstoqueCentral:** saldo consolidado por produto/unidade;
- **Lote:** quantidade física, validade, embalagem, multiplicador e rastreabilidade;
- **MovimentacaoEstoque:** trilha das alterações de saldo;
- **Pedido:** solicitação e ciclo de aprovação/entrega;
- **Fiscalização:** classificação explícita de produtos controlados.

---

## 📦 Estoque, FIFO e FEFO

Seleção de lotes:

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

### Embalagem e fracionamento

O saldo permanece em unidade-base.

Exemplo:

```text
2 kits × 50 reações = 100 reações
```

Formas de retirada atuais:

```text
UNITARIO
KIT
CAIXA
GARRAFA
GALAO
```

Fracionamento já aprovado:

```text
false → true  permitido
true  → false não permitido
```

---

## 🧾 Pedidos

Fluxo:

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

A aprovação realiza a baixa de estoque. A entrega apenas registra a conclusão. Cancelar um pedido já aprovado restaura os lotes exatos consumidos.

Urgência é atributo do pedido; não altera automaticamente FIFO/FEFO.

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

Campos principais:

```text
fiscalizado
orgaosFiscalizadores
observacaoFiscalizacao
```

Não inferir fiscalização a partir de risco químico ou perecibilidade.

---

## 📊 Relatórios

Relatórios concluídos:

```text
Estagiários
Produtos
Movimentações
Resumo operacional
Estoque e lotes
Fiscalização
```

**Pedidos entregues não é um relatório separado.** Esse recorte é obtido por Movimentações, usando origem `PEDIDO` e, quando aplicável, tipo `SAIDA`.

Resíduos está reservado como relatório futuro após integração do módulo correspondente.

Detalhes: [`docs/RELATORIOS.md`](docs/RELATORIOS.md).

---

## 📄 Exportação PDF/XLSX

A geração oficial fica no backend.

```text
prévia JSON
PDF
XLSX
→ mesma consulta e mesmos filtros
```

Bibliotecas:

```text
OpenPDF 2.0.5
Apache POI 5.5.1
```

Características dos arquivos:

- logo SGL;
- título, filtros e data de geração;
- resumo do relatório;
- A4 e orientação adaptada;
- paginação/quebra de texto no PDF;
- autofiltro, congelamento e configuração de impressão no XLSX.

Detalhes: [`docs/EXPORTACAO_RELATORIOS.md`](docs/EXPORTACAO_RELATORIOS.md).

---

## ♻️ Resíduos

Decisão de domínio:

```text
Produto ≠ Resíduo
```

Resíduo representa material gerado pelo laboratório e pode conter um ou vários reagentes/produtos sem alterar automaticamente o estoque desses produtos.

Existe uma implementação anterior em `feat/gestao-residuos`, porém em 31/08/2026 ela está divergente da `main` e precisa ser **reconciliada/portada**, não mergeada cegamente.

O procedimento detalhado está em [`CONTINUIDADE.md`](CONTINUIDADE.md) e no [`docs/DOSSIE_PROJETO_SGL.md`](docs/DOSSIE_PROJETO_SGL.md).

---

## 🔐 Autenticação e segurança

O projeto já possui dependências de Spring Security/OAuth2, mas isso não deve ser confundido com autenticação corporativa concluída.

Estado:

```text
base técnica de segurança                         ✅
login visual/sessão DEV no frontend               ✅
autenticação local definitiva                     ⏳
autorização real por perfil                       ⏳
auditoria derivada da sessão autenticada          ⏳
integração corporativa/SSO                        ⏳
```

Essa etapa permanece para depois do fechamento funcional principal do frontend.

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

### Pré-requisitos

```text
Java 17+
Maven 3.8+
PostgreSQL
Git
```

### Banco

```sql
CREATE DATABASE sgl;
```

Variáveis opcionais:

```text
DB_URL=jdbc:postgresql://localhost:5432/sgl
DB_USER=postgres
DB_PASSWD=<senha>
```

### Executar

```bash
cd backend/sgl-backend
mvn spring-boot:run
```

Acessos locais:

```text
API        http://localhost:8080
Swagger    http://localhost:8080/swagger-ui/index.html
OpenAPI    http://localhost:8080/v3/api-docs
```

### Testes

```bash
mvn test
```

Não use um número histórico fixo de testes como critério de saúde: a suíte cresce conforme o projeto evolui. O critério é build/testes atuais concluírem sem falhas.

---

## 🗺️ Planejamento atual

O backend principal já suporta os fluxos usados pelo frontend atual. A sequência do produto é:

```text
1. Administração / Cadastros no frontend            ← PRÓXIMO
   ├── Produtos + fiscalização
   ├── Laboratórios
   ├── Projetos
   ├── Usuários
   └── Estagiários
2. reconciliar/integrar Resíduos
3. relatório + PDF/XLSX de Resíduos
4. documentos/upload e rotulagem pendentes
5. dashboard final / alertas / robustez
6. autenticação + autorização + auditoria definitiva
7. integração corporativa
8. refactor pós-protótipo para nomenclatura técnica em inglês
```

Não criar CRUD manual de Unidade no frontend: a decisão vigente é que Unidade venha futuramente da integração corporativa.

---

## 📚 Documentação

| Documento | Uso |
|---|---|
| [`CONTINUIDADE.md`](CONTINUIDADE.md) | checkpoint e próximo passo |
| [`docs/DOSSIE_PROJETO_SGL.md`](docs/DOSSIE_PROJETO_SGL.md) | handoff completo para humano/IA |
| [`docs/README.md`](docs/README.md) | índice e hierarquia da documentação |
| [`docs/RELATORIOS.md`](docs/RELATORIOS.md) | relatórios e decisões de consulta |
| [`docs/EXPORTACAO_RELATORIOS.md`](docs/EXPORTACAO_RELATORIOS.md) | PDF/XLSX |
| [`docs/PENDENCIAS_POS_PROTOTIPO.md`](docs/PENDENCIAS_POS_PROTOTIPO.md) | refactors pós-protótipo |
| [`docs/FLUXO_DO_SISTEMA.md`](docs/FLUXO_DO_SISTEMA.md) | fluxo de domínio |
| [`docs/GUIA_ESTRUTURAL.md`](docs/GUIA_ESTRUTURAL.md) | organização arquitetural |

> Payloads e endpoints devem ser confirmados no Swagger antes de copiar exemplos históricos.

---

<div align="center">
  <strong>SGL — Sistema de Gestão de Laboratórios</strong><br/>
  Backend operacional estável; evolução do produto concentrada no frontend e nos módulos complementares planejados.
</div>
