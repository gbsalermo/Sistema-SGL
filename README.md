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

## 📝 Licença

A definir.

---

## 👥 Contribuidores

- [PREENCHER]
