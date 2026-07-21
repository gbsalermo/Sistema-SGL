# 📦 Projeto SGL - Sistema de Gestão de Laboratórios

## 📋 Status do Projeto
**Fase:** Desenvolvimento - CRUD Produto implementado (pendente teste no Postman)  
**Data de início:** 13/07/2026  
**Última atualização:** 21/07/2026  

---

## 🎯 Visão Geral

**Nome do projeto:** SGL (Sistema de Gestão de Laboratórios)  
**Tipo:** Sistema de Gestão de Laboratórios  
**Objetivo:** Automatizar e centralizar o controle de materiais em laboratórios de pesquisa/ensino  
**Arquitetura de Estoque:** EstoqueCentral (estoque real - entrada/saída) + EstoqueLaboratorio (conferência/histórico)

---

## 💡 Conceito e Diferencial

### O que é o projeto?
Sistema completo para gestão de estoque de laboratórios, controlando entrada/saída de materiais, vinculando estoque a projetos e pesquisadores, com armazenamento seguro de documentos (pedidos, relatórios, etc.).

### Por que é diferente?
- **Foco em laboratórios** - Não é um genérico de estoque, é específico para o contexto de pesquisa/ensino
- **Vinculação com projetos** - Cada pedido está atrelado a um projeto e pesquisador
- **Hierarquia organizacional** - Respeita a estrutura: Unidade → Laboratório → Pesquisador
- **Armazenamento de documentos** - Guarda pedidos, relatórios e comprovantes
- **Base sólida para expansão** - API REST preparada para novas funcionalidades
- **Estoque inteligente** - Controle centralizado + histórico por laboratório

### Hierarquia do Sistema
```
┌─────────────────────────────────────────────────────────────┐
│                        UNIDADE                              │
│                    (Tenant/Instituição)                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      LABORATÓRIO                            │
│                  (Cada unidade tem N labs)                  │
└─────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
┌─────────────────────────┐       ┌─────────────────────────┐
│   ESTUDANTE/PESQUISADOR │       │      PRODUTO/ITEM       │
│    (Usuários do lab)    │       │    (Catálogo central)   │
└─────────────────────────┘       └─────────────────────────┘
              │                               │
              ▼                               ▼
┌─────────────────────────────────────────────────────────────┐
│                         PEDIDO                              │
│  (Pesquisador solicita material)                            │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    DOCUMENTOS                               │
│      (Pedidos, relatórios, comprovantes armazenados)       │
└─────────────────────────────────────────────────────────────┘
```

### Fluxo de Estoque (Nova Arquitetura)
```
┌─────────────────────────────────────────────────────────────┐
│                     ESTOQUE CENTRAL                         │
│        (Quantidade total disponível para distribuição)      │
│        Ex: 10 Álcool 70% disponíveis no total               │
│        ↑↓ Entrada/Saída/Atualização                         │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ Pedido aprovado
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                         PEDIDO                              │
│  (Pesquisador solicita material)                            │
│  (Ao aprovar: baixa automática no EstoqueCentral)           │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ Material entregue
                              ▼
┌─────────────────────────────────────────────────────────────┐
│               ESTOQUE LABORATÓRIO (Conferência)             │
│        (Apenas registrou: "lab recebeu X unidades")         │
│        (Não tem entrada/saída - é só histórico)             │
│        Ex: Lab1 recebeu 2 álcools em 21/07/2026             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🏗️ Arquitetura Técnica

### Stack Tecnológica

| Camada | Tecnologia | Justificativa |
|--------|------------|---------------|
| **Frontend** | Vue.js | Moderno, reativo, boa comunidade |
| **Backend** | Java Spring Boot | robusto, escalável, ecoistema maduro |
| **Banco de Dados** | PostgreSQL | Confiável, JSON support, open source |
| **API** | REST | Padrão de mercado, fácil integração |
| **Storage** | Local/S3 (futuro) | Armazenamento de documentos |

### Arquitetura Geral
```
┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│                  │     │                  │     │                  │
│    FRONTEND      │────▶│     BACKEND      │────▶│    DATABASE      │
│    (Vue.js)      │     │  (Spring Boot)   │     │   (PostgreSQL)   │
│                  │     │                  │     │                  │
└──────────────────┘     └──────────────────┘     └──────────────────┘
       │                         │                         │
       │                         │                         │
       ▼                         ▼                         ▼
   Navegador               API REST                    Dados
```

### Estrutura de Pastas
```
sgl/
├── backend/                    # API Spring Boot
│   ├── src/main/java/
│   │   └── com.sgl/
│   │       ├── SglApplication.java
│   │       ├── controller/     # Endpoints REST
│   │       ├── service/        # Lógica de negócio
│   │       ├── repository/     # Acesso a dados
│   │       ├── model/          # Entidades JPA
│   │       │   └── enums/      # Enums (NivelRisco, TipoRisco, etc)
│   │       ├── dto/            # Data Transfer Objects (EM USO)
│   │       ├── config/         # Configurações
│   │       └── exception/      # Tratamento de exceções
│   └── pom.xml
│
├── frontend/                   # Vue.js
│   ├── src/
│   │   ├── components/         # Componentes reutilizáveis
│   │   ├── views/              # Páginas
│   │   ├── services/           # Chamadas à API
│   │   ├── store/              # Estado global
│   │   └── router/             # Rotas
│   └── package.json
│
├── docs/                       # Documentação
│   └── CONTINUIDADE.md         # Este arquivo
│
└── docker-compose.yml          # Orquestração (opcional)
```

### Fluxo de Estoque (Nova Arquitetura)
```
┌─────────────────────────────────────────────────────────────┐
│                     ESTOQUE CENTRAL                         │
│        (Quantidade total disponível para distribuição)      │
│        Ex: 10 Álcool 70% disponíveis no total               │
│        ↑↓ Entrada/Saída/Atualização                         │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ Pedido aprovado
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                         PEDIDO                              │
│  (Pesquisador solicita material)                            │
│  (Ao aprovar: baixa automática no EstoqueCentral)           │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ Material entregue
                              ▼
┌─────────────────────────────────────────────────────────────┐
│               ESTOQUE LABORATÓRIO (Conferência)             │
│        (Apenas registrou: "lab recebeu X unidades")         │
│        (Não tem entrada/saída - é só histórico)             │
│        Ex: Lab1 recebeu 2 álcools em 21/07/2026             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🏛️ Padrões de Arquitetura

### Fluxo de Comunicação (DTO Layer)
```
Controller <-> Service <-> Repository <-> Entity
         DTO              Entity           (banco)
```
- **DTO é usado APENAS entre Controller e Service.** Repository e Entity nunca conhecem DTO.
- A conversão `Entity -> DTO` acontece dentro do Service, via construtor no DTO (ex: `new UnidadeDTO(entity)`).
- A conversão `DTO -> Entity` acontece dentro do Service antes de chamar `repository.save()`.

### Fluxo de Estoque
```
┌─────────────────┐
│  EstoqueCentral │ ← ÚNICO com entrada/saída
└────────┬────────┘
         │
         │ Pedido aprovado
         ▼
┌─────────────────┐
│     Pedido      │ ← Baixa automática no EstoqueCentral
└────────┬────────┘
         │
         │ Material entregue
         ▼
┌─────────────────┐
│EstoqueLaboratorio│ ← Apenas conferência/histórico
└─────────────────┘
```

### Regras de Relacionamento em DTOs
- DTOs **não** replicam relacionamentos bidirecionais das Entities.
- O sentido de exposição é sempre "de cima para baixo" (ex: `UnidadeDTO` pode eventualmente expor uma lista de `LaboratorioDTO`, mas `LaboratorioDTO` nunca traz `UnidadeDTO` completo — no máximo um campo `unidadeId`).
- Campos de relacionamento em Entity devem ser marcados com `@ToString.Exclude` e `@EqualsAndHashCode.Exclude` (Lombok) para evitar recursão infinita.
- **EstoqueLaboratorio** usa DTOs para expor `laboratorioId`, `produtoId` e `pedidoId`, não os objetos completos.
- **EstoqueCentral** usa DTO para expor `produtoId`, não o objeto Produto completo.

### Convenções de Código
- **Lombok:** `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor` nas entidades e DTOs para reduzir boilerplate.
- **Injeção de dependência:** via construtor com `@RequiredArgsConstructor` (sem uso de `@Autowired` em campo).
- **Transacionalidade:** métodos de escrita (salvar, atualizar, deletar) usam `@Transactional`; métodos de leitura usam `@Transactional(readOnly = true)`.
- **Campo `sigla`:** na entidade Unidade, identifica a instituição (ex: "IB", "IF", "IQ"). Deve ser único.
- **EstoqueCentral:** cada produto tem UM registro (OneToOne com Produto). ÚNICO com entrada/saída.
- **EstoqueLaboratorio:** apenas conferência/histórico (sem entrada/saída). Registra que o lab recebeu material.

---

## 📊 Modelo de Dados (Entidades Principais)

### Unidade (Tenant)
```java
- id (auto-gerado)
- nome
- sigla
```

### Laboratório
```java
- id
- unidade (ManyToOne → Unidade)
- nome
- descricao
- responsavel (ManyToOne → Usuario)  // ID do responsável
- ativo
```

### Estudante/Pesquisador → **USUARIO (substituído)**
```java
// ANTIGA entidade Estudante/Pesquisador - REMOVIDA
// Substituída por Usuario com enum Perfil (ver abaixo)
```

### Perfil (enum)
```java
public enum Perfil {
    ADMINISTRADOR, // Acesso total ao sistema
    GESTOR,        // Gerencia lab e aprova pedidos
    TECNICO,       // Técnico de laboratório
    PESQUISADOR,   // Acesso a pedidos e relatórios do lab
    ESTAGIARIO     // Só pode fazer pedidos
}
```

### Usuario
```java
- id (auto-gerado)
- nome
- email (único, usado para login)
- senha (criptografada com BCrypt)
- perfil (enum: ESTAGIARIO, PESQUISADOR, PROFESSOR, GESTOR, ADMIN)
- laboratorio_id (FK → Laboratorio)
- ativo
```

### Produto/Item (Catálogo Central)
```java
- id
- nome
- descricao
- codigo_referencia
- unidade_medida
- localizacao_fisica
- ativo

// Campos de risco e perecibilidade
- risco (enum: NENHUM, BAIXO, MEDIO, ALTO)
- tipo_risco (enum: NENHUM, INFLAMAVEL, RADIOATIVO, TOXICO, CORROSIVO, BIOLOGICO)
- descricao_risco (texto livre para detalhes específicos, ex: "Material radioativo classe 3")
- perecivel (boolean)
- dias_validade (inteiro, opcional - para alertas de validade)
- tipo_perecivel (enum: NENHUM, VEGETAL, ANIMAL, MICROBIANO, QUIMICO)
- condicoes_armazenamento (texto livre, ex: "Armazenar em freezer -20°C")

// IMPORTANTE: Produto NÃO tem laboratorio_id
// Produto é um catálogo central - existe uma única vez
// O estoque por laboratório é controlado pela entidade EstoqueLaboratorio
```

### EstoqueCentral (Estoque Total Disponível)
```java
- id
- produto_id (FK → Produto)  // Único por produto
- quantidade_atual           // Quantidade total disponível para distribuição
- quantidade_minima          // Alerta quando atingir (estoque central baixo)
- ativo

// REGRA: Cada produto tem UM registro no EstoqueCentral
// Exemplo: EstoqueCentral(produto_id=1, quantidade_atual=10)
// Significa: temos 10 unidades do produto 1 disponíveis para distribuir
```

### EstoqueLaboratorio (Histórico/Conferência)
```java
- id
- laboratorio_id (FK → Laboratorio)
- produto_id (FK → Produto)
- quantidade                  // Quantidade que este lab recebeu
- data_recebimento            // Data em que o material chegou ao lab
- pedido_id (FK → Pedido)     // Referência ao pedido que originou
- ativo

// IMPORTANTE: EstoqueLaboratorio é apenas para CONFERÊNCIA/HISTÓRICO
// Ele NÃO tem entrada/saída/updates de estoque
// Apenas registrou: "este lab recebeu X unidades do produto Y na data Z"
// Para saber "quantos álcools foram pro lab1 este mês": WHERE laboratorio_id = X AND data_recebimento BETWEEN...
// O estoque REAL fica apenas no EstoqueCentral
```

### Pedido
```java
- id
- estudante_id (FK)
- laboratorio_id (FK)
- data_solicitacao
- status (PENDENTE, APROVADO, REJEITADO, ENTREGUE)
- observacao
- arquivo_documento (URL/path do documento)
```

### ItemPedido (itens do pedido)
```java
- id
- pedido_id (FK)
- produto_id (FK)
- quantidade_solicitada
- quantidade_aprovada
```

### Projeto
```java
- id
- laboratorio_id (FK)
- nome
- descricao
- data_inicio
- data_fim
- responsavel
- ativo
```

---

## ⚙️ Funcionalidades

### MVP (Mínimo Viável)

#### Gestão de Estoque
- [ ] Cadastro de produtos no catálogo central (sem vinculação a lab)
- [ ] Controle de estoque central (quantidade total disponível) - ÚNICO com entrada/saída
- [ ] Distribuição de estoque para laboratórios (via pedido)
- [ ] Consulta de histórico por laboratório (EstoqueLaboratorio)
- [ ] Alertas automáticos de estoque baixo (apenas EstoqueCentral)
- [ ] Classificação de risco dos produtos (Nenhum/Baixo/Médio/Alto)
- [ ] Tipo de risco (Inflamável/Radioativo/Tóxico/Corrosivo/Biológico)
- [ ] Cadastro de produtos perecíveis com controle de validade
- [ ] Alertas de validade próxima

#### Participantes
- [ ] Cadastro de unidades (tenants)
- [ ] Cadastro de laboratórios
- [ ] Cadastro de estudantes/pesquisadores
- [ ] Vinculação pesquisador ↔ laboratório
- [ ] Vinculação pesquisador ↔ projeto

#### Pedidos
- [ ] Solicitação de material pelo pesquisador
- [ ] Verificação de estoque disponível no EstoqueCentral
- [ ] Aprovação/rejeição pelo responsável
- [ ] Baixa automática no EstoqueCentral ao aprovar
- [ ] Criação de registro no EstoqueLaboratorio ao entregar
- [ ] Histórico de pedidos

#### Armazenamento de Documentos
- [ ] Upload de pedidos (PDF, imagem)
- [ ] Upload de relatórios
- [ ] Download de documentos
- [ ] Organização por laboratório/pedido

### Funcionalidades Futuras
- [ ] Relatórios gerenciais
- [ ] Exportação (PDF, Excel)
- [ ] Notificações por email
- [ ] Dashboard com gráficos
- [ ] Integração com sistemas externos
- [ ] Controle de validade de produtos
- [ ] Código de barras/QR Code
- [ ] Relatório de riscos por laboratório
- [ ] Histórico de manuseio de materiais perigosos

---

## ⚠️ Controle de Risco e Perecibilidade

### Níveis de Risco
| Nível | Descrição | Exemplos |
|-------|-----------|----------|
| **NENHUM** | Sem risco identificado | Materiais de escritório, vidrarias comuns |
| **BAIXO** | Risco mínimo, manuseio padrão | Solventes diluídos, materiais biológicos inativos |
| **MÉDIO** | Requer cuidados específicos | Inflamáveis concentrados, materiais biológicos ativos |
| **ALTO** | Requer protocolo especial | Radioativos, materiais altamente tóxicos, agentes patogênicos |

### Tipos de Risco
| Tipo | Descrição |
|------|-----------|
| **INFLAMAVEL** | Materiais que pegam fogo facilmente |
| **RADIOATIVO** | Emissores de radiação (requer blindagem) |
| **TOXICO** | Tóxicos para contato/ingestão/inalação |
| **CORROSIVO** | Danificam materiais e tecidos |
| **BIOLOGICO** | Agentes biológicos (bactérias, vírus, fungos) |

### Tipos de Perecibilidade
| Tipo | Descrição |
|------|-----------|
| **VEGETAL** | Plantas, extratos vegetais, culturas de tecidos |
| **ANIMAL** | Tecidos animais, soro, antígenos |
| **MICROBIANO** | Bactérias, vírus, fungos, leveduras |
| **QUIMICO** | Compostos químicos instáveis |

### Regras de Negócio para Risco/Perecibilidade
1. **Produtos de risco ALTO** exigem confirmação extra antes de liberar pedido
2. **Produtos perecíveis** têm alerta automático de validade (configurável)
3. **Produtos radioativos** podem exigir campos adicionais (atividade, data-calibração)
4. **Relatório de risco** disponível para gestores (por lab, por tipo)

### Regras de Negócio para Estoque
1. **EstoqueCentral** é o ÚNICO que controla entrada/saída de materiais
2. **EstoqueLaboratorio** é apenas para CONFERÊNCIA/HISTÓRICO (registrou que o lab recebeu)
3. **Ao aprovar pedido**: baixa automática no EstoqueCentral
4. **Ao entregar material**: cria registro no EstoqueLaboratorio (conferência)
5. **Alertas**: estoque baixo apenas no EstoqueCentral
6. **Consultas**: "quantos álcools foram pro lab1?" → WHERE laboratorio_id = X AND data_recebimento BETWEEN...
7. **Diferença**: EstoqueCentral = estoque real | EstoqueLaboratorio = log/histórico

---

## 🔌 Endpoints da API (Exemplos)

### Unidades
```
GET    /api/v1/unidades           - Listar unidades
POST   /api/v1/unidades           - Criar unidade
GET    /api/v1/unidades/{id}      - Buscar unidade
PUT    /api/v1/unidades/{id}      - Atualizar unidade
DELETE /api/v1/unidades/{id}      - Remover unidade
```

### Laboratórios
```
GET    /api/v1/laboratorios                  - Listar laboratórios
POST   /api/v1/laboratorios                  - Criar laboratório
GET    /api/v1/laboratorios/{id}             - Buscar laboratório
PUT    /api/v1/laboratorios/{id}             - Atualizar laboratório
GET    /api/v1/unidades/{id}/laboratorios    - Listar labs de uma unidade
```

### Produtos (Catálogo Central)
```
GET    /api/v1/produtos                      - Listar produtos
POST   /api/v1/produtos                      - Cadastrar produto
GET    /api/v1/produtos/{id}                 - Buscar produto por ID
PUT    /api/v1/produtos/{id}                 - Atualizar produto
DELETE /api/v1/produtos/{id}                 - Remover produto
GET    /api/v1/produtos/risco/{nivel}        - Filtrar por nível de risco
GET    /api/v1/produtos/pereciveis           - Listar perecíveis
GET    /api/v1/produtos/validade-proxima     - Alertas de validade
```

### EstoqueCentral (Estoque Total)
```
GET    /api/v1/estoque-central               - Listar estoque central
GET    /api/v1/estoque-central/{id}          - Buscar por ID
GET    /api/v1/estoque-central/produto/{produtoId} - Estoque de um produto
POST   /api/v1/estoque-central               - Cadastrar estoque central
PUT    /api/v1/estoque-central/{id}          - Atualizar quantidade
GET    /api/v1/estoque-central/estoque-baixo - Listar com estoque baixo
```

### EstoqueLaboratorio (Estoque por Lab)
```
GET    /api/v1/estoque-laboratorio                    - Listar todo estoque
GET    /api/v1/estoque-laboratorio/{id}               - Buscar por ID
GET    /api/v1/estoque-laboratorio/laboratorio/{labId} - Estoque de um lab
GET    /api/v1/estoque-laboratorio/produto/{produtoId} - Onde está o produto
POST   /api/v1/estoque-laboratorio                    - Cadastrar estoque lab
PUT    /api/v1/estoque-laboratorio/{id}               - Atualizar quantidade
GET    /api/v1/estoque-laboratorio/estoque-baixo      - Listar com estoque baixo
```

### Pedidos
```
GET    /api/v1/pedidos                       - Listar pedidos
POST   /api/v1/pedidos                       - Criar pedido
PUT    /api/v1/pedidos/{id}/aprovar          - Aprovar pedido
PUT    /api/v1/pedidos/{id}/rejeitar         - Rejeitar pedido
PUT    /api/v1/pedidos/{id}/entregar         - Marcar como entregue
```

### Documentos
```
POST   /api/v1/pedidos/{id}/documentos       - Upload documento
GET    /api/v1/pedidos/{id}/documentos       - Listar documentos
GET    /api/v1/documentos/{id}/download      - Download documento
```

---

## 📝 Decisões Tomadas

| Data | Decisão | Motivo |
|------|---------|--------|
| 13/07/2026 | Criar projeto | Ideia inicial do app de estoque |
| 13/07/2026 | Java Spring Boot | Base sólida, escalável, preparado para expansão |
| 13/07/2026 | Vue.js no frontend | Moderno, reativo, fácil integração com API |
| 13/07/2026 | PostgreSQL | Confiável, suporte a JSON, open source |
| 13/07/2026 | Modelo multi-tenant | Unidade = Tenant, permite múltiplas instituições |
| 16/07/2026 | Classificação de risco | Laboratórios lidam com materiais perigosos (radioativos, inflamáveis, biológicos) |
| 16/07/2026 | Controle de perecibilidade | Produtos como plantas, bactérias e frutos precisam de controle de validade |
| 16/07/2026 | Ambientes via branches Git primeiro | Simplicidade inicial, banco único enquanto o core é construído |
| 16/07/2026 | Bancos separados por ambiente como evolução | Após base do projeto estar pronta |
| 16/07/2026 | Nuvem/volumes adiados | Prioridade é ter o projeto base funcionando antes de pensar em infraestrutura de produção |
| 16/07/2026 | Spring Boot 4.1.0 | Versão mais recente, com melhorias de performance e segurança |
| 17/07/2026 | Adoção da camada DTO | Controle de acesso por papel (ADMIN/GESTAO vs PARTICIPANTE_LAB), evita LazyInitializationException, evita loop de serialização infinito, desacopla API do modelo de banco, segurança (evita vazar campo senha) |
| 17/07/2026 | Renomear projeto para SGL | Nome mais preciso: "Sistema de Gestão de Laboratórios" |
| 17/07/2026 | Simplificar entidade Unidade | Removidos campos `codigo` e `ativo`, adicionado `sigla` (único). Motivo: evitar confusão entre dois códigos diferentes e manter apenas atributos essenciais (id, nome, sigla) |
| 17/07/2026 | Validação nos DTOs | Uso de `@NotNull`, `@NotBlank` + `@Valid` no controller para garantir dados obrigatórios antes de chegar ao service |
| 17/07/2026 | Exception handler global | `@RestControllerAdvice` no package `exception` para mapear `EntityNotFoundException` → 404, evita erros 500 genéricos |
| 17/07/2026 | Endpoint listarPorUnidade | Implementado `GET /api/v1/laboratorios/por-unidade?unidadeId=X` conforme documentado no CONTINUIDADE |
| 17/07/2026 | DataInitializer para testes | `CommandLineRunner` no package `test` injeta 3 unidades e 5 laboratórios ao iniciar a aplicação |
| 17/07/2026 | Perfil como enum, não entity | Decidido usar `enum Perfil` (ESTAGIARIO, PESQUISADOR, PROFESSOR, GESTOR, ADMIN) em vez de tabela separada. Motivo: perfis são fixos e bem definidos, enum simplifica o código (menos 4 arquivos), não precisa de CRUD para perfis |
| 17/07/2026 | Substituir Estudante/Pesquisador por Usuario | A entidade "Estudante/Pesquisador" foi substituída por "Usuario" com campo `perfil` (enum) e `senha` (BCrypt). Mais flexível e preparado para autenticação futura |
| 20/07/2026 | Laboratorio.responsavel como Usuario | Campo `responsavel` alterado de `String` para `ManyToOne<Usuario>`. Permite vincular um usuário existente como responsável pelo laboratório |
| 20/07/2026 | Correção do DataInitializer | Laboratórios criados primeiro com responsavel null, depois usuários criados, e por fim responsáveis atribuídos aos laboratórios |
| 20/07/2026 | Ordem de implementação: Enum primeiro | Para entidades que usam enums, criar os enums ANTES das entidades. Ex: Produto precisa de Risco, TipoRisco, TipoPerecivel antes de ser criado |
| 21/07/2026 | Nova arquitetura de estoque | Removido `laboratorio_id` de Produto. Criadas entidades EstoqueCentral (estoque total disponível) e EstoqueLaboratorio (conferência/histórico). Motivo: Produto é catálogo central, não pertence a um lab específico. Estoque por lab é apenas log de conferência |
| 21/07/2026 | EstoqueCentral como entidade separada | Cada produto tem UM registro no EstoqueCentral com a quantidade total disponível. É o ÚNICO que tem entrada/saída. Motivo: Controle centralizado do estoque |
| 21/07/2026 | EstoqueLaboratorio é apenas conferência | EstoqueLaboratorio apenas registrou que o lab recebeu material (sem entrada/saída). Motivo: Histórico para consultas como "quantos álcools foram pro lab1 este mês?" |
| 21/07/2026 | CRUD de Produto implementado | Entidade com 14 campos (nome, descricao, codigoReferencia, unidadeMedida, localizacaoFisica, risco, tipoRisco, descricaoRisco, perecivel, dataValidade, tipoPerecivel, condicoesArmazenamento, ativo). DTO com validações. Service com 8 métodos. Controller com 8 endpoints REST. 6 produtos de teste no DataInitializer |

---

## 🚧 Em Andamento

- [x] Definição da ideia principal (13/07/2026)
- [x] Definição dos 3 pilares (13/07/2026)
- [x] Escolha das tecnologias (13/07/2026)
- [x] Criação da estrutura base Spring Boot (16/07/2026)
- [x] Configurar application.properties (16/07/2026)
- [x] Configurar SecurityConfig para H2 Console (16/07/2026)
- [x] Criar entidade Unidade (17/07/2026)
- [x] Criar entidade Laboratório (17/07/2026)
- [x] Criar entidade Estudante/Pesquisador (17/07/2026)
- [x] Criar entidade Produto (17/07/2026)
- [x] Criar entidade Pedido (17/07/2026)
- [x] Criar entidade ItemPedido (17/07/2026)
- [x] Criar entidade Projeto (17/07/2026)
- [x] Implementar DTO para Unidade (17/07/2026)
- [x] CRUD de Unidade com DTO (17/07/2026)
- [x] Simplificar entidade Unidade - remover codigo/ativo, adicionar sigla (17/07/2026)
- [x] Implementar DTO para Laboratório (17/07/2026)
- [x] CRUD de Laboratório com DTO (17/07/2026)
- [x] Adicionar validação nos DTOs com @NotNull/@NotBlank (17/07/2026)
- [x] Criar exception handler global com @RestControllerAdvice (17/07/2026)
- [x] Implementar endpoint listarPorUnidade (17/07/2026)
- [x] Criar DataInitializer para injetar dados de teste (17/07/2026)
- [x] Testar CRUD de Unidade no Postman (17/07/2026)
- [x] Testar CRUD de Laboratório no Postman (17/07/2026)
- [x] Validar comportamento DELETE com integridade referencial (17/07/2026)
- [x] Definir nova estratégia: Usuario + enum Perfil (17/07/2026)
- [x] Implementar enum Perfil (17/07/2026)
- [x] Implementar entidade Usuario (17/07/2026)
- [x] UsuarioDTO (corrigido em 20/07/2026)
- [x] UsuarioRepository (20/07/2026)
- [x] UsuarioService (20/07/2026)
- [x] UsuarioController (20/07/2026)
- [x] Atualizar DataInitializer com usuarios de teste (20/07/2026)
- [x] Alterar Laboratorio.responsavel de String para Usuario (20/07/2026)
- [x] Corrigir DataInitializer para usar Usuario como responsavel (20/07/2026)
- [x] Atualizar diagrama UML (20/07/2026)
- [x] Nova arquitetura de estoque: Produto sem laboratorio_id, EstoqueCentral e EstoqueLaboratorio (21/07/2026)
- [x] Clarificação: EstoqueLaboratorio é apenas conferência/histórico (21/07/2026)
- [x] Implementar CRUD de Produtos (catálogo central - sem laboratorio_id) (21/07/2026)
- [x] Criar ProdutoController com endpoints REST (21/07/2026)
- [x] Adicionar 6 produtos de teste no DataInitializer (21/07/2026)
- [ ] Testar CRUD de Produtos no Postman
- [ ] Prototipação das telas

### Próximas Implementações (curto prazo)
- [ ] Testar CRUD de Produtos no Postman
- [ ] CRUD de EstoqueCentral (estoque total - ÚNICO com entrada/saída)
- [ ] CRUD de EstoqueLaboratorio (apenas conferência/histórico)
- [ ] CRUD de Pedidos (com baixa automática no EstoqueCentral)
- [ ] Atualizar DataInitializer com estoque e pedidos de teste

---

## ✅ Concluído

- [x] Criação da pasta do projeto (13/07/2026)
- [x] Criação do arquivo de continuidade (13/07/2026)
- [x] Definição da arquitetura (13/07/2026)
- [x] Definição do modelo de dados (13/07/2026)
- [x] Definição de controle de risco e perecibilidade (16/07/2026)
- [x] Definição de estratégia de ambientes (16/07/2026)
- [x] Criação da estrutura base Spring Boot (16/07/2026)
- [x] Configuração do application.properties (16/07/2026)
- [x] Configuração do SecurityConfig para H2 Console (16/07/2026)
- [x] Criação da entidade Unidade (17/07/2026)
- [x] Criação da entidade Laboratório (17/07/2026)
- [x] Criação da entidade Estudante/Pesquisador (17/07/2026)
- [x] Criação da entidade Produto (17/07/2026)
- [x] Criação da entidade Pedido (17/07/2026)
- [x] Criação da entidade ItemPedido (17/07/2026)
- [x] Criação da entidade Projeto (17/07/2026)
- [x] Implementação do padrão DTO (UnidadeDTO) (17/07/2026)
- [x] CRUD de Unidade com DTO funcional (17/07/2026)
- [x] Simplificação da entidade Unidade (id, nome, sigla) (17/07/2026)
- [x] Renomeação do projeto para SGL (17/07/2026)
- [x] Revisão e correção de integridade do código (17/07/2026)
- [x] Migração de package `com.sgl` para `com.sgl` (17/07/2026)
- [x] Renomeação de `sgl-backend` para `sgl-backend` (17/07/2026)
- [x] Implementação do padrão DTO (LaboratorioDTO) (17/07/2026)
- [x] CRUD de Laboratório com DTO funcional (17/07/2026)
- [x] Adição de validações nos DTOs (17/07/2026)
- [x] Criação do exception handler global (17/07/2026)
- [x] Implementação do endpoint listarPorUnidade (17/07/2026)
- [x] Criação do DataInitializer para dados de teste (17/07/2026)
- [x] Testes de CRUD de Unidade no Postman - todos OK (17/07/2026)
- [x] Testes de CRUD de Laboratório no Postman - todos OK (17/07/2026)
- [x] Validação do DELETE com foreign key - identificado e documentado (17/07/2026)
- [x] Definição da nova estratégia de Usuário com enum Perfil (17/07/2026)
- [x] Substituição da entidade Estudante/Pesquisador por Usuario (17/07/2026)
- [x] Implementação do enum Perfil (17/07/2026)
- [x] Implementação da entidade Usuario (17/07/2026)
- [x] Criação do UsuarioDTO (parcial - ver pendências) (17/07/2026)
- [x] Correção do UsuarioDTO (20/07/2026)
- [x] Implementação do UsuarioRepository (20/07/2026)
- [x] Implementação do UsuarioService (20/07/2026)
- [x] Implementação do UsuarioController (20/07/2026)
- [x] Atualização do DataInitializer com 5 usuários de teste (20/07/2026)
- [x] Testes de CRUD de Usuário no Postman (20/07/2026)
- [x] Definição da nova arquitetura de estoque (21/07/2026)
- [x] Remoção de laboratorio_id de Produto (21/07/2026)
- [x] Definição de EstoqueCentral como entidade separada (21/07/2026)
- [x] Definição de EstoqueLaboratorio como conferência/histórico (21/07/2026)
- [x] Clarificação: EstoqueLaboratorio é apenas log, não gestão de estoque (21/07/2026)
- [x] Implementar ProdutoDTO com validações (21/07/2026)
- [x] Implementar ProdutoRepository com queries customizadas (21/07/2026)
- [x] Implementar ProdutoService com CRUD completo (21/07/2026)
- [x] Implementar ProdutoController com endpoints REST (21/07/2026)
- [x] Corrigir bug na rota /risco/{nivel} no ProdutoController (21/07/2026)
- [x] Adicionar 6 produtos de teste no DataInitializer (21/07/2026)

---

## ✅ Pendências do UsuarioDTO (RESOLVIDO em 20/07/2026)

| Item | Status | Descrição |
|------|--------|-----------|
| Construtor Entity→DTO | ✅ CORRIGIDO | Mapeia `id, nome, email, perfil, laboratorioId, ativo` |
| Campo `senha` | ✅ CORRIGIDO | Senha não volta no construtor Entity→DTO |
| Campo `unidadeId` | ✅ CORRIGIDO | Removido (unidade via laboratorio.unidade) |
| Validação `email` | ✅ CORRIGIDO | `@NotBlank` + `@Email` |
| Validação `senha` | ✅ CORRIGIDO | `@NotBlank` |
| Validação `perfil` | ✅ CORRIGIDO | `@NotNull` |

### Correções necessárias no UsuarioDTO

```java
// REMOVER campo:
private Long unidadeId;  // ← remover (Usuario não tem unidade direta)

// REMOVER do construtor:
this.senha = entity.getSenha();  // ← senha nunca volta no DTO

// ADICIONAR no construtor:
this.perfil = entity.getPerfil();
this.laboratorioId = entity.getLaboratorio() != null ? entity.getLaboratorio().getId() : null;

// ADICIONAR validações:
@NotBlank(message = "email é obrigatório")
@Email(message = "email inválido")
private String email;

@NotBlank(message = "senha é obrigatória")
private String senha;

@NotNull(message = "perfil é obrigatório")
private Perfil perfil;
```

---

## ❌ Bloqueios e Pendências

| Item | Descrição | Responsável | Status | Prioridade |
|------|-----------|-------------|--------|------------|
| ~~Exception customizada~~ | ~~Substituir `RuntimeException` genérica por `RecursoNaoEncontradoException` com `@RestControllerAdvice` global~~ | Dev | **Resolvido** | ~~Média~~ |
| ~~DELETE com foreign key~~ | ~~Unidade não pode ser deletada se tem laboratórios vinculados. Necessário tratar `DataIntegrityViolationException` no exception handler para retornar 409~~ | Dev | **Identificado** - pendente implementação do handler | Média |
| Spring Boot 4.1.0 | Versão pode não existir ainda (máxima estável é 3.x). Verificar e corrigir se necessário. | Dev | Verificar | Média |
| EstoqueCentral | Implementar entidade para estoque total disponível (ÚNICO com entrada/saída) | Dev | Pendente | Alta |
| EstoqueLaboratorio | Implementar entidade para conferência/histórico (sem entrada/saída) | Dev | Pendente | Alta |
| Validação de estoque | Implementar regras de negócio para baixa automática no EstoqueCentral ao aprovar pedido | Dev | Pendente | Alta |

---

## 📌 Próximos Passos

### Curto Prazo
1. ~~Implementar CRUD de Laboratório com DTO~~ **(CONCLUÍDO)**
2. ~~Criar exception handler global~~ **(CONCLUÍDO)**
3. ~~DataInitializer para testes~~ **(CONCLUÍDO)**
4. ~~Testar CRUD no Postman~~ **(CONCLUÍDO)**
5. ~~Implementar enum Perfil~~ **(CONCLUÍDO)**
6. ~~Implementar entidade Usuario~~ **(CONCLUÍDO)**
7. ~~Corrigir UsuarioDTO~~ **(CONCLUÍDO)**
8. ~~Criar UsuarioRepository~~ **(CONCLUÍDO)**
9. ~~Criar UsuarioService~~ **(CONCLUÍDO)**
10. ~~Criar UsuarioController~~ **(CONCLUÍDO)**
11. ~~Atualizar DataInitializer com usuarios de teste~~ **(CONCLUÍDO)**
12. ~~Alterar Laboratorio.responsavel para Usuario~~ **(CONCLUÍDO)**
13. ~~Implementar CRUD de Produtos~~ **(CONCLUÍDO)**
14. **Testar CRUD de Produtos no Postman**
15. **Implementar CRUD de EstoqueCentral** (estoque total - ÚNICO com entrada/saída)
16. **Implementar CRUD de EstoqueLaboratorio** (apenas conferência/histórico)
17. **Implementar CRUD de Pedidos** (com baixa automática no EstoqueCentral)
18. **Tratar DELETE com foreign key** - adicionar `DataIntegrityViolationException` no handler
19. **Criar banco de dados** - Scripts SQL das tabelas

---

## 📋 PASSO A PASSO - IMPLEMENTAÇÃO PRODUTO, ESTOQUE E PEDIDO

### ORDEM CORRETA (Enum → Entidade → DTO → Repository → Service → Controller)

---

### PRODUTO (Catálogo Central - SEM laboratorio_id)

#### Passo 1: Criar Enums
- [x] Criar `model/enums/NivelRisco.java` (NENHUM, BAIXO, MEDIO, ALTO) ✅ JÁ EXISTE
- [x] Criar `model/enums/TipoRisco.java` (NENHUM, INFLAMAVEL, RADIOATIVO, TOXICO, CORROSIVO, BIOLOGICO) ✅ JÁ EXISTE
- [x] Criar `model/enums/TipoPerecivel.java` (NENHUM, VEGETAL, ANIMAL, MICROBIANO, QUIMICO) ✅ JÁ EXISTE

#### Passo 2: Criar Entidade
- [x] Criar `model/Produto.java` (usa os 3 enums acima, SEM laboratorio_id) ✅ CONCLUÍDO
- [x] IMPORTANTE: Produto NÃO tem relationship com Laboratorio

#### Passo 3: Criar DTO
- [x] Criar `dto/ProdutoDTO.java` (SEM campo laboratorioId) ✅ CONCLUÍDO

#### Passo 4: Criar Repository
- [x] Criar `repository/ProdutoRepository.java` ✅ CONCLUÍDO

#### Passo 5: Criar Service
- [x] Criar `service/ProdutoService.java` (SEM validação de laboratorio) ✅ CONCLUÍDO

#### Passo 6: Criar Controller
- [x] Criar `controller/ProdutoController.java` ✅ CONCLUÍDO

#### Passo 6.1: Adicionar dados de teste
- [x] Adicionar 6 produtos de teste no DataInitializer ✅ CONCLUÍDO

---

### ESTOQUE CENTRAL (Estoque Total - ÚNICO com Entrada/Saída)

#### Passo 7: Criar Entidade
- [ ] Criar `model/EstoqueCentral.java` (produto_id único, quantidade_atual, quantidade_minima)

#### Passo 8: Criar DTO
- [ ] Criar `dto/EstoqueCentralDTO.java`

#### Passo 9: Criar Repository
- [ ] Criar `repository/EstoqueCentralRepository.java`

#### Passo 10: Criar Service
- [ ] Criar `service/EstoqueCentralService.java`

#### Passo 11: Criar Controller
- [ ] Criar `controller/EstoqueCentralController.java`

---

### ESTOQUE LABORATÓRIO (Apenas Conferência/Histórico)

#### Passo 12: Criar Entidade
- [ ] Criar `model/EstoqueLaboratorio.java` (laboratorio_id, produto_id, quantidade, dataRecebimento, pedido_id)

#### Passo 13: Criar DTO
- [ ] Criar `dto/EstoqueLaboratorioDTO.java`

#### Passo 14: Criar Repository
- [ ] Criar `repository/EstoqueLaboratorioRepository.java`

#### Passo 15: Criar Service
- [ ] Criar `service/EstoqueLaboratorioService.java`

#### Passo 16: Criar Controller
- [ ] Criar `controller/EstoqueLaboratorioController.java`

---

### PEDIDO (Com Baixa Automática no EstoqueCentral)

#### Passo 17: Criar Enum
- [ ] Criar `model/StatusPedido.java` (PENDENTE, APROVADO, REJEITADO, ENTREGUE, CANCELADO)

#### Passo 18: Criar Entidades
- [ ] Criar `model/Pedido.java` (usa StatusPedido, Usuario, Laboratorio, Projeto)
- [ ] Criar `model/ItemPedido.java` (usa Pedido, Produto)

#### Passo 19: Criar DTOs
- [ ] Criar `dto/PedidoDTO.java`
- [ ] Criar `dto/ItemPedidoDTO.java`

#### Passo 20: Criar Repositories
- [ ] Criar `repository/PedidoRepository.java`
- [ ] Criar `repository/ItemPedidoRepository.java`

#### Passo 21: Criar Service
- [ ] Criar `service/PedidoService.java`
- [ ] IMPORTANTE: Ao aprovar pedido, baixar automaticamente do EstoqueCentral

#### Passo 22: Criar Controller
- [ ] Criar `controller/PedidoController.java`

---

### DATA INITIALIZER

#### Passo 23: Atualizar DataInitializer
- [ ] Adicionar produtos de teste no `DataInitializer.java`
- [ ] Adicionar estoque central de teste
- [ ] Adicionar pedidos de teste
- [ ] Adicionar registros de conferência no EstoqueLaboratorio (após entregas)

---

## 📋 CÓDIGOS DE REFERÊNIA

### Enums para Produto (já existem em model/enums/)

#### NivelRisco.java ✅ JÁ EXISTE
```java
package com.sgl.model.enums;

public enum NivelRisco {
    NENHUM,
    BAIXO,
    MEDIO,
    ALTO
}
```

#### TipoRisco.java ✅ JÁ EXISTE
```java
package com.sgl.model.enums;

public enum TipoRisco {
    NENHUM,
    INFLAMAVEL,
    RADIOATIVO,
    TOXICO,
    CORROSIVO,
    BIOLOGICO
}
```

#### TipoPerecivel.java ✅ JÁ EXISTE
```java
package com.sgl.model.enums;

public enum TipoPerecivel {
    NENHUM,
    VEGETAL,
    ANIMAL,
    MICROBIANO,
    QUIMICO
}
```

### Entidade Produto (Catálogo Central - SEM laboratorio_id)
```java
package com.sgl.model;

// Campos:
// id, nome, descricao, codigoReferencia,
// unidadeMedida, localizacaoFisica,
// risco (NivelRisco enum), tipoRisco (TipoRisco enum), descricaoRisco,
// perecivel (Boolean), diasValidade, tipoPerecivel (TipoPerecivel enum),
// condicoesArmazenamento, ativo

// IMPORTANTE: NÃO tem laboratorio_id
// Produto é catálogo central - existe uma única vez
```

### Entidade EstoqueCentral (Estoque Total - ÚNICO com Entrada/Saída)
```java
package com.sgl.model;

// Campos:
// id, produto (OneToOne → Produto), 
// quantidadeAtual, quantidadeMinima, ativo

// IMPORTANTE: EstoqueCentral é o ÚNICO que tem entrada/saída de estoque
// Cada produto tem UM registro no EstoqueCentral
// Controla a quantidade total disponível para distribuição
// Quando aprovado pedido: baixa automática (quantidadeAtual -= quantidadeSolicitada)
```

### Entidade EstoqueLaboratorio (Apenas Conferência/Histórico)
```java
package com.sgl.model;

// Campos:
// id, laboratorio (ManyToOne → Laboratorio), 
// produto (ManyToOne → Produto),
// quantidade, dataRecebimento, pedido (ManyToOne → Pedido), ativo

// IMPORTANTE: EstoqueLaboratorio é apenas para CONFERÊNCIA/HISTÓRICO
// Ele NÃO tem entrada/saída/updates de estoque
// Apenas registrou: "este lab recebeu X unidades do produto Y na data Z via pedido W"
// Para saber "quantos álcools foram pro lab1 este mês": WHERE laboratorio_id = X AND data_recebimento BETWEEN...
// O estoque REAL fica apenas no EstoqueCentral
```

### Enum para Pedido

#### StatusPedido.java
```java
package com.sgl.model;

public enum StatusPedido {
    PENDENTE,
    APROVADO,
    REJEITADO,
    ENTREGUE,
    CANCELADO
}
```

### Entidade Pedido
```java
package com.sgl.model;

// Campos:
// id, usuario (ManyToOne), laboratorio (ManyToOne),
// projeto (ManyToOne, opcional), dataSolicitacao,
// status (StatusPedido enum), observacao, arquivoDocumento
```

### Entidade ItemPedido
```java
package com.sgl.model;

// Campos:
// id, pedido (ManyToOne), produto (ManyToOne),
// quantidadeSolicitada, quantidadeAprovada
```

---

## 📋 ENDPOINTS PARA TESTAR

### Produtos (Catálogo Central)
```
GET    /api/v1/produtos                      - Listar todos
GET    /api/v1/produtos/{id}                 - Buscar por ID
POST   /api/v1/produtos                      - Criar
PUT    /api/v1/produtos/{id}                 - Atualizar
DELETE /api/v1/produtos/{id}                 - Deletar
GET    /api/v1/produtos/risco/{nivel}        - Listar por risco
GET    /api/v1/produtos/pereciveis           - Listar perecíveis
GET    /api/v1/produtos/validade-proxima     - Alertas de validade
```

### EstoqueCentral (Estoque Total - ÚNICO com Entrada/Saída)
```
GET    /api/v1/estoque-central               - Listar todo estoque
GET    /api/v1/estoque-central/{id}          - Buscar por ID
GET    /api/v1/estoque-central/produto/{produtoId} - Estoque de um produto
POST   /api/v1/estoque-central               - Criar
PUT    /api/v1/estoque-central/{id}          - Atualizar (entrada/saída)
GET    /api/v1/estoque-central/estoque-baixo - Listar com estoque baixo
```

### EstoqueLaboratorio (Apenas Conferência/Histórico)
```
GET    /api/v1/estoque-laboratorio                    - Listar todo histórico
GET    /api/v1/estoque-laboratorio/{id}               - Buscar por ID
GET    /api/v1/estoque-laboratorio/laboratorio/{labId} - Histórico de um lab
GET    /api/v1/estoque-laboratorio/produto/{produtoId} - Onde foi o produto
GET    /api/v1/estoque-laboratorio/pedido/{pedidoId}   - Itens de um pedido
POST   /api/v1/estoque-laboratorio                    - Criar registro (após entrega)
```

### Pedidos
```
GET    /api/v1/pedidos                       - Listar todos
GET    /api/v1/pedidos/{id}                  - Buscar por ID
GET    /api/v1/pedidos/por-usuario?usuarioId=X - Listar por usuário
GET    /api/v1/pedidos/por-status?status=X   - Listar por status
POST   /api/v1/pedidos                       - Criar
PUT    /api/v1/pedidos/{id}/aprovar          - Aprovar
PUT    /api/v1/pedidos/{id}/rejeitar         - Rejeitar
PUT    /api/v1/pedidos/{id}/entregar         - Entregar
DELETE /api/v1/pedidos/{id}                 - Deletar
```

---

## 🎓 Guia: Criando o Projeto Spring Boot (Spring Initializr)

### Passo 1: Acessar o Spring Initializr
- Acesse: https://start.spring.io/
- Alternativa: https://spring.io/initializr

### Passo 2: Configurar o Projeto
Preencha os campos:
- **Project:** Maven
- **Language:** Java
- **Spring Boot:** 4.1.0
- **Group:** `com.sgl`
- **Artifact:** `sgl-backend`
- **Name:** `sgl-backend`
- **Description:** Gestão de Estoque para Laboratórios
- **Package name:** `com.sgl`
- **Packaging:** Jar
- **Java:** 17

### Passo 3: Selecionar Dependências
Adicione as seguintes dependências:

**Core:**
- Spring Web (para APIs REST)
- Spring Data JPA (para acesso ao banco)
- Validation (para validação de dados)

**Banco de Dados:**
- PostgreSQL Driver
- H2 Database (para testes locais)

**Segurança:**
- Spring Security
- OAuth2 Resource Server (para JWT futuro)

**Outros:**
- Lombok (para reduzir código boilerplate)
- MapStruct (opcional, para mapeamento de DTOs)
- SpringDoc OpenAPI (para documentação da API)

**Testes:**
- Spring Boot Test
- Testcontainers (opcional, para testes de integração)

### Passo 4: Gerar e Baixar
- Clique em "GENERATE"
- Salve o arquivo `sgl-backend.zip`
- Descompacte na pasta `C:\Users\07548262523\Documents\stock\backend\`

### Passo 5: Estrutura Criada
O Spring Initializr criará:
```
backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/sgl/
│   │   │       └── StockBackendApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── static/
│   │       └── templates/
│   └── test/
│       └── java/
│           └── com/sgl/
│               └── StockBackendApplicationTests.java
├── pom.xml
└── .mvn/
    └── wrapper/
```

### Passo 6: Configuração Inicial
1. **Abrir no IDE** (IntelliJ, Eclipse, VS Code)
2. **Importar projeto Maven**
3. **Executar teste básico** para verificar se compila:
   ```bash
   cd backend
   mvn clean compile
   ```

### Passo 7: Verificar Dependências
Execute no terminal:
```bash
mvn dependency:tree
```
Verifique se todas as dependências foram baixadas corretamente.

### Passo 8: Configurar SecurityConfig (para H2 Console)
Crie o arquivo `backend/sgl-backend/src/main/java/com/sgl/config/SecurityConfig.java`:
```java
package com.sgl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                .referrerPolicy(referrer -> 
                    referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFerrer)
                )
                .frameOptions(frame -> frame.disable())
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()
                .anyRequest().permitAll()
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
```

### Passo 9: Configurar application.properties
```properties
# Servidor
server.port=8080

# Banco de Dados (desenvolvimento com H2)
spring.datasource.url=jdbc:h2:mem:stockdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.h2.console.enabled=true

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# PostgreSQL (produção) - descomente quando necessário
# spring.datasource.url=jdbc:postgresql://localhost:5432/stock
# spring.datasource.driverClassName=org.postgresql.Driver
# spring.datasource.username=postgres
# spring.datasource.password=sua_senha
# spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

**Arquivo:** `backend/sgl-backend/src/main/resources/application.properties`

### Passo 10: Testar H2 Console
1. Reinicie a aplicação: `mvn spring-boot:run`
2. Acesse: http://localhost:8080/h2-console
3. Login:
   - **JDBC URL:** `jdbc:h2:mem:stockdb`
   - **User Name:** `sa`
   - **Password:** (vazio)
4. Clique em "Connect"
5. Deve aparecer a tela do console H2 (vazia, sem tabelas ainda)

### Passo 11: Próximos Passos no Código
1. Criar primeira entidade (Unidade)
2. Criar Repository
3. Criar Service
4. Criar Controller
5. Testar CRUD básico

### Passo 9: Testar a Aplicação
```bash
mvn spring-boot:run
```
Acesse: http://localhost:8080

### Passo 10: Próximos Passos no Código
1. Criar primeira entidade (Unidade)
2. Criar Repository
3. Criar Service
4. Criar Controller
5. Testar CRUD básico

---

## 📋 Próximo Passo: Configurar application.properties

### Ação Necessária
Edite o arquivo `backend/sgl-backend/src/main/resources/application.properties` e adicione as seguintes configurações:

```properties
# Servidor
server.port=8080

# Banco de Dados (desenvolvimento com H2)
spring.datasource.url=jdbc:h2:mem:stockdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.h2.console.enabled=true

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### Por que essas configurações?
- **server.port=8080:** Porta padrão para APIs REST
- **H2 Database:** Banco de dados em memória para desenvolvimento (não precisa instalar nada)
- **spring.h2.console.enabled=true:** Permite acessar o console do banco via navegador
- **spring.jpa.hibernate.ddl-auto=update:** Cria/atualiza tabelas automaticamente
- **spring.jpa.show-sql=true:** Mostra SQL executado no console (útil para debug)

### Verificação
Após salvar, rode novamente:
```bash
cd backend/sgl-backend
mvn spring-boot:run
```
Acesse: http://localhost:8080
Console H2: http://localhost:8080/h2-console

---

**Checklist de Acompanhamento:** `docs/checklist-estrutura-base.md`

---

## 🎓 Guia: Criando a Primeira Entidade - Unidade

### Passo 1: Criar estrutura de pastas
No diretório `backend/sgl-backend/src/main/java/com/sgl/`, crie as seguintes pastas:
```
com/sgl/
├── config/          (já existe - SecurityConfig)
├── model/           (criar)
├── repository/      (criar)
├── service/         (criar)
└── controller/      (criar)
```

### Passo 2: Criar Model - Unidade.java
Crie o arquivo `model/Unidade.java`:

```java
package com.sgl.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "unidades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Unidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String sigla;
}
```

### Passo 3: Criar Repository - UnidadeRepository.java
Crie o arquivo `repository/UnidadeRepository.java`:

```java
package com.sgl.repository;

import com.sgl.model.Unidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnidadeRepository extends JpaRepository<Unidade, Long> {
}
```

### Passo 4: Criar Service - UnidadeService.java
Crie o arquivo `service/UnidadeService.java`:

```java
package com.sgl.service;

import com.sgl.model.Unidade;
import com.sgl.repository.UnidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UnidadeService {

    private final UnidadeRepository unidadeRepository;

    public List<Unidade> listarTodos() {
        return unidadeRepository.findAll();
    }

    public Unidade buscarPorId(Long id) {
        return unidadeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Unidade não encontrada com id: " + id));
    }

    public Unidade salvar(Unidade unidade) {
        return unidadeRepository.save(unidade);
    }

    public Unidade atualizar(Long id, Unidade unidadeAtualizada) {
        Unidade unidade = buscarPorId(id);
        unidade.setNome(unidadeAtualizada.getNome());
        unidade.setSigla(unidadeAtualizada.getSigla());
        return unidadeRepository.save(unidade);
    }

    public void deletar(Long id) {
        unidadeRepository.deleteById(id);
    }
}
```

### Passo 5: Criar Controller - UnidadeController.java
Crie o arquivo `controller/UnidadeController.java`:

```java
package com.sgl.controller;

import com.sgl.model.Unidade;
import com.sgl.service.UnidadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/unidades")
@RequiredArgsConstructor
public class UnidadeController {

    private final UnidadeService unidadeService;

    @GetMapping
    public ResponseEntity<List<Unidade>> listarTodos() {
        return ResponseEntity.ok(unidadeService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Unidade> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(unidadeService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Unidade> salvar(@RequestBody Unidade unidade) {
        Unidade novaUnidade = unidadeService.salvar(unidade);
        return ResponseEntity.status(HttpStatus.CREATED).body(novaUnidade);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Unidade> atualizar(@PathVariable Long id, @RequestBody Unidade unidade) {
        return ResponseEntity.ok(unidadeService.atualizar(id, unidade));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        unidadeService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
```

### Passo 6: Testar CRUD
1. Reinicie a aplicação: `mvn spring-boot:run`
2. Acesse o H2 Console e verifique se a tabela `unidades` foi criada
3. Teste os endpoints:

```bash
# Criar unidade
curl -X POST http://localhost:8080/api/v1/unidades \
  -H "Content-Type: application/json" \
  -d '{"nome": "Instituto de Biologia", "sigla": "IB"}'

# Listar unidades
curl http://localhost:8080/api/v1/unidades

# Buscar por ID
curl http://localhost:8080/api/v1/unidades/1

# Atualizar
curl -X PUT http://localhost:8080/api/v1/unidades/1 \
  -H "Content-Type: application/json" \
  -d '{"nome": "Instituto de Biologia Atualizado", "sigla": "IB"}'

# Deletar
curl -X DELETE http://localhost:8080/api/v1/unidades/1
```

### Estrutura Final
```
backend/sgl-backend/src/main/java/com/sgl/
├── StockBackendApplication.java
├── config/
│   └── SecurityConfig.java
├── model/
│   └── Unidade.java
├── repository/
│   └── UnidadeRepository.java
├── service/
│   └── UnidadeService.java
└── controller/
    └── UnidadeController.java
```

---

**Checklist de Acompanhamento:** `docs/checklist-estrutura-base.md`

---

**IMPORTANTE:** Este guia é para você seguir passo a passo. Eu (Mimo) estou aqui apenas para orientar, documentar e fazer checkups. Você mesmo criará o código! Mimo NÃO implementa código automaticamente - apenas quando solicitado explicitamente.

---

## 🎯 Papéis Definidos

### Você (Desenvolvedor/Estudante)
- **Responsável por:** Criar todo o código, métodos, classes, funções
- **Objetivo:** Treinar e entender o sistema todo
- **Ação:** Seguir os guias e checklists, implementar cada passo

### Mimo (Orientador)
- **Responsável por:** Documentação, orientação, checkups
- **Objetivo:** Guiar, explicar conceitos, verificar progresso
- **Ação:** Fornecer guias, responder dúvidas, validar etapas

### Regra de Ouro
> **Você cria o código. Eu oriento. Juntos garantimos a qualidade.**

### ⚠️ REGRA FUNDAMENTAL - IMPLEMENTAÇÃO
> **Toda e qualquer implementação de código (criar classes, métodos, funções, repositories, services, controllers, DTOs, etc.) DEVE ser feita por VOCÊ, o desenvolvedor.**
>
> **Mimo NÃO implementa código automaticamente.** Mimo apenas:
> - Orienta e explica conceitos
> - Fornece guias e documentação
> - Documenta decisões e progresso
> - Responde dúvidas técnicas
> - Valida etapas concluídas
>
> **Exceção:** Mimo só implementa código quando você solicita **explicitamente** dizendo algo como:
> - "Mimo, implemente isso para mim"
> - "Crie o código disso"
> - "Pode fazer essa implementação?"
>
> **Sem essa solicitação explícita, Mimo apenas orienta.**

### Médio Prazo
5. **Implementar lógica de pedidos** - Fluxo de aprovação
6. **Alertas de estoque baixo** - Notificação automática
7. **Upload de documentos** - Armazenamento de arquivos
8. **Frontend** - Telas principais com Vue.js

### Longo Prazo
9. **Relatórios** - Dashboard e exportações
10. **Notificações** - Email, push
11. **Integrações** - Sistemas externos

---

## 🔗 Recursos e Links

- [ ] Repositório Git: [CRIAR]
- [ ] Protótipo/Figma: [CRIAR]
- [ ] Documentação da API (Swagger): [CRIAR]
- [ ] Banco de dados: [CRIAR SCRIPTS]
- [ ] Diagrama de Classes Atualizado: [CRIAR com EstoqueCentral e EstoqueLaboratorio (apenas conferência)]

---

## 📞 Contatos

| Nome | Papel | Contato |
|------|-------|---------|
| [PREENCHER] | Desenvolvedor | [PREENCHER] |

---

## 📝 Notas e Observações

### Stack Escolhida
- **Backend:** Java Spring Boot (robusto, escalável, ecoistema maduro)
- **Frontend:** Vue.js (moderno, reativo, fácil de aprender)
- **Banco:** PostgreSQL (confiável, open source, suporte a JSON)
- **API:** REST (padrão de mercado, fácil integração)

### Conceitos Importantes
- **Multi-tenant:** Cada Unidade é um tenant separado
- **Estoque central:** Produto é catálogo central, estoque total fica em EstoqueCentral (ÚNICO com entrada/saída)
- **Estoque laboratório:** Apenas conferência/histórico - registrou que o lab recebeu material (sem entrada/saída)
- **Fluxo de pedido:** Pesquisador solicita → Verifica estoque central → Responsável aprova → Baixa no EstoqueCentral → Registro no EstoqueLaboratorio
- **Armazenamento:** Documentos ficam vinculados aos pedidos
- **Risco:** Classificação Nenhum/Baixo/Médio/Alto com tipo específico (radioativo, inflamável, etc)
- **Perecibilidade:** Controle de validade para produtos biológicos, vegetais, animais

### Diferença entre Estoque
- **EstoqueCentral:** Controle real de estoque (entrada/saída/quantidade)
- **EstoqueLaboratorio:** Apenas log/histórico (registrou que o lab recebeu)
- **Consulta:** "quantos álcools foram pro lab1?" → WHERE laboratorio_id = X AND data_recebimento BETWEEN...

### Para Continuar o Projeto
1. Ler este arquivo primeiro
2. Seguir a ordem dos "Próximos Passos"
3. Atualizar este arquivo com decisões e progresso
4. Manter o checklist atualizado
5. Implementar na ordem: Produto → EstoqueCentral → EstoqueLaboratorio → Pedido
6. Lembrar: EstoqueLaboratorio é apenas conferência/histórico (sem entrada/saída)
7. Lembrar: EstoqueCentral é o ÚNICO com controle de estoque (entrada/saída)

---

## 🌐 Estratégia de Ambientes

### Fase 1 (Atual) - Separação via branches Git
- **Branch `main`**: Produção/Release (código estável)
- **Branch `develop` ou `alpha`**: Desenvolvimento/Testes
- **Banco de dados único compartilhado** enquanto o projeto está em construção do core
- Simplicidade inicial, sem custo de infraestrutura adicional

### Fase 2 (Futura) - Bancos separados por ambiente
- Após o projeto base estar consolidado
- **Alpha**: Banco de dados separado para testes e validação
- **Produção**: Banco de dados separado para uso final
- Permite testes mais realistas sem afetar dados de produção

### Fase 3 (Futura, não priorizada) - Nuvem/Infraestrutura
- Avaliar volumes e necessidades de infraestrutura
- Considerar serviços gerenciados (banco, armazenamento, etc.)
- **Decisão adiada** até o projeto base estar funcional
- Prioridade é ter o sistema rodando antes de otimizar infraestrutura

---

**IMPORTANTE:** Este arquivo é o ponto de continuidade do projeto. Qualquer pessoa ou IA que pegar este projeto deve ler este arquivo primeiro para entender o contexto e continuar de onde paramos.
