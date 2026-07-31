# 📦 SGL — Sistema de Gestão de Laboratórios

[![Java](https://img.shields.io/badge/Java-17+-orange)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.x-green)](https://vuejs.org)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-blue)](https://www.postgresql.org)
[![Status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow)]()

## 📋 Sobre o Projeto

O **SGL** automatiza e centraliza o controle de materiais em laboratórios de pesquisa e ensino. O sistema organiza a operação em uma hierarquia institucional (**Unidade → Laboratório → Usuário**), vincula pedidos de material a projetos e pesquisadores, controla riscos e perecibilidade dos produtos, e mantém histórico completo de movimentações para fins de auditoria.

> ⚠️ **Projeto em fase de desenvolvimento (backend).**
> Este README dá uma visão geral do sistema. Para o histórico detalhado de decisões, pendências e o passo a passo de implementação, consulte [`CONTINUIDADE.md`](docs/CONTINUIDADE.md).

---

## 🧭 Em que pé está o desenvolvimento

O projeto está na fase de **backend**, com o núcleo de estoque já implementado e testado. O frontend (Vue.js) ainda não foi iniciado.

| Módulo | Status | Observação |
|---|:---:|---|
| Unidade (CRUD) | ✅ Concluído | Testado no Postman |
| Laboratório (CRUD) | ✅ Concluído | Testado no Postman |
| Usuário (CRUD + Perfil) | ✅ Concluído | Testado no Postman |
| Produto (catálogo central) | ✅ Concluído | Testado no Postman |
| EstoqueCentral (entrada/saída) | ✅ Concluído | Testado no Postman |
| Projeto | ✅ Concluído | Entity + DTO + Repository + Service + Controller |
| ItemPedido / Pedido | ✅ Concluído | Fluxo completo de aprovação/entrega/cancelamento implementado |
| HistoricoLaboratorio (histórico) | ✅ Implementado | Entity + DTO + Repository + Service + Controller implementados; dataRecebimento atualmente usa LocalDate (considerar migração para LocalDateTime) |
| Autenticação (Spring Security + JWT) | ⏳ Pendente | Planejado após validações e regras de negócio |
| Frontend (Vue.js) | ⬜ Não iniciado | Prioridade é consolidar e testar o backend primeiro |

**Próximo passo imediato:** avançar para validações de regras de negócio e autenticação (Spring Security + JWT). Consulte [CONTINUIDADE.md](docs/CONTINUIDADE.md) para detalhes.

Todas as decisões técnicas, correções aplicadas e o roadmap completo, etapa por etapa, estão documentados em [`CONTINUIDADE.md`](docs/CONTINUIDADE.md).

---

## 🏛️ Hierarquia do Sistema

```
UNIDADE (Instituição/Tenant)
   │
   ├──▶ ESTOQUE GERAL (EstoqueCentral — 1 por Unidade)
   │        Abastece os pedidos de usuários de TODOS os
   │        laboratórios daquela unidade
   │
   └──▶ LABORATÓRIO (cada unidade tem N laboratórios)
              │
              ├──▶ USUÁRIO (perfis: ADMINISTRADOR, GESTOR, TECNICO, PESQUISADOR, ESTAGIARIO)
              │        │
              │        ▼
              │     PEDIDO (usuário solicita materiais do Estoque Geral da Unidade)
              │        │        └─ opcionalmente marcado com um PROJETO do laboratório
              │        ▼
              │  ITEM PEDIDO ──▶ PRODUTO (catálogo central de materiais)
              │
              └──▶ PROJETO (vínculo opcional para o Pedido — não agrupa pedidos)
```

O **Produto** não pertence a um laboratório específico — ele é um catálogo central único. Quem controla "quanto tem disponível" é o **EstoqueCentral**, que é **único por Unidade** (o estoque geral daquela instituição) e atende pedidos de usuários de qualquer laboratório vinculado a ela. Quem registra "o que cada laboratório já recebeu" é o **HistoricoLaboratorio** (histórico/conferência). O **Projeto** não agrupa os pedidos do laboratório — é apenas uma informação opcional no Pedido, indicando se ele está ou não vinculado a um projeto específico daquele laboratório.

---

## ⚙️ Como o Sistema Funciona

### Fluxo de estoque

O SGL separa **estoque real** de **histórico de distribuição**, e essa é a decisão de design mais importante do projeto:

```
┌───────────────────────────────────────────────────────────┐
│              ESTOQUE CENTRAL (1 por Unidade)                │
│     Quantidade total disponível para distribuição entre    │
│     TODOS os laboratórios daquela Unidade                  │
│     Ex.: 10 unidades de Álcool 70% no total da Unidade      │
│     ↑↓ ÚNICO ponto do sistema com entrada/saída real        │
└───────────────────────────────────────────────────────────┘
                              │  Pedido aprovado
                              ▼  (baixa automática)
┌───────────────────────────────────────────────────────────┐
│                          PEDIDO                             │
│   Pesquisador solicita → Gestor aprova/rejeita → Entrega    │
└───────────────────────────────────────────────────────────┘
                              │  Material entregue
                              ▼  (registro de conferência)
┌───────────────────────────────────────────────────────────┐
│              HISTÓRICO LABORATÓRIO (histórico)               │
│   Apenas registra: "este lab recebeu X unidades em Y data"  │
│   Não tem entrada/saída própria — é só log de conferência    │
└───────────────────────────────────────────────────────────┘
```

**Por que separar assim?** Sem essa separação, "quantos vidros de reagente o Laboratório 1 recebeu este mês" e "quanto ainda existe no estoque geral" seriam a mesma pergunta, misturando controle de estoque com histórico de distribuição — o que dificulta tanto auditoria quanto relatórios gerenciais.

### Fluxo de um pedido, passo a passo

1. Um usuário (pesquisador/estagiário) cria um **Pedido**, com um ou mais **ItemPedido**, opcionalmente marcando um **Projeto** do laboratório ao qual o pedido está relacionado (esse vínculo é só informativo — o Projeto não agrupa nem controla os pedidos).
2. O sistema valida se cada produto solicitado possui registro no `EstoqueCentral`.
3. O pedido nasce com status `PENDENTE`.
4. Um usuário com perfil `GESTOR` ou `ADMINISTRADOR` aprova ou rejeita — na aprovação, a quantidade aprovada pode ser menor que a solicitada.
5. Ao aprovar, o sistema faz a **baixa automática** no `EstoqueCentral`.
6. Ao marcar como entregue, o sistema cria o registro correspondente no `HistoricoLaboratorio` (conferência).
7. Se um pedido aprovado for cancelado, o estoque é **devolvido** automaticamente ao `EstoqueCentral`.
8. Status segue sempre um único sentido: `PENDENTE → APROVADO/REJEITADO → ENTREGUE/CANCELADO`.

### Controle de risco e perecibilidade

Cada `Produto` carrega classificação de **nível de risco** (`NENHUM`, `BAIXO`, `MEDIO`, `ALTO`) e **tipo de risco** (`INFLAMAVEL`, `RADIOATIVO`, `TOXICO`, `CORROSIVO`, `BIOLOGICO`), pensado para o contexto real de laboratório — manuseio de reagentes, agentes biológicos e materiais perigosos exige esse nível de detalhe desde o cadastro. Produtos perecíveis (`VEGETAL`, `ANIMAL`, `MICROBIANO`, `QUIMICO`) contam com alerta de validade próxima via endpoint dedicado.

---

## 🧩 Padrões de Arquitetura

### Camadas e fluxo de comunicação

```
Frontend (Vue.js) → Controller → DTO → Service → Repository → Entity → PostgreSQL
```

<p align="center">
  <img src="docs/diagramas/diagrama-arquitetura.png" alt="Diagrama de camadas do SGL" width="380">
</p>

- O **DTO** é usado **apenas** entre `Controller` e `Service`. `Repository` e `Entity` nunca conhecem DTO.
- A conversão `Entity → DTO` acontece dentro do `Service`, via construtor no próprio DTO (`new UnidadeDTO(entity)`).
- A conversão `DTO → Entity` acontece dentro do `Service`, antes de chamar `repository.save()`.
- DTOs **não replicam relacionamentos bidirecionais** das entidades — a exposição é sempre "de cima para baixo" (ex.: `LaboratorioDTO` nunca devolve o objeto `Unidade` completo, só o `unidadeId`).

### Convenções adotadas

- **Lombok** (`@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`) para reduzir boilerplate em entidades e DTOs.
- **Injeção de dependência via construtor** com `@RequiredArgsConstructor` — sem uso de `@Autowired` em campo.
- **Transacionalidade:** métodos de escrita usam `@Transactional`; métodos de leitura usam `@Transactional(readOnly = true)`.
- **Exception handler global** (`@RestControllerAdvice`) mapeia `EntityNotFoundException` → 404, evitando erros 500 genéricos.
- **Validação em duas camadas:** Bean Validation (`@NotNull`, `@NotBlank`, `@Valid`) nos DTOs de entrada, e regras de negócio explícitas no Service.

---

## 🗂️ Diagramas do Projeto

### Diagrama de Classes (UML)

Visão completa das entidades, enums e seus relacionamentos.

<p align="center">
  <img src="docs/diagramas/diagrama-geral.png" alt="Diagrama de classes UML do SGL" width="700">
</p>

### Diagrama Entidade-Relacionamento (banco de dados)

Estrutura das tabelas, colunas, chaves primárias e estrangeiras conforme mapeado pelo JPA/Hibernate.

<p align="center">
  <img src="docs/diagramas/diagrama-er.png" alt="Diagrama ER do banco de dados do SGL" width="700">
</p>

---

## 📊 Modelo de Dados — Entidades Principais

| Entidade | Papel no sistema |
|---|---|
| **Unidade** | Instituição/tenant; agrupa laboratórios (`id`, `nome`, `sigla`) |
| **Laboratorio** | Vinculado a uma Unidade; possui um usuário responsável |
| **Usuario** | Login e perfil de acesso (`Perfil`: ADMINISTRADOR, GESTOR, TECNICO, PESQUISADOR, ESTAGIARIO); senha com BCrypt |
| **Produto** | Catálogo central de materiais — **não** pertence a um laboratório específico; carrega classificação de risco e perecibilidade |
| **EstoqueCentral** | Quantidade total disponível por produto (relação 1:1 com Produto); **único** ponto com entrada/saída real |
| **HistoricoLaboratorio** | Histórico/conferência do que cada laboratório recebeu — sem lógica própria de entrada/saída |
| **Projeto** | Agrupa pedidos de um laboratório; vínculo opcional no Pedido |
| **Pedido** | Solicitação feita por um Usuario, com status controlado (`StatusPedido`) |
| **ItemPedido** | Cada produto e quantidade dentro de um Pedido, com quantidade solicitada e aprovada |

---

## 📏 Principais Regras de Negócio

- Estoque nunca fica negativo — saída além do disponível lança exceção.
- Cada produto possui **um único** registro no `EstoqueCentral` (relação 1:1).
- Só usuários com perfil `GESTOR` ou `ADMINISTRADOR` aprovam ou rejeitam pedidos.
- Todo pedido nasce com status `PENDENTE`.
- `quantidadeAprovada` nunca é maior que `quantidadeSolicitada`.
- Cancelamento de um pedido já aprovado devolve a quantidade ao `EstoqueCentral`.
- Produto perecível exige data de validade; produto de risco `ALTO` exige descrição do risco.
- Exclusão de entidade "pai" com filhos vinculados (ex.: Unidade com Laboratórios) é bloqueada, retornando 409.

---

## 🔌 Endpoints Principais da API

<details>
<summary><strong>Unidades</strong></summary>

```
GET    /api/v1/unidades
POST   /api/v1/unidades
GET    /api/v1/unidades/{id}
PUT    /api/v1/unidades/{id}
DELETE /api/v1/unidades/{id}
```
</details>

<details>
<summary><strong>Laboratórios</strong></summary>

```
GET    /api/v1/laboratorios
POST   /api/v1/laboratorios
GET    /api/v1/laboratorios/{id}
PUT    /api/v1/laboratorios/{id}
GET    /api/v1/laboratorios/por-unidade?unidadeId=X
```
</details>

<details>
<summary><strong>Produtos (catálogo central)</strong></summary>

```
GET    /api/v1/produtos
POST   /api/v1/produtos
GET    /api/v1/produtos/{id}
PUT    /api/v1/produtos/{id}
DELETE /api/v1/produtos/{id}
GET    /api/v1/produtos/risco/{nivel}
GET    /api/v1/produtos/pereciveis
GET    /api/v1/produtos/validade-proxima?dias=30
```
</details>

<details>
<summary><strong>Estoque Central</strong></summary>

```
GET    /api/v1/estoque-central
GET    /api/v1/estoque-central/{id}
GET    /api/v1/estoque-central/produto/{produtoId}
POST   /api/v1/estoque-central
PUT    /api/v1/estoque-central/{id}
PUT    /api/v1/estoque-central/{id}/entrada
PUT    /api/v1/estoque-central/{id}/saida
DELETE /api/v1/estoque-central/{id}
GET    /api/v1/estoque-central/estoque-baixo
```
</details>

<details>
<summary><strong>Histórico Laboratório</strong></summary>

```
GET    /api/v1/historico-laboratorio
GET    /api/v1/historico-laboratorio/{id}
GET    /api/v1/historico-laboratorio/laboratorio/{labId}
GET    /api/v1/historico-laboratorio/produto/{produtoId}
GET    /api/v1/historico-laboratorio/pedido/{pedidoId}
POST   /api/v1/historico-laboratorio
```
</details>

<details>
<summary><strong>Pedidos</strong></summary>

```
GET    /api/v1/pedidos
GET    /api/v1/pedidos/{id}
GET    /api/v1/pedidos/por-usuario?usuarioId=X
GET    /api/v1/pedidos/por-status?status=X
POST   /api/v1/pedidos
PUT    /api/v1/pedidos/{id}/aprovar
PUT    /api/v1/pedidos/{id}/rejeitar
PUT    /api/v1/pedidos/{id}/entregar
DELETE /api/v1/pedidos/{id}
```
</details>

Lista completa e atualizada de endpoints em [`CONTINUIDADE.md`](docs/CONTINUIDADE.md).

---

## 🚀 Tecnologias

| Camada | Tecnologia |
|---|---|
| Frontend | Vue.js 3 *(ainda não iniciado)* |
| Backend | Java 17 + Spring Boot 4.1.0 |
| Banco de Dados | PostgreSQL 14+ (H2 em memória para desenvolvimento) |
| API | REST |
| Segurança (planejado) | Spring Security + JWT |
| Utilitários | Lombok, Bean Validation |

---

## 📁 Estrutura do Projeto

```
sgl/
├── backend/
│   ├── src/main/java/com/sgl/
│   │   ├── SglApplication.java
│   │   ├── controller/     # Endpoints REST
│   │   ├── service/        # Regras de negócio
│   │   ├── repository/     # Acesso a dados (Spring Data JPA)
│   │   ├── model/          # Entidades JPA
│   │   │   └── enums/      # NivelRisco, TipoRisco, TipoPerecivel, StatusPedido, Perfil
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── config/          # SecurityConfig e demais configurações
│   │   └── exception/       # Exception handler global
│   └── pom.xml
│
├── frontend/                # Vue.js (não iniciado)
│
├── docs/
│   ├── CONTINUIDADE.md              # Histórico completo de decisões e progresso
│   ├── codigos-referencia-pedidos.md
│   └── diagramas/                   # Diagramas UML, ER e de arquitetura
│
├── README.md
└── docker-compose.yml
```

---

## 🔧 Pré-requisitos

- Java 17+
- Node.js 18+ *(quando o frontend for iniciado)*
- PostgreSQL 14+
- Maven 3.8+

---

## 📦 Instalação e Execução

### Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

A aplicação sobe em `http://localhost:8080`. Em desenvolvimento, o banco H2 em memória é usado por padrão — console disponível em `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:sgldb`, usuário `sa`, sem senha).

### Banco de Dados (PostgreSQL — produção)

```bash
psql -U postgres -c "CREATE DATABASE sgl;"
psql -U postgres -d sgl -f database/init.sql
```

### Frontend

```bash
cd frontend
npm install
npm run serve
```
*(ainda não iniciado — estrutura reservada no repositório)*

---

## 🗺️ Roadmap Resumido

- [x] Etapa 1 — CRUDs básicos (Unidade, Laboratório, Usuário, Produto)
- [x] Etapa 2 — Estoque Central (entrada/saída, alertas)
- [x] Etapa 2.5 — Projeto
- [x] Etapa 3 — Pedido (fluxo completo de aprovação/entrega/cancelamento)
- [ ] Etapa 2 (continuação) — Histórico Laboratório (correção de campos pendente)
- [ ] Etapa 4 — Regras de negócio e validações finais
- [ ] Etapa 5 — Segurança e autenticação (Spring Security + JWT)
- [ ] Etapa 6 — Infraestrutura (PostgreSQL, scripts SQL de produção)
- [ ] Etapa 7 — Validação final do backend (testes ponta a ponta)
- [ ] Etapa 8 — Início do frontend (Vue.js)

Roadmap detalhado, com cada sub-tarefa e status, em [`CONTINUIDADE.md`](docs/CONTINUIDADE.md).

---

## 📚 Documentação Complementar

- [`CONTINUIDADE.md`](docs/CONTINUIDADE.md) — histórico de decisões, pendências e roadmap detalhado
- [`docs/codigos-referencia-pedidos.md`](docs/codigos-referencia-pedidos.md) — referência de implementação de Projeto, ItemPedido, Pedido e HistoricoLaboratorio
- Documentação da API (Swagger) — a criar

---

## 🌐 Estratégia de Ambientes

- **Fase atual:** separação via branches Git (`main` para produção/release, `develop`/`alpha` para desenvolvimento), com banco de dados único compartilhado enquanto o core é construído.
- **Fase futura:** bancos de dados separados por ambiente (alpha e produção), após o projeto base estar consolidado.
- **Infraestrutura em nuvem:** avaliação adiada até o sistema estar funcional de ponta a ponta.

---

## 📝 Licença

A definir.

## 👥 Contribuidores

- [PREENCHER]
