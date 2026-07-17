# 📦 Projeto SGL - Sistema de Gestão de Laboratórios

## 📋 Status do Projeto
**Fase:** Planejamento / Amadurecimento de Ideias  
**Data de início:** 13/07/2026  
**Última atualização:** 13/07/2026  

---

## 🎯 Visão Geral

**Nome do projeto:** SGL  
**Tipo:** Sistema de Gestão de Estoque para Laboratórios de Pesquisa  
**Objetivo:** Automatizar e centralizar o controle de materiais entre unidade, laboratórios e pesquisadores

---

## 💡 Conceito Central

### O que é o sistema?
Sistema web para gerenciar o estoque de materiais de uma **unidade** (instituição), onde:
- O **estoque pertence à unidade**
- Os **laboratórios** fazem pedidos ao departamento de gestão
- Os **pesquisadores** solicitam materiais para seus projetos

### Fluxo Principal
```
┌─────────────────────────────────────────────────────────────────┐
│                        UNIDADE                                  │
│              (Ex: Campus, Instituição, Empresa)                 │
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │              DEPARTAMENTO DE GESTÃO                     │   │
│   │                                                         │   │
│   │  • Recebe pedidos dos laboratórios                      │   │
│   │  • Aprova/rejeita pedidos                               │   │
│   │  • Entrega materiais                                    │   │
│   │  • Faz pedidos externos (compras)                       │   │
│   │  • Alimenta o estoque                                   │   │
│   │  • Recebe relatórios e alertas                          │   │
│   └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                    ESTOQUE                              │   │
│   │                                                         │   │
│   │  • Produtos com quantidades                             │   │
│   │  • Quantidade mínima para alerta                        │   │
│   │  • Lotes com validade (opcional)                        │   │
│   │  • Histórico de movimentações                           │   │
│   └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│         ┌────────────────────┼────────────────────┐             │
│         ▼                    ▼                    ▼             │
│   ┌──────────┐         ┌──────────┐         ┌──────────┐       │
│   │  LAB 1   │         │  LAB 2   │         │  LAB 3   │       │
│   └──────────┘         └──────────┘         └──────────┘       │
│         │                    │                    │             │
│         ▼                    ▼                    ▼             │
│   ┌──────────┐         ┌──────────┐         ┌──────────┐       │
│   │Pesquisad.│         │Pesquisad.│         │Pesquisad.│       │
│   └──────────┘         └──────────┘         └──────────┘       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 👥 Perfis de Usuários

### 1. Pesquisador / Estudante / Estagiário
- **Acesso:** Apenas tela de pedidos
- **O que faz:** Solicita material para seu laboratório vinculado a um projeto
- **Pode ver:** Apenas seus próprios pedidos
- **Não pode:** Ver estoque completo, relatórios, outros pedidos

### 2. Departamento de Gestão (Operador)
- **Acesso:** Estoque + Pedidos + Relatórios
- **O que faz:** Gerencia todo o fluxo de materiais da unidade
- **Pode ver:** Todos os pedidos, estoque completo, relatórios
- **Pode fazer:**
  - Aprovar/rejeitar pedidos
  - Dar baixa no estoque (entrega)
  - Cadastrar produtos e lotes
  - Fazer pedidos externos (compras)
  - Alimentar estoque (entradas)
  - Gerar relatórios

### 3. Administrador da Unidade
- **Acesso:** Tudo
- **O que faz:** Configura o sistema e gerencia usuários
- **Pode fazer:** Tudo + gerenciar laboratórios e usuários

---

## 📦 Estoque - Conceitos Importantes

### Valores
- **Sem valor financeiro** - O sistema controla apenas quantidades, não preços

### Categorias de Produtos

| Categoria | Descrição | Controle Especial |
|-----------|-----------|-------------------|
| **Perecível** | Itens com validade | Lote com data de validade + alerta |
| **Permanente** | Itens de uso contínuo | Sem validade |
| **Consumível** | Itens que acabam | Controle por quantidade |

### Lotes (para perecíveis)
```
Cada entrada de produto perecível pode ser um LOTE:
- Número do lote
- Data de fabricação
- Data de validade
- Quantidade do lote

Quando estoque ≤ mínimo OU validade próxima → ALERTA
```

### Alertas
1. **Estoque baixo:** Quantidade ≤ quantidade mínima
2. **Validade próxima:** Lote vencendo (configurar dias de antecedência)
3. **Ambos:** Dashboard + aviso ao fazer pedido

---

## 📋 Pedidos - Regras

### Obrigatório
- Todo pedido **DEVE** estar vinculado a:
  - **Laboratório** (de qual laboratório é o pedido)

### Opcional
- **Projeto** (se o material é para um projeto específico)
- Documento/justificativa (não obrigatório)

### Fluxo Detalhado
```
1. Pesquisador acessa "Fazer Pedido"
   │
   ├─ Seleciona LABORATÓRIO (obrigatório)
   ├─ Seleciona PROJETO (opcional)
   ├─ Seleciona ITENS desejados
   ├─ Informa QUANTIDADE
   └─ Anexa DOCUMENTO (opcional)
   │
   ▼
2. Pedido criado → Status: PENDENTE
   │
   ▼
3. Departamento de Gestão vê na fila
   │
   ├─ Verifica estoque disponível
   │   └─ Se insuficiente → Alerta "só restam X unidades"
   │
   ├─ APROVA → Baixa no estoque → Status: ENTREGUE
   └─ REJEITA → Justificativa → Status: REJEITADO
```

---

## 🏗️ Arquitetura Técnica

### Stack Tecnológica

| Camada | Tecnologia | Justificativa |
|--------|------------|---------------|
| **Frontend** | Vue.js 3 | Moderno, reativo, fácil integração |
| **Backend** | Java 17 + Spring Boot 3 | Robusto, escalável, ecoistema maduro |
| **Banco de Dados** | PostgreSQL 14+ | Confiável, open source, JSON support |
| **API** | REST | Padrão de mercado |
| **Auth** | JWT + Login local | Simples, seguro |

### Modelo de Dados (Entidades)

```sql
-- UNIDADE (Tenant)
unidade (
  id, nome, codigo, endereco, telefone, email, ativo
)

-- LABORATÓRIO
laboratorio (
  id, unidade_id, nome, descricao, 
  responsavel_nome, responsavel_email, ativo
)

-- USUÁRIO
usuario (
  id, unidade_id, nome, email, senha_hash, 
  tipo, laboratorio_id, ativo
)

-- TIPOS DE USUÁRIO
-- PESQUISADOR, ESTUDANTE, ESTAGIARIO (só faz pedidos)
-- GESTOR (Departamento de Gestão)
-- ADMIN (Administrador da Unidade)

-- PRODUTO
produto (
  id, unidade_id, nome, descricao, codigo, 
  categoria, -- PERECIVEL, PERMANENTE, CONSUMIVEL
  quantidade_atual, quantidade_minima, 
  unidade_medida, localizacao, ativo
)

-- LOTE (para perecíveis - opcional)
lote (
  id, produto_id, numero_lote, 
  data_fabricacao, data_validade, 
  quantidade, ativo
)

-- PROJETO
projeto (
  id, unidade_id, nome, descricao, responsavel,
  data_inicio, data_fim, ativo
)

-- PEDIDO
pedido (
  id, unidade_id, laboratorio_id, usuario_id, 
  projeto_id, -- OPCIONAL (pode ser null)
  data_solicitacao, data_aprovacao, 
  status, -- PENDENTE, APROVADO, REJEITADO, ENTREGUE, CANCELADO
  observacao, justificativa, arquivo_documento
)

-- ITEM DO PEDIDO
item_pedido (
  id, pedido_id, produto_id, 
  quantidade_solicitada, quantidade_aprovada,
  lote_id, -- Opcional: para perecíveis, qual lote foi entregue
  observacao
)

-- HISTÓRICO DE MOVIMENTAÇÃO
movimentacao (
  id, unidade_id, produto_id, lote_id,
  tipo, -- ENTRADA, SAIDA, TRANSFERENCIA, AJUSTE
  quantidade, pedido_id, usuario_id, 
  data_movimentacao, observacao
)
```

---

## 🔌 Endpoints da API

### Autenticação
```
POST   /api/v1/auth/login            - Login
POST   /api/v1/auth/logout           - Logout
GET    /api/v1/auth/me               - Dados do usuário logado
```

### Unidades
```
GET    /api/v1/unidades              - Listar (admin geral)
POST   /api/v1/unidades              - Criar unidade
GET    /api/v1/unidades/{id}         - Buscar unidade
PUT    /api/v1/unidades/{id}         - Atualizar unidade
```

### Laboratórios
```
GET    /api/v1/laboratorios              - Listar labs da unidade
POST   /api/v1/laboratorios              - Criar laboratório
GET    /api/v1/laboratorios/{id}         - Buscar laboratório
PUT    /api/v1/laboratorios/{id}         - Atualizar laboratório
GET    /api/v1/laboratorios/{id}/pedidos - Pedidos do laboratório
```

### Usuários
```
GET    /api/v1/usuarios                  - Listar usuários da unidade
POST   /api/v1/usuarios                  - Criar usuário
PUT    /api/v1/usuarios/{id}             - Atualizar usuário
```

### Produtos (Estoque)
```
GET    /api/v1/produtos                  - Listar produtos da unidade
POST   /api/v1/produtos                  - Cadastrar produto
PUT    /api/v1/produtos/{id}             - Atualizar produto
GET    /api/v1/produtos/{id}/historico   - Histórico de movimentações
GET    /api/v1/produtos/estoque-baixo    - Listar com estoque ≤ mínimo
GET    /api/v1/produtos/validade-proxima - Listar perecíveis vencendo
```

### Lotes
```
GET    /api/v1/produtos/{id}/lotes       - Listar lotes do produto
POST   /api/v1/produtos/{id}/lotes       - Criar lote (entrada)
PUT    /api/v1/lotes/{id}                - Atualizar lote
```

### Pedidos
```
GET    /api/v1/pedidos                   - Listar pedidos (filtrar por status)
POST   /api/v1/pedidos                   - Criar pedido (pesquisador)
GET    /api/v1/pedidos/{id}              - Buscar pedido
PUT    /api/v1/pedidos/{id}/aprovar      - Aprovar pedido (gestão)
PUT    /api/v1/pedidos/{id}/rejeitar     - Rejeitar pedido (gestão)
PUT    /api/v1/pedidos/{id}/entregar     - Marcar como entregue (gestão)
POST   /api/v1/pedidos/{id}/documento    - Upload documento
GET    /api/v1/pedidos/{id}/documento    - Download documento
```

### Projetos
```
GET    /api/v1/projetos                  - Listar projetos da unidade
POST   /api/v1/projetos                  - Criar projeto
GET    /api/v1/projetos/{id}             - Buscar projeto
PUT    /api/v1/projetos/{id}             - Atualizar projeto
GET    /api/v1/projetos/{id}/pedidos     - Pedidos do projeto
```

### Relatórios (apenas Gestor/Admin)
```
GET    /api/v1/relatorios/estoque        - Relatório de estoque atual
GET    /api/v1/relatorios/pedidos        - Relatório de pedidos
GET    /api/v1/relatorios/laboratorio/{id} - Relatório do laboratório
GET    /api/v1/relatorios/movimentacoes  - Relatório de movimentações
GET    /api/v1/relatorios/validades      - Relatório de validades próximas
```

---

## 📝 Decisões Tomadas

| Data | Decisão | Motivo |
|------|---------|--------|
| 13/07/2026 | Criar projeto | Ideia inicial do app de estoque |
| 13/07/2026 | Java Spring Boot | Base sólida, escalável |
| 13/07/2026 | Vue.js no frontend | Moderno, reativo |
| 13/07/2026 | PostgreSQL | Confiável, open source |
| 13/07/2026 | Estoque é da UNIDADE | Não do laboratório |
| 13/07/2026 | 3 perfis iniciais | Pesquisador, Gestor, Admin |
| 13/07/2026 | Documento opcional | Não obrigatório |
| 13/07/2026 | Alerta no dashboard + pedido | Não automático por email |
| 13/07/2026 | Login e senha local | Simples, sem integração |
| 13/07/2026 | Sem valor financeiro | Só controle de quantidade |
| 13/07/2026 | Categorias: perecível/permanente | Perecível com lote e validade |
| 13/07/2026 | Pedido vincula lab obrigatório, projeto opcional | Flexibilidade para pedidos gerais |
| 13/07/2026 | Pesquisador só vê pedidos | Acesso limitado ao necessário |

---

## 🚧 Em Andamento

- [x] Definição da ideia principal (13/07/2026)
- [x] Definição dos 3 pilares (13/07/2026)
- [x] Escolha das tecnologias (13/07/2026)
- [x] Definição dos perfis de usuário (13/07/2026)
- [x] Definição do fluxo de pedidos (13/07/2026)
- [x] Definição do modelo de dados (13/07/2026)
- [x] Definição de categorias e lotes (13/07/2026)
- [ ] Prototipação das telas
- [ ] Criação do banco de dados
- [ ] Desenvolvimento da API

---

## ✅ Concluído

- [x] Criação da pasta do projeto (13/07/2026)
- [x] Criação do arquivo de continuidade (13/07/2026)
- [x] Definição da arquitetura (13/07/2026)
- [x] Definição do modelo de dados (13/07/2026)
- [x] Definição das regras de negócio (13/07/2026)
- [x] Definição de categorias e lotes (13/07/2026)

---

## ❌ Bloqueios e Pendências

| Item | Descrição | Responsável | Status |
|------|-----------|-------------|--------|
| - | Nenhum no momento | - | - |

---

## 📌 Próximos Passos

### Curto Prazo
1. **Prototipar telas principais** - Wireframe do fluxo de pedido
2. **Validar modelo de dados** - Conferir se cobre todos os casos
3. **Criar repositório Git** - Versionar o código

### Médio Prazo
4. **Configurar Spring Boot** - Projeto base com dependências
5. **Criar banco de dados** - Scripts SQL das tabelas
6. **Desenvolver CRUDs básicos** - Unidades, Labs, Produtos, Usuários

### Longo Prazo
7. **Implementar fluxo de pedidos** - Solicitação → Aprovação → Entrega
8. **Alertas de estoque baixo + validade** - Dashboard + aviso no pedido
9. **Upload de documentos** - Armazenamento de arquivos
10. **Frontend** - Telas principais com Vue.js
11. **Relatórios** - Dashboard e exportações

---

## 🔗 Recursos e Links

- [ ] Repositório Git: [CRIAR]
- [ ] Protótipo/Figma: [CRIAR]
- [ ] Documentação da API (Swagger): [CRIAR]
- [ ] Banco de dados: [CRIAR SCRIPTS]

---

## 📞 Contatos

| Nome | Papel | Contato |
|------|-------|---------|
| [PREENCHER] | Desenvolvedor | [PREENCHER] |

---

## 📝 Notas e Observações

### Conceitos Importantes (Resumo)

1. **Estoque é da Unidade** - Não do laboratório
2. **Laboratórios pedem** - Ao departamento de gestão
3. **Pesquisadores solicitam** - Para seu lab (obrigatório) E projeto (opcional)
4. **Gestão aprova e entrega** - É o centro de controle
5. **Sem valor financeiro** - Só controle de quantidade
6. **Categorias:** Perecível (com lote/validade), Permanente, Consumível
7. **Alertas:** Estoque baixo + Validade próxima
8. **Pesquisador:** Só acessa para fazer pedidos (acesso limitado)

### Fluxo Resumido
```
Pesquisador → Faz Pedido (com projeto) → Gestão Avalia → Aprova/Rejeita → Entrega → Baixa no Estoque
```

### Para Continuar o Projeto
1. Ler este arquivo primeiro
2. Seguir a ordem dos "Próximos Passos"
3. Atualizar este arquivo com decisões e progresso
4. Manter o checklist atualizado

---

**IMPORTANTE:** Este arquivo é o ponto de continuidade do projeto. Qualquer pessoa ou IA que pegar este projeto deve ler este arquivo primeiro para entender o contexto e continuar de onde paramos.
