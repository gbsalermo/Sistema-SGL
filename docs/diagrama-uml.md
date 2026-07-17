# 📊 Diagrama UML - Projeto STOCK

## Diagrama de Classes

```mermaid
classDiagram
    direction TB

    class Unidade {
        +Long id
        +String nome
        +String codigo
        +String endereco
        +String telefone
        +String email
        +Boolean ativo
        +LocalDateTime dataCriacao
    }

    class Laboratorio {
        +Long id
        +Long unidade_id
        +String nome
        +String descricao
        +String responsavelNome
        +String responsavelEmail
        +Boolean ativo
        +LocalDateTime dataCriacao
    }

    class Usuario {
        +Long id
        +Long unidade_id
        +Long laboratorio_id
        +String nome
        +String email
        +String senhaHash
        +TipoUsuario tipo
        +Boolean ativo
        +LocalDateTime dataCriacao
    }

    class Produto {
        +Long id
        +Long unidade_id
        +String nome
        +String descricao
        +String codigo
        +CategoriaProduto categoria
        +Integer quantidadeAtual
        +Integer quantidadeMinima
        +String unidadeMedida
        +String localizacao
        +Boolean ativo
        +LocalDateTime dataCriacao
    }

    class Lote {
        +Long id
        +Long produto_id
        +String numeroLote
        +LocalDate dataFabricacao
        +LocalDate dataValidade
        +Integer quantidade
        +Boolean ativo
        +LocalDateTime dataCriacao
    }

    class Projeto {
        +Long id
        +Long unidade_id
        +String nome
        +String descricao
        +String responsavel
        +LocalDate dataInicio
        +LocalDate dataFim
        +Boolean ativo
        +LocalDateTime dataCriacao
    }

    class Pedido {
        +Long id
        +Long unidade_id
        +Long laboratorio_id
        +Long usuario_id
        +Long projeto_id
        +LocalDateTime dataSolicitacao
        +LocalDateTime dataAprovacao
        +StatusPedido status
        +String observacao
        +String justificativa
        +String arquivoDocumento
        +LocalDateTime dataCriacao
    }

    class ItemPedido {
        +Long id
        +Long pedido_id
        +Long produto_id
        +Long lote_id
        +Integer quantidadeSolicitada
        +Integer quantidadeAprovada
        +String observacao
    }

    class Movimentacao {
        +Long id
        +Long unidade_id
        +Long produto_id
        +Long lote_id
        +TipoMovimentacao tipo
        +Integer quantidade
        +Long pedido_id
        +Long usuario_id
        +LocalDateTime dataMovimentacao
        +String observacao
    }

    class TipoUsuario {
        <<enumeration>>
        PESQUISADOR
        ESTUDANTE
        ESTAGIARIO
        GESTOR
        ADMIN
    }

    class CategoriaProduto {
        <<enumeration>>
        PERECIVEL
        PERMANENTE
        CONSUMIVEL
    }

    class StatusPedido {
        <<enumeration>>
        PENDENTE
        APROVADO
        REJEITADO
        ENTREGUE
        CANCELADO
    }

    class TipoMovimentacao {
        <<enumeration>>
        ENTRADA
        SAIDA
        TRANSFERENCIA
        AJUSTE
    }

    %% Relacionamentos
    Unidade "1" --> "*" Laboratorio : possui
    Unidade "1" --> "*" Usuario : possui
    Unidade "1" --> "*" Produto : possui
    Unidade "1" --> "*" Projeto : possui

    Laboratorio "1" --> "*" Usuario : abriga
    Laboratorio "1" --> "*" Pedido : recebe

    Usuario "1" --> "*" Pedido : realiza
    Usuario "1" --> "*" Movimentacao : registra

    Produto "1" --> "*" Lote : possui
    Produto "1" --> "*" ItemPedido : incluido em
    Produto "1" --> "*" Movimentacao : movimentado

    Lote "1" --> "*" ItemPedido : pode ser
    Lote "1" --> "*" Movimentacao : pode ser

    Projeto "1" --> "*" Pedido : vinculado a

    Pedido "1" --> "*" ItemPedido : contem
    Pedido "1" --> "*" Movimentacao : gera
```

---

## Descrição dos Relacionamentos

| Relacionamento | Cardinalidade | Descrição |
|----------------|---------------|-----------|
| Unidade → Laboratório | 1:N | Uma unidade possui vários laboratórios |
| Unidade → Usuário | 1:N | Uma unidade possui vários usuários |
| Unidade → Produto | 1:N | Uma unidade possui vários produtos |
| Unidade → Projeto | 1:N | Uma unidade possui vários projetos |
| Laboratório → Usuário | 1:N | Um laboratório abriga vários usuários |
| Laboratório → Pedido | 1:N | Um laboratório recebe vários pedidos |
| Usuário → Pedido | 1:N | Um usuário realiza vários pedidos |
| Usuário → Movimentação | 1:N | Um usuário registra várias movimentações |
| Produto → Lote | 1:N | Um produto pode ter vários lotes |
| Produto → ItemPedido | 1:N | Um produto pode estar em vários pedidos |
| Produto → Movimentação | 1:N | Um produto pode ser movimentado várias vezes |
| Lote → ItemPedido | 1:N | Um lote pode estar em vários pedidos |
| Lote → Movimentação | 1:N | Um lote pode ser movimentado várias vezes |
| Projeto → Pedido | 1:N | Um projeto pode ter vários pedidos |
| Pedido → ItemPedido | 1:N | Um pedido contém vários itens |
| Pedido → Movimentação | 1:N | Um pedido gera várias movimentações |

---

## Enumerações

### TipoUsuario
| Valor | Descrição |
|-------|-----------|
| PESQUISADOR | Pesquisador do laboratório |
| ESTUDANTE | Estudante vinculado ao laboratório |
| ESTAGIARIO | Estagiário do laboratório |
| GESTOR | Departamento de Gestão |
| ADMIN | Administrador da Unidade |

### CategoriaProduto
| Valor | Descrição |
|-------|-----------|
| PERECIVEL | Produto com validade (usa lotes) |
| PERMANENTE | Produto de uso contínuo |
| CONSUMIVEL | Produto que acaba com o uso |

### StatusPedido
| Valor | Descrição |
|-------|-----------|
| PENDENTE | Aguardando aprovação |
| APROVADO | Aprovado pelo gestor |
| REJEITADO | Rejeitado pelo gestor |
| ENTREGUE | Material entregue |
| CANCELADO | Pedido cancelado |

### TipoMovimentacao
| Valor | Descrição |
|-------|-----------|
| ENTRADA | Material entra no estoque |
| SAIDA | Material sai do estoque |
| TRANSFERENCIA | Move entre locais |
| AJUSTE | Correção de quantidade |

---

## Diagrama de Fluxo - Pedido

```mermaid
flowchart TD
    A[Pesquisador inicia pedido] --> B[Seleciona Laboratório]
    B --> C{Deseja vincular projeto?}
    C -->|Sim| D[Seleciona Projeto]
    C -->|Não| E[Seleciona Itens]
    D --> E
    E --> F[Informa Quantidades]
    F --> G{Anexa documento?}
    G -->|Sim| H[Upload Documento]
    G -->|Não| I[Envia Pedido]
    H --> I
    I --> J[Status: PENDENTE]
    J --> K[Gestor visualiza pedido]
    K --> L{Estoque disponível?}
    L -->|Sim| M{Aprovar?}
    L -->|Não| N[Alerta: Estoque insuficiente]
    N --> O[Gestor decide: Aguardar/Rejeitar]
    M -->|Sim| P[Aprova Pedido]
    M -->|Não| Q[Rejeita Pedido]
    P --> R[Baixa no Estoque]
    R --> S[Status: ENTREGUE]
    Q --> T[Status: REJEITADO]
    O -->|Rejeitar| Q
    O -->|Aguardar| U[Status: PENDENTE]
```

---

## Diagrama de Fluxo - Estoque

```mermaid
flowchart TD
    A[Produto Cadastrado] --> B[Estoque Inicial]
    B --> C{Tipo de Produto}
    
    C -->|Perecível| D[Cadastra Lote]
    D --> E[Atualiza Quantidade]
    
    C -->|Permanente/Consumível| E
    
    E --> F[Monitora Estoque]
    F --> G{Quantidade ≤ Mínima?}
    G -->|Sim| H[Alerta: Estoque Baixo]
    G -->|Não| I[Estoque OK]
    
    F --> J{Lote Vencendo?}
    J -->|Sim| K[Alerta: Validade Próxima]
    J -->|Não| L[Sem Alerta]
    
    H --> M[Gestor toma decisão]
    K --> M
    M --> N[Compra/Transferência]
    N --> O[Entrada no Estoque]
    O --> E
```

---

## Observações Técnicas

### Chaves Estrangeiras
- `laboratorio.unidade_id` → `unidade.id`
- `usuario.unidade_id` → `unidade.id`
- `usuario.laboratorio_id` → `laboratorio.id`
- `produto.unidade_id` → `unidade.id`
- `lote.produto_id` → `produto.id`
- `projeto.unidade_id` → `unidade.id`
- `pedido.unidade_id` → `unidade.id`
- `pedido.laboratorio_id` → `laboratorio.id`
- `pedido.usuario_id` → `usuario.id`
- `pedido.projeto_id` → `projeto.id` (nullable)
- `item_pedido.pedido_id` → `pedido.id`
- `item_pedido.produto_id` → `produto.id`
- `item_pedido.lote_id` → `lote.id` (nullable)
- `movimentacao.unidade_id` → `unidade.id`
- `movimentacao.produto_id` → `produto.id`
- `movimentacao.lote_id` → `lote.id` (nullable)
- `movimentacao.pedido_id` → `pedido.id` (nullable)
- `movimentacao.usuario_id` → `usuario.id`

### Índices Recomendados
- `unidade.codigo` (único)
- `laboratorio.unidade_id`
- `usuario.unidade_id`
- `usuario.email` (único por unidade)
- `produto.unidade_id`
- `produto.categoria`
- `lote.produto_id`
- `lote.data_validade`
- `pedido.unidade_id`
- `pedido.laboratorio_id`
- `pedido.usuario_id`
- `pedido.status`
- `pedido.data_solicitacao`
- `movimentacao.unidade_id`
- `movimentacao.produto_id`
- `movimentacao.data_movimentacao`
