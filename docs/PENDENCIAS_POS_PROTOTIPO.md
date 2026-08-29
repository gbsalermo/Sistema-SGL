# Pendências pós-protótipo — SGL

**Registrado em:** 29/08/2026  
**Escopo:** `gbsalermo/Sistema-SGL` + `gbsalermo/SGL-FRONTEND`

Este documento registra refactors e melhorias que devem ser executados **depois do fechamento e validação do protótipo**, sem alterar o roadmap funcional atual.

---

# 1. Padronização do código para inglês

## Objetivo

Padronizar o código-fonte do SGL para inglês, substituindo nomes em português usados em:

- classes;
- interfaces;
- enums, quando fizer sentido;
- DTOs;
- services;
- repositories;
- controllers;
- métodos;
- parâmetros e variáveis internas;
- nomes técnicos equivalentes no frontend (types, interfaces, services, composables, stores e funções).

A interface exibida ao usuário **continua em português**.

## Exemplos atuais

O backend ainda possui nomes como:

```text
Pedido
ItemPedido
Produto
Laboratorio
Projeto
Usuario
EstoqueCentral
MovimentacaoEstoque
HistoricoLaboratorio
PedidoService
MovimentacaoEstoqueService
RelatorioFiscalizacaoService
```

E métodos como:

```text
criar
listarTodos
buscarPorId
listarPorUsuario
listarPorStatus
listarPorUrgencia
listarPorProjetoEPeriodo
aprovar
rejeitar
entregar
validarConsistenciaPedido
validarPeriodo
registrarSaida
```

A conversão deve resultar em nomenclatura técnica consistente em inglês, por exemplo:

```text
Pedido                  → Order
ItemPedido              → OrderItem
Produto                 → Product
Laboratorio             → Laboratory
Projeto                  → Project
Usuario                  → User
EstoqueCentral          → CentralInventory / InventoryBalance (definir padrão antes do refactor)
MovimentacaoEstoque     → InventoryMovement
HistoricoLaboratorio    → LaboratoryHistory

criar                    → create
listarTodos              → findAll / listAll
buscarPorId              → findById
listarPorUsuario         → findByUser
listarPorStatus          → findByStatus
aprovar                  → approve
rejeitar                 → reject
entregar                 → deliver
registrarSaida           → registerOutboundMovement
```

Os nomes definitivos devem ser definidos em um mapa de nomenclatura antes da alteração em massa.

---

# 2. Regra de segurança do refactor

Esta mudança é estrutural e **não deve alterar regra de negócio**.

Executar somente em branch própria após o protótipo estar estável.

Fluxo obrigatório:

```text
1. congelar contratos funcionais do protótipo
2. criar branch específica de refactor
3. criar mapa Português → Inglês
4. renomear domínio e DTOs
5. atualizar imports e referências
6. renomear services/repositories/controllers e seus métodos
7. atualizar testes
8. atualizar frontend quando os tipos/contratos exigirem
9. executar build + testes backend
10. executar build + testes/lint frontend
11. validar Swagger
12. validar manualmente fluxos críticos
13. somente então realizar merge
```

---

# 3. Compatibilidade que deve ser preservada

Para evitar quebra desnecessária, a primeira etapa do refactor deve **preservar os contratos externos** sempre que possível.

Não renomear automaticamente junto com as classes Java:

```text
nomes de tabelas PostgreSQL
nomes de colunas já persistidas
migrations Flyway antigas
paths públicos da API
campos JSON consumidos pelo frontend
valores persistidos de enums
```

Quando for desejável traduzir também um contrato externo, isso deve ser tratado como uma migração separada e coordenada entre backend e frontend.

Em entidades JPA, nomes de classes podem ser traduzidos mantendo `@Table` e `@Column` apontando para a estrutura existente do banco.

---

# 4. Áreas críticas para regressão

Depois do refactor, validar obrigatoriamente:

```text
Pedidos: criação, aprovação, rejeição, entrega e cancelamento
Urgência
FIFO / FEFO
Entrada e saída por lote
Fracionamento e embalagem
Concorrência de aprovação
Movimentações
Histórico e rastreabilidade
Fiscalização
Relatórios
Exportação PDF/XLSX
Resíduos, se já estiver integrado
Autenticação/autorização, se já estiver concluída
```

---

# 5. Critério de conclusão

A pendência só é considerada concluída quando:

```text
[ ] classes técnicas padronizadas em inglês
[ ] métodos técnicos padronizados em inglês
[ ] DTOs/repositories/services/controllers consistentes
[ ] frontend técnico padronizado onde aplicável
[ ] banco existente abre sem migration destrutiva
[ ] Swagger continua funcional
[ ] backend compila e testes passam
[ ] frontend compila
[ ] fluxos críticos continuam funcionando
[ ] documentação técnica é atualizada
```

---

# Decisão

**Status:** ⏳ Pós-protótipo.

Não executar durante o fechamento funcional atual. A tradução do código deve ser tratada como um refactor controlado, sem alteração simultânea de comportamento, banco e contratos HTTP.
