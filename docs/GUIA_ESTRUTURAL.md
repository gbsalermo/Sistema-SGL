# Guia Estrutural do Backend

## Camadas

### Controller

Recebe requisições HTTP, aplica Bean Validation, extrai parâmetros e chama o Service. Não deve calcular saldo, validar relacionamentos de domínio ou acessar Repository diretamente.

### DTO

Define o contrato da API. DTOs de entrada recebem IDs de relacionamentos e dados necessários para a operação. DTOs de saída evitam expor entidades JPA completas e ciclos de serialização.

### Service

Concentra casos de uso e regras de negócio. Métodos de escrita são transacionais; métodos de consulta usam `readOnly = true` quando possível.

### Repository

Fornece acesso ao banco com Spring Data JPA. Métodos derivados expressam consultas simples; consultas JPQL são usadas quando a regra exige intervalo, relacionamento ou condição composta.

### Model

Representa o domínio persistido. Entidades guardam estado e relacionamentos; regras que dependem de múltiplos repositórios permanecem no Service.

## Classes do domínio

### Unidade

Representa a instituição, unidade administrativa ou tenant operacional. É a fronteira usada para separar laboratórios e estoques.

### Laboratorio

Representa o local solicitante e receptor dos materiais. Pertence a uma Unidade e serve de vínculo para usuários, projetos, pedidos e histórico.

### Usuario

Representa uma pessoa que usa o sistema. O perfil indica a função prevista. A senha é armazenada como hash BCrypt. O campo `ativo` permite desativação sem apagar históricos.

### Estagiario

Especialização de Usuario com informações próprias do estágio, como período, bolsa e supervisão.

### Produto

Catálogo global do material. Não possui quantidade disponível. Os campos de risco e perecibilidade descrevem cuidados necessários.

Campos menos óbvios:

- `codigoReferencia`: identificador externo ou interno único do material.
- `localizacaoFisica`: posição de armazenamento, como sala, armário ou prateleira.
- `unidadeMedida`: unidade usada para expressar quantidades.
- `unidadeArmazenamento`: apresentação física, como “frasco de 1 L”.
- `risco`: nível geral de severidade.
- `tipoRisco`: natureza do risco.
- `descricaoRisco`: orientação complementar não representada pelo enum.
- `tipoPerecivel`: categoria de perecibilidade.
- `dataValidade`: validade atualmente associada ao cadastro do produto; futuramente pode migrar para lote.

### EstoqueCentral

Representa o saldo de um Produto em uma Unidade. `quantidadeAtual` é o saldo disponível e `quantidadeMinima` é o limite usado para alerta de reposição.

### MovimentacaoEstoque

Trilha de auditoria das alterações de saldo.

Campos importantes:

- `tipoMovimentacao`: efeito operacional, como entrada, saída ou devolução.
- `origem`: contexto que causou a movimentação, como pedido ou compra.
- `quantidadeAnterior`: saldo imediatamente antes da operação.
- `quantidadeAtual`: saldo imediatamente depois da operação.
- `quantidadeMovimentada`: quantidade adicionada ou retirada.
- `usuario`: pessoa responsável pela operação.
- `pedido`: vínculo opcional quando a alteração nasceu de um pedido.
- `estoqueCentral`: registro de saldo efetivamente alterado.

### Projeto

Contexto opcional do Pedido. Não controla estoque e não é obrigatório para solicitar material.

### Pedido

Agregado que representa uma solicitação. Guarda solicitante, laboratório, projeto opcional, status, observação, documento e itens.

### ItemPedido

Liga Pedido e Produto. `quantidadeSolicitada` registra o pedido original; `quantidadeAprovada` registra a decisão do aprovador e pode ser menor.

### HistoricoLaboratorio

Registra o material efetivamente entregue. Não representa saldo e não deve ser usado para realizar nova baixa.

## Services

### PedidoService

Orquestra o ciclo completo do pedido. As lógicas menos triviais são:

- `validarConsistenciaPedido`: garante que usuário, laboratório, unidade e projeto formem um conjunto coerente.
- `validarEntidadesAtivas`: bloqueia operação com cadastros inativos.
- `aprovar`: reduz saldo, grava quantidade aprovada e registra movimentação.
- `entregar`: cria histórico sem realizar segunda baixa.
- `cancelar`: devolve saldo quando o pedido já havia sido aprovado.

### EstoqueCentralService

Controla o saldo. Entrada, saída e descarte devem validar quantidade, calcular saldos e registrar movimentação na mesma transação.

### MovimentacaoEstoqueService

Responsável pelas consultas ao histórico de movimentações. Alterações de saldo devem nascer no caso de uso correspondente, evitando caminhos duplicados para modificar o estoque.

### ProdutoService

Aplica regras condicionais de risco, perecibilidade, código único e ativação. Os métodos auxiliares existem para manter criação e atualização coerentes.

### UsuarioService

Valida e-mail único, associa laboratório, protege senha com BCrypt e inativa usuários sem apagar seus vínculos históricos.

### HistoricoLaboratorioService

Disponibiliza consultas sobre entregas realizadas aos laboratórios. Não modifica o saldo central.

### ProjetoService

Valida laboratório e consistência das datas do projeto.

### EstagiarioService

Mantém os dados específicos de estágio e as restrições relacionadas ao perfil do usuário.

## Convenções de comentários no código

- Javadoc de classe explica o papel no domínio.
- Métodos CRUD óbvios não precisam repetir o nome em comentários.
- Métodos com transição de status, cálculo, consistência ou efeito colateral devem explicar a regra.
- Campos recebem comentário apenas quando o significado não é evidente pelo nome.
- Comentários devem explicar o motivo da regra, não traduzir cada linha de Java.
