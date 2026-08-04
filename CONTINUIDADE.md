# 📦 Continuidade do Projeto SGL

## 📋 Estado do projeto

**Projeto:** SGL — Sistema de Gestão de Laboratórios  
**Início:** 13/07/2026  
**Última atualização:** 04/08/2026  
**Fase atual:** consolidação das regras de negócio do backend

Este arquivo é a referência principal para continuar o desenvolvimento. O `README.md` apresenta a visão pública e resumida; este documento registra decisões, pendências técnicas e a ordem de execução.

---

## 🎯 Objetivo

O SGL automatiza e centraliza o controle de materiais em laboratórios de pesquisa e ensino.

O sistema deve permitir:

- cadastrar Unidades, Laboratórios, Usuários e Estagiários;
- manter um catálogo central de Produtos;
- controlar separadamente o estoque de cada Unidade;
- registrar entradas e saídas de materiais;
- monitorar estoque mínimo para reposição;
- criar, aprovar, rejeitar, entregar e cancelar Pedidos;
- vincular Pedidos opcionalmente a Projetos;
- manter histórico do que foi entregue a cada Laboratório;
- controlar risco, perecibilidade e validade dos Produtos;
- futuramente, autenticar usuários, armazenar documentos e oferecer uma interface Vue.js.

---

## 🧭 Ordem oficial de desenvolvimento

1. **Corrigir decisões estruturais contraditórias.**
2. **Consolidar regras de negócio.**
3. Padronizar exceções e respostas HTTP.
4. Testar fluxos completos e falhas.
5. Implementar autenticação e autorização.
6. Migrar definitivamente para PostgreSQL.
7. Iniciar o frontend.

### Situação do item 1

- [x] Definir oficialmente a arquitetura do `EstoqueCentral` por Unidade.
- [x] Migrar o código para a nova definição.
- [x] Adaptar `EstoqueCentral`, DTO, Repository, Service e Controller.
- [x] Adaptar aprovação e cancelamento de Pedido para `Unidade + Produto`.
- [x] Atualizar o `DataInitializer`.
- [x] Alinhar a Unidade dos Usuários à Unidade de seus Laboratórios nos dados de teste.
- [x] Validar os principais fluxos pelo Postman.
- [ ] Revisar diagramas UML e ER após a alteração do código.
- [ ] Conferir o enum `Perfil` e padronizar a documentação.
- [ ] Revisar trechos antigos que ainda possam descrever Projeto como agrupador de Pedidos.

> O item 1 está funcionalmente concluído. Restam apenas revisão de diagramas e documentação complementar.

---

# 1. Decisão estrutural: EstoqueCentral por Unidade

Cada **Unidade possui seu próprio estoque central**.

No modelo relacional, `EstoqueCentral` representa o saldo de um Produto dentro de uma Unidade específica.

```text
EstoqueCentral
- id
- unidade
- produto
- quantidadeAtual
- quantidadeMinima
- ativo
```

A combinação abaixo é única:

```text
Unidade + Produto
```

Restrição no banco:

```sql
UNIQUE (unidade_id, produto_id)
```

Consequências:

- o mesmo Produto pode existir no estoque de várias Unidades;
- uma Unidade não pode possuir dois registros para o mesmo Produto;
- Produto permanece como catálogo global, sem saldo próprio;
- Pedido consome somente o estoque da Unidade do Laboratório solicitante;
- cancelamento devolve a quantidade ao mesmo estoque usado na aprovação;
- consultas de estoque baixo são filtradas por Unidade.

Exemplo:

```text
Produto: Álcool 70%
├── Unidade A — 50 frascos
├── Unidade B — 25 frascos
└── Unidade C — 80 frascos
```

## Situação da implementação

- [x] `EstoqueCentral` possui relacionamento `ManyToOne` com `Unidade`.
- [x] `EstoqueCentral` possui relacionamento `ManyToOne` com `Produto`.
- [x] A unicidade é composta por `unidade_id` e `produto_id`.
- [x] `EstoqueCentralDTO` possui `unidadeId`, nome e sigla da Unidade.
- [x] Repository consulta por `Unidade + Produto`.
- [x] Service e Controller oferecem consultas por Unidade.
- [x] Pedido localiza o estoque pela Unidade do Laboratório.
- [x] `DataInitializer` cria estoques válidos por Unidade.

---

# 2. Regras de movimentação do estoque

## Entrada

```text
quantidadeAtual = quantidadeAtual + quantidadeEntrada
```

## Saída manual

```text
quantidadeAtual = quantidadeAtual - quantidadeSaida
```

A operação deve falhar quando a quantidade solicitada for maior que a quantidade disponível.

```text
quantidadeAtual nunca pode ser negativa
```

## Quantidade mínima

Um item está com estoque baixo quando:

```text
quantidadeAtual <= quantidadeMinima
```

A consulta considera a Unidade.

## Baixa por Pedido

A regra oficial atual é:

```text
PENDENTE
→ APROVADO: reduz imediatamente o saldo disponível
→ ENTREGUE: confirma a entrega e registra o histórico
→ CANCELADO após aprovação: devolve a quantidade ao estoque
```

### Motivo da baixa na aprovação

A aprovação compromete a quantidade para aquele Pedido. Se a baixa acontecesse apenas na entrega, dois Pedidos poderiam ser aprovados utilizando o mesmo saldo disponível.

Exemplo:

```text
Saldo: 10
Pedido A aprovado: 7
Pedido B tenta aprovar: 5
```

Após a aprovação do Pedido A, o saldo disponível passa a ser 3. Assim, o Pedido B não pode ser aprovado com 5 unidades.

### Regras obrigatórias

- a aprovação reduz somente `quantidadeAprovada`;
- a entrega não reduz o estoque novamente;
- o cancelamento de Pedido `APROVADO` devolve `quantidadeAprovada`;
- o cancelamento de Pedido `PENDENTE` não altera estoque;
- Pedido `ENTREGUE` não pode ser cancelado pelo fluxo comum;
- a aprovação deve permanecer `@Transactional`;
- se qualquer item falhar, nenhuma baixa parcial deve permanecer.

---

# 3. Produto, estoque e histórico

## Produto

`Produto` é um catálogo central. Ele descreve o material, mas não define quanto existe disponível.

O Produto não pertence diretamente a uma Unidade ou Laboratório.

## EstoqueCentral

É o saldo disponível de um Produto dentro de uma Unidade.

É responsável por:

- entrada;
- saída;
- quantidade atual;
- quantidade mínima;
- baixa na aprovação de Pedido;
- devolução no cancelamento;
- alerta de reposição.

## HistoricoLaboratorio

`HistoricoLaboratorio` não representa estoque disponível.

Ele registra que um Laboratório recebeu determinado Produto por meio de um Pedido.

A entrega deve gerar o histórico, mas não realizar uma segunda baixa no `EstoqueCentral`.

---

# 4. Fluxo de Pedido

```text
Pedido
→ Laboratório
→ Unidade do Laboratório
→ EstoqueCentral localizado por Unidade + Produto
```

Fluxo principal:

1. usuário cria o Pedido;
2. o Pedido possui pelo menos um ItemPedido;
3. o Pedido pertence a um Laboratório;
4. Projeto é opcional e, quando informado, deve pertencer ao mesmo Laboratório;
5. o status inicial é `PENDENTE`;
6. o Pedido pode ser aprovado ou rejeitado;
7. a quantidade aprovada pode ser menor que a solicitada;
8. o estoque é localizado por `Unidade + Produto`;
9. a aprovação reduz imediatamente o estoque;
10. a entrega cria o histórico do Laboratório;
11. o cancelamento de Pedido aprovado devolve o estoque.

Fluxo de status:

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

Regras importantes:

- Pedido de uma Unidade não pode consumir estoque de outra;
- quantidade aprovada não pode superar a solicitada;
- estoque nunca pode ficar negativo;
- aprovação deve ser transacional;
- apenas Pedido aprovado pode ser entregue;
- Pedido entregue ou rejeitado não volta ao estado anterior.

---

# 5. Testes concluídos em 04/08/2026

Os testes manuais pelo Postman foram concluídos com sucesso para os principais fluxos da migração.

- [x] listar todos os estoques;
- [x] listar estoque por Unidade;
- [x] buscar estoque por `Unidade + Produto`;
- [x] confirmar o mesmo Produto em Unidades diferentes com saldos independentes;
- [x] criar estoque para Produto em outra Unidade;
- [x] bloquear duplicidade da combinação `Unidade + Produto`;
- [x] entrada manual;
- [x] saída manual;
- [x] bloquear saída maior que o saldo;
- [x] consultar estoque baixo por Unidade;
- [x] criar Pedido válido;
- [x] bloquear Pedido com Produto sem estoque na Unidade do Laboratório;
- [x] aprovar Pedido e reduzir o estoque;
- [x] bloquear aprovação maior que a quantidade solicitada;
- [x] bloquear aprovação com estoque insuficiente;
- [x] cancelar Pedido aprovado e devolver a quantidade;
- [x] entregar Pedido aprovado;
- [x] bloquear cancelamento de Pedido entregue.

Observação técnica identificada durante os testes:

- `MovimentacaoEstoqueDTO` utiliza `quantidadeMovimentada`;
- os métodos de entrada e saída devem utilizar `getQuantidadeMovimentada()`;
- futuramente é recomendado criar um DTO menor específico para entrada e saída manual, evitando campos redundantes.

---

# 6. Papel do Projeto

`Projeto` é um contexto opcional do Pedido.

Ele não cria estoque separado, não controla saldo e não é um agrupador obrigatório de Pedidos.

```text
Pedido pode possuir projetoId ou projetoId = null
```

Quando informado, o Projeto deve pertencer ao mesmo Laboratório do Pedido.

---

# 7. Padrões arquiteturais

```text
Controller → DTO → Service → Repository → Entity → Banco
```

- Controller recebe e devolve DTOs.
- Service contém regras de negócio.
- Repository trabalha somente com Entity.
- Entity e Repository não conhecem DTO.
- Relacionamentos são expostos nos DTOs principalmente por IDs.

Usar injeção por construtor com `@RequiredArgsConstructor`.

Transações:

- escrita: `@Transactional`;
- leitura: `@Transactional(readOnly = true)`.

Operações compostas, principalmente aprovação, entrega e cancelamento, devem permanecer atômicas.

---

# 8. Próxima etapa — Consolidar regras de negócio

O próximo passo oficial é o **item 2 do roadmap**.

Prioridade recomendada:

1. validar se o Usuário pertence ao mesmo Laboratório informado no Pedido;
2. validar se a Unidade do Usuário corresponde à Unidade do Laboratório;
3. validar se o Projeto informado pertence ao Laboratório do Pedido;
4. impedir uso de Unidade, Laboratório, Usuário, Projeto, Produto ou Estoque inativos;
5. revisar todas as transições de status do Pedido;
6. revisar regras de risco, perecibilidade e validade;
7. eliminar validações duplicadas ou conflitantes.

A primeira implementação recomendada é consolidar as regras do método `PedidoService.criar`, pois ele concentra os vínculos entre Usuário, Laboratório, Projeto, Produto e Estoque.

---

# 9. Etapas posteriores

## Etapa 3 — Padronizar exceções e respostas HTTP

Planejar exceções de domínio:

- recurso não encontrado;
- recurso duplicado;
- entidade em uso;
- estoque insuficiente;
- transição de status inválida;
- regra de negócio violada.

Padronizar respostas `400`, `401`, `403`, `404`, `409` e demais códigos necessários.

## Etapa 4 — Testar fluxos completos e falhas

Os testes manuais principais foram executados, mas esta etapa futura deve incluir testes automatizados e cenários de rollback, concorrência e integridade referencial.

## Etapa 5 — Autenticação e autorização

- Spring Security;
- JWT;
- login por e-mail e senha;
- autorização por Perfil;
- restrição por Unidade e Laboratório.

## Etapa 6 — PostgreSQL

- configurar perfis de ambiente;
- criar banco definitivo;
- preparar migrations;
- revisar constraints e tipos;
- remover dependência do H2 como ambiente principal.

## Etapa 7 — Frontend

O frontend Vue.js só deve ser iniciado após:

- regras estabilizadas;
- respostas HTTP padronizadas;
- autenticação funcionando;
- PostgreSQL configurado;
- backend validado ponta a ponta.

---

# 10. Funcionalidades futuras

- Swagger/OpenAPI;
- upload e download de documentos;
- relatórios gerenciais;
- exportação PDF e Excel;
- notificações por e-mail;
- dashboard;
- código de barras e QR Code;
- relatórios de risco;
- controle avançado de validade;
- auditoria detalhada de movimentações;
- possível separação futura entre saldo físico, reservado e disponível.

---

## 📂 Referências

- [`README.md`](README.md) — visão geral do projeto.
- [`docs/codigos-referencia-pedidos.md`](docs/codigos-referencia-pedidos.md) — referência do fluxo de Pedidos.
- `docs/diagramas/` — diagramas UML, ER e arquitetura.

---

## 📝 Histórico resumido de decisões

| Data | Decisão |
|---|---|
| 13/07/2026 | Início do SGL e definição de Java, Spring Boot, Vue.js e PostgreSQL |
| 17/07/2026 | Adoção de DTOs, validação e exception handler global |
| 17/07/2026 | Substituição de Estudante/Pesquisador por Usuario + Perfil |
| 20/07/2026 | Responsável do Laboratório passou a ser Usuario |
| 21/07/2026 | Separação entre Produto, EstoqueCentral e HistoricoLaboratorio |
| 24/07/2026 | Projeto definido como vínculo opcional no Pedido |
| 31/07/2026 | Revisões dos CRUDs de Unidade, Usuario e Projeto |
| 03/08/2026 | Estagiario migrou para herança JOINED de Usuario |
| 04/08/2026 | EstoqueCentral definido e implementado por combinação Unidade + Produto |
| 04/08/2026 | Baixa de estoque definida na aprovação; entrega apenas confirma e registra histórico |
| 04/08/2026 | Testes manuais de estoque e Pedido concluídos com sucesso |
