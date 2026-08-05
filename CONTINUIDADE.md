# Continuidade do Projeto SGL

**Projeto:** Sistema de Gestão de Laboratórios  
**Última atualização:** 05/08/2026  
**Fase atual:** documentação estrutural e preparação para testes automatizados

Este arquivo registra o estado real do backend, as decisões já consolidadas e a ordem recomendada para continuar o desenvolvimento.

## Estado atual

### Concluído

- CRUDs de Unidade, Laboratório, Usuário, Estagiário, Produto e Projeto.
- Estoque central separado por combinação `Unidade + Produto`.
- Entrada e saída manual com alteração de saldo e histórico de movimentação.
- Descarte de produto vencido.
- Pedido com criação, aprovação, rejeição, entrega e cancelamento.
- Aprovação com baixa transacional do estoque.
- Registro de movimentação `SAIDA` durante a aprovação do pedido.
- Histórico de materiais entregues ao laboratório.
- Validações de consistência entre usuário, laboratório, unidade e projeto.
- Validações de risco, perecibilidade e código de referência do produto.
- Senhas armazenadas com BCrypt.
- Exclusão lógica de usuário por inativação.
- Revisão das mensagens de erro mais evidentes.
- Documentação estrutural, fluxo e fontes UML atualizados.

### Pendente

- Registrar movimentação de devolução ao cancelar pedido aprovado.
- Padronizar exceções de domínio e respostas HTTP.
- Criar testes automatizados de service e controller.
- Revisar concorrência de estoque e bloqueio de atualizações simultâneas.
- Implementar autenticação e autorização com JWT.
- Preparar migrations e PostgreSQL definitivo.
- Implementar consultas e endpoints de relatórios após a migração para PostgreSQL.
- Adicionar exportação de relatórios em PDF e Excel.
- Criar documentação OpenAPI.
- Iniciar o frontend.

## Decisões oficiais

### Produto e estoque

`Produto` é um catálogo global. Ele descreve o material, risco, perecibilidade e forma de armazenamento, mas não possui saldo.

`EstoqueCentral` representa o saldo de um produto em uma unidade. Sua identidade lógica é:

```text
Unidade + Produto
```

A mesma unidade não pode possuir dois registros de estoque para o mesmo produto.

### Movimentação

Toda alteração de saldo relevante deve gerar `MovimentacaoEstoque`, contendo:

- produto e registro de estoque afetado;
- usuário responsável;
- tipo e origem da movimentação;
- quantidade movimentada;
- saldo anterior e saldo resultante;
- data e observação;
- pedido ou laboratório quando aplicável.

### Pedido

O pedido nasce como `PENDENTE`.

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

- A criação valida os vínculos, mas não reserva saldo.
- A aprovação valida saldo e reduz `quantidadeAprovada`.
- A aprovação registra movimentação `SAIDA` com origem `PEDIDO`.
- A entrega cria `HistoricoLaboratorio` e não reduz o estoque novamente.
- O cancelamento de pedido aprovado devolve o saldo.
- Pedido entregue não pode ser cancelado pelo fluxo comum.

### Usuários

- O solicitante é `pedido.getUsuario()`.
- O aprovador é informado no DTO de aprovação enquanto a autenticação não está pronta.
- A movimentação de aprovação registra o aprovador como usuário responsável.
- Usuário é inativado, não removido fisicamente.
- A senha permanece protegida por BCrypt.

### Produto

- O código de referência é único.
- Na atualização, a verificação de duplicidade ignora o próprio produto.
- `NivelRisco.NENHUM` limpa tipo e descrição de risco.
- Produto com risco exige tipo de risco.
- Produto não perecível limpa data de validade e tipo de perecível.
- Produto perecível exige data de validade e tipo de perecível.

### Relatórios

A etapa de relatórios será iniciada após a migração definitiva para PostgreSQL, quando o modelo de dados e as consultas estiverem estabilizados.

A primeira versão deve disponibilizar endpoints JSON para:

- estoque baixo por Unidade;
- movimentações por período, produto, usuário e origem;
- pedidos por status, laboratório e período;
- produtos vencidos ou próximos do vencimento;
- materiais entregues por Laboratório;
- consumo de produtos por Unidade ou Laboratório.

Depois da validação das consultas, o backend poderá oferecer exportação em PDF e Excel. O frontend será responsável por selecionar filtros, exibir os resultados e iniciar os downloads.

## Fluxo técnico

```text
Cliente
  → Controller
  → DTO validado
  → Service transacional
  → Repository
  → Banco
  → DTO de resposta
```

As regras que dependem do estado do banco pertencem ao Service. Controller não deve implementar regra de negócio. Repository não deve trabalhar com DTO.

## Próxima ordem de trabalho

1. Registrar `DEVOLUCAO` durante cancelamento de pedido aprovado.
2. Criar exceções específicas para regras de negócio.
3. Padronizar o corpo das respostas de erro.
4. Criar testes automatizados dos fluxos de estoque e pedido.
5. Revisar concorrência de saldo.
6. Implementar Spring Security e JWT.
7. Migrar para PostgreSQL com migrations.
8. Implementar consultas e endpoints JSON de relatórios.
9. Implementar exportação de relatórios em PDF e Excel.
10. Adicionar OpenAPI.
11. Iniciar frontend e integrar visualização e download dos relatórios.

## Cenários prioritários de teste

- rollback quando um item falha durante aprovação;
- aprovação parcial;
- estoque insuficiente;
- produto vencido sem autorização;
- pedido aprovado cancelado e saldo devolvido;
- tentativa de cancelar pedido entregue;
- movimentação com saldo anterior e atual corretos;
- duplicidade de `Unidade + Produto`;
- atualização de produto sem falso conflito de código;
- atualização de usuário sem troca de senha;
- inativação repetida de usuário.

## Documentos de referência

- [`README.md`](README.md): apresentação e execução.
- [`docs/FLUXO_DO_SISTEMA.md`](docs/FLUXO_DO_SISTEMA.md): fluxo operacional completo.
- [`docs/GUIA_ESTRUTURAL.md`](docs/GUIA_ESTRUTURAL.md): papel das classes e camadas.
- [`docs/diagrama-uml-completo.puml`](docs/diagrama-uml-completo.puml): entidades e relacionamentos.
- [`docs/componentes.puml`](docs/componentes.puml): arquitetura em componentes.
- [`docs/sequencia-pedido.puml`](docs/sequencia-pedido.puml): sequência do pedido.

## Histórico recente

| Data | Decisão |
|---|---|
| 04/08/2026 | Estoque central consolidado por Unidade + Produto |
| 04/08/2026 | Baixa definida no momento da aprovação |
| 05/08/2026 | Movimentação registrada durante aprovação |
| 05/08/2026 | Validações de Produto revisadas |
| 05/08/2026 | BCrypt mantido para proteger senhas |
| 05/08/2026 | Usuário passou a ser inativado em vez de excluído |
| 05/08/2026 | Documentação estrutural e UML revisados |
| 05/08/2026 | Relatórios planejados para depois da migração ao PostgreSQL |
