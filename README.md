# 📦 SGL - Sistema de Gestão de Laboratórios

[![Java](https://img.shields.io/badge/Java-17+-orange)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.x-green)](https://vuejs.org)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-blue)](https://www.postgresql.org)

## 📋 Sobre o Projeto

Sistema de gestão de laboratórios de pesquisa e ensino, desenvolvido para automatizar o controle de materiais, vincular estoque a projetos e pesquisadores, e armazenar documentos de forma segura.

> ⚠️ **Projeto em fase de desenvolvimento!**  
> Leia o arquivo [CONTINUIDADE.md](CONTINUIDADE.md) para entender o status atual.

---

## 🎯 Funcionalidades Principais

### Gestão de Estoque
- Cadastro de produtos com quantidade mínima
- Controle de entrada/saída de materiais
- Alertas automáticos de estoque baixo
- Consulta por laboratório

### Participantes de Projeto
- Cadastro de unidades (tenants)
- Cadastro de laboratórios
- Vinculação pesquisador ↔ laboratório ↔ projeto

### Armazenamento de Documentos
- Upload de pedidos e relatórios
- Download de documentos
- Organização por laboratório/pedido

---

## 🏗️ Arquitetura

```
┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│                  │     │                  │     │                  │
│    FRONTEND      │────▶│     BACKEND      │────▶│    DATABASE      │
│    (Vue.js)      │     │  (Spring Boot)   │     │   (PostgreSQL)   │
│                  │     │                  │     │                  │
└──────────────────┘     └──────────────────┘     └──────────────────┘
```

---

## 🚀 Tecnologias

| Camada | Tecnologia |
|--------|------------|
| Frontend | Vue.js 3 |
| Backend | Java 17 + Spring Boot 4.1.0 |
| Banco de Dados | PostgreSQL 14+ |
| API | REST |

---

## 📁 Estrutura do Projeto

```
sgl/
├── backend/           # API Spring Boot
├── frontend/          # Vue.js
├── database/          # Scripts SQL
├── docs/              # Documentação
├── CONTINUIDADE.md
├── README.md
└── docker-compose.yml
```

---

## 🔧 Pré-requisitos

- Java 17+
- Node.js 18+
- PostgreSQL 14+
- Maven 3.8+

---

## 📦 Instalação

### Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run serve
```

### Banco de Dados

```bash
# Criar banco de dados
psql -U postgres -c "CREATE DATABASE sgl;"

# Executar scripts
psql -U postgres -d sgl -f database/init.sql
```

---

## 📚 Documentação

- [CONTINUIDADE.md](docs/CONTINUIDADE.md) - Documento principal do projeto
- [API Documentation](docs/api.md) - (a criar)
- [Database Schema](docs/schema.md) - (a criar)

---

## 🔧 Correções Realizadas em Testes

### Data: 21/07/2026 - Correções de Validações

#### 1. Unidade de Medida: `@NotBlank` → `@NotNull`

**Problema:** O campo `unidadeMedida` estava usando `@NotBlank`, que não é aplicável a enums.

**Solução:** Alterado para `@NotNull` no `ProdutoDTO`.

**Motivo:**
- `@NotBlank` só funciona com `String` - valida que a string não é nula, vazia ou só espaços
- `@NotNull` funciona com qualquer tipo, incluindo enums - valida que o campo não é nula

**Arquivo alterado:** `backend/src/main/java/com/sgl/dto/ProdutoDTO.java`

```java
// ANTES (incorreto):
@NotBlank(message = "unidade de medida é obrigatória")
private String unidadeMedida;

// DEPOIS (correto):
@NotNull(message = "unidade de medida é obrigatória")
private String unidadeMedida;
```

**Lembrete:** Para campos do tipo `enum`, sempre usar `@NotNull` em vez de `@NotBlank`.

---

#### 2. Endpoint `/validade-proxima` - Implementação Faltante

**Problema:** Durante a etapa de testes do CRUD de Produtos no Postman, foi verificado que o endpoint `GET /api/v1/produtos/validade-proxima` não estava implementado, conforme documentado na documentação da API.

**Solução:** Adicionar implementação em 3 arquivos:

**Arquivo 1:** `ProdutoRepository.java` - Adicionar query customizada:
```java
@Query("SELECT p FROM Produto p WHERE p.perecivel = true AND p.dataValidade BETWEEN :dataAtual AND :dataLimite")
List<Produto> findPereciveisComValidadeProxima(@Param("dataAtual") LocalDate dataAtual, @Param("dataLimite") LocalDate dataLimite);
```

**Arquivo 2:** `ProdutoService.java` - Adicionar método:
```java
@Transactional(readOnly = true)
public List<ProdutoDTO> listarValidadeProxima(int dias) {
    LocalDate dataAtual = LocalDate.now();
    LocalDate dataLimite = dataAtual.plusDays(dias);
    return produtoRepository.findPereciveisComValidadeProxima(dataAtual, dataLimite)
            .stream()
            .map(ProdutoDTO::new)
            .toList();
}
```

**Arquivo 3:** `ProdutoController.java` - Adicionar endpoint:
```java
@GetMapping("/validade-proxima")
public ResponseEntity<List<ProdutoDTO>> listarValidadeProxima(@RequestParam(defaultValue = "30") int dias) {
    return ResponseEntity.ok(produtoService.listarValidadeProxima(dias));
}
```

**Uso:**
- `GET /api/v1/produtos/validade-proxima` → produtos com validade nos próximos 30 dias
- `GET /api/v1/produtos/validade-proxima?dias=7` → produtos com validade nos próximos 7 dias

---

---

## 📝 Licença

A definir.

---

## 👥 Contribuidores

- [PREENCHER]
