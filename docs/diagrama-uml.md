# 📊 Diagrama UML - Projeto SGL

## Diagrama de Classes

```mermaid
classDiagram
    direction TB

    class Unidade {
        +Long id
        +String nome
        +String sigla
        +List~Laboratorio~ laboratorios
    }

    class Laboratorio {
        +Long id
        +Unidade unidade
        +String nome
        +String descricao
        +Usuario responsavel
        +Boolean ativo
    }

    class Usuario {
        +Long id
        +String nome
        +String email
        +String senha
        +Perfil perfil
        +Laboratorio laboratorio
        +Boolean ativo
    }

    class Produto {
        +Long id
        +Laboratorio laboratorio
        +String nome
        +String descricao
        +String codigoReferencia
        +Integer quantidadeAtual
        +Integer quantidadeMinima
        +String unidadeMedida
        +String localizacaoFisica
        +Risco risco
        +TipoRisco tipoRisco
        +String descricaoRisco
        +Boolean perecivel
        +Integer diasValidade
        +TipoPerecivel tipoPerecivel
        +String condicoesArmazenamento
        +Boolean ativo
    }

    class Pedido {
        +Long id
        +Usuario usuario
        +Laboratorio laboratorio
        +Projeto projeto
        +LocalDateTime dataSolicitacao
        +StatusPedido status
        +String observacao
        +String arquivoDocumento
    }

    class ItemPedido {
        +Long id
        +Pedido pedido
        +Produto produto
        +Integer quantidadeSolicitada
        +Integer quantidadeAprovada
    }

    class Projeto {
        +Long id
        +Laboratorio laboratorio
        +String nome
        +String descricao
        +LocalDate dataInicio
        +LocalDate dataFim
        +String responsavel
        +Boolean ativo
    }

    class Documento {
        +Long id
        +Pedido pedido
        +String nomeArquivo
        +String caminho
        +String tipo
        +Long tamanho
        +LocalDateTime dataUpload
    }

    class Movimentacao {
        +Long id
        +Produto produto
        +TipoMovimentacao tipo
        +Integer quantidade
        +Pedido pedido
        +Usuario usuario
        +LocalDateTime dataMovimentacao
        +String observacao
    }

    class Lote {
        +Long id
        +Produto produto
        +String numeroLote
        +LocalDate dataFabricacao
        +LocalDate dataValidade
        +Integer quantidade
        +Boolean ativo
    }

    class Perfil {
        <<enumeration>>
        ADMINISTRADOR
        GESTOR
        TECNICO
        PESQUISADOR
        ESTAGIARIO
    }

    class StatusPedido {
        <<enumeration>>
        PENDENTE
        APROVADO
        REJEITADO
        ENTREGUE
        CANCELADO
    }

    class Risco {
        <<enumeration>>
        NENHUM
        BAIXO
        MEDIO
        ALTO
    }

    class TipoRisco {
        <<enumeration>>
        NENHUM
        INFLAMAVEL
        RADIOATIVO
        TOXICO
        CORROSIVO
        BIOLOGICO
    }

    class TipoPerecivel {
        <<enumeration>>
        NENHUM
        VEGETAL
        ANIMAL
        MICROBIANO
        QUIMICO
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

    Laboratorio "1" --> "*" Usuario : abriga
    Laboratorio "1" --> "0..1" Usuario : responsavel
    Laboratorio "1" --> "*" Pedido : recebe
    Laboratorio "1" --> "*" Produto : contem
    Laboratorio "1" --> "*" Projeto : contem

    Usuario "1" --> "0..1" Laboratorio : vinculado a
    Usuario "1" --> "*" Pedido : realiza
    Usuario "1" --> "*" Movimentacao : registra

    Produto "1" --> "*" Lote : possui
    Produto "1" --> "*" ItemPedido : incluido em
    Produto "1" --> "*" Movimentacao : movimentado

    Lote "1" --> "*" ItemPedido : pode ser
    Lote "1" --> "*" Movimentacao : pode ser

    Projeto "1" --> "*" Pedido : vinculado a

    Pedido "1" --> "*" ItemPedido : contem
    Pedido "1" --> "*" Documento : possui
    Pedido "1" --> "*" Movimentacao : gera
```

---

## Descrição dos Relacionamentos

| Relacionamento | Cardinalidade | Descrição |
|----------------|---------------|-----------|
| Unidade → Laboratório | 1:N | Uma unidade possui vários laboratórios |
| Unidade → Usuário | 1:N | Uma unidade possui vários usuários |
| Laboratório → Usuário | 1:N | Um laboratório abriga vários usuários |
| Laboratório → Usuário (responsável) | 1:0..1 | Um laboratório tem um responsável (Usuario) |
| Laboratório → Pedido | 1:N | Um laboratório recebe vários pedidos |
| Laboratório → Produto | 1:N | Um laboratório contém vários produtos |
| Laboratório → Projeto | 1:N | Um laboratório contém vários projetos |
| Usuário → Laboratório | N:1 | Um usuário está vinculado a um laboratório |
| Usuário → Pedido | 1:N | Um usuário realiza vários pedidos |
| Usuário → Movimentação | 1:N | Um usuário registra várias movimentações |
| Produto → Lote | 1:N | Um produto pode ter vários lotes |
| Produto → ItemPedido | 1:N | Um produto pode estar em vários pedidos |
| Produto → Movimentação | 1:N | Um produto pode ser movimentado várias vezes |
| Lote → ItemPedido | 1:N | Um lote pode estar em vários pedidos |
| Lote → Movimentação | 1:N | Um lote pode ser movimentado várias vezes |
| Projeto → Pedido | 1:N | Um projeto pode ter vários pedidos |
| Pedido → ItemPedido | 1:N | Um pedido contém vários itens |
| Pedido → Documento | 1:N | Um pedido pode ter vários documentos |
| Pedido → Movimentação | 1:N | Um pedido gera várias movimentações |

---

## Enumerações

### Perfil
| Valor | Descrição |
|-------|-----------|
| ADMINISTRADOR | Administrador do sistema |
| GESTOR | Gerente de laboratório |
| TECNICO | Técnico de laboratório |
| PESQUISADOR | Pesquisador do laboratório |
| ESTAGIARIO | Estagiário do laboratório |

### StatusPedido
| Valor | Descrição |
|-------|-----------|
| PENDENTE | Aguardando aprovação |
| APROVADO | Aprovado pelo gestor |
| REJEITADO | Rejeitado pelo gestor |
| ENTREGUE | Material entregue |
| CANCELADO | Pedido cancelado |

### Risco
| Valor | Descrição |
|-------|-----------|
| NENHUM | Sem risco |
| BAIXO | Risco mínimo |
| MEDIO | Risco médio |
| ALTO | Risco alto |

### TipoRisco
| Valor | Descrição |
|-------|-----------|
| NENHUM | Sem tipo de risco |
| INFLAMAVEL | Material inflamável |
| RADIOATIVO | Material radioativo |
| TOXICO | Material tóxico |
| CORROSIVO | Material corrosivo |
| BIOLOGICO | Agente biológico |

### TipoPerecivel
| Valor | Descrição |
|-------|-----------|
| NENHUM | Não é perecível |
| VEGETAL | Produto vegetal |
| ANIMAL | Produto animal |
| MICROBIANO | Microrganismo |
| QUIMICO | Produto químico |

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
- `laboratorio.responsavel_id` → `usuario.id` (nullable)
- `usuario.laboratorio_id` → `laboratorio.id` (nullable)
- `produto.laboratorio_id` → `laboratorio.id`
- `lote.produto_id` → `produto.id`
- `projeto.laboratorio_id` → `laboratorio.id`
- `pedido.usuario_id` → `usuario.id`
- `pedido.laboratorio_id` → `laboratorio.id`
- `pedido.projeto_id` → `projeto.id` (nullable)
- `item_pedido.pedido_id` → `pedido.id`
- `item_pedido.produto_id` → `produto.id`
- `documento.pedido_id` → `pedido.id`
- `movimentacao.produto_id` → `produto.id`
- `movimentacao.pedido_id` → `pedido.id` (nullable)
- `movimentacao.usuario_id` → `usuario.id`

### Índices Recomendados
- `unidade.sigla` (único)
- `laboratorio.unidade_id`
- `laboratorio.responsavel_id`
- `usuario.email` (único)
- `usuario.laboratorio_id`
- `produto.laboratorio_id`
- `lote.produto_id`
- `lote.data_validade`
- `pedido.usuario_id`
- `pedido.laboratorio_id`
- `pedido.status`
- `pedido.data_solicitacao`
- `movimentacao.produto_id`
- `movimentacao.usuario_id`
- `movimentacao.data_movimentacao`
