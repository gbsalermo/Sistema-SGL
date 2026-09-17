# Etapa 8.2 — Inativação segura e delete lógico

**Vínculo no roadmap:** complemento oficial de `docs/PLANO_PRE_PRODUCAO.md` → **Etapa 8.2 — Avaliação de delete lógico**  
**Status:** requisito registrado para análise e implementação na Etapa 8.2  
**Origem da decisão:** revisão do comportamento atual de inativação dos cadastros operacionais

## Objetivo

Revisar o significado de **inativar** em cada entidade do SGL antes de consolidar o padrão de delete lógico.

A inativação não deve ser tratada como uma simples troca indiscriminada de `ativo=true` para `ativo=false`. Antes de permitir a operação, o backend deve verificar se a entidade ainda participa de dependências operacionais ativas que seriam quebradas, ocultadas ou deixadas inconsistentes pela inativação.

A prioridade é impedir que uma ação administrativa acidental quebre um fluxo que ainda está em andamento.

## Regra central

```text
Administrador/Gestão solicita inativação
→ backend verifica dependências ativas
→ existem pendências ou vínculos operacionais ativos?
   → SIM: recusar inativação e informar os impedimentos
   → NÃO: permitir inativação lógica
→ preservar todo o histórico já encerrado
```

A responsabilidade principal pela consistência deve ficar no **backend**, e não no Solicitante.

O frontend administrativo pode antecipar a informação e apresentar os motivos do bloqueio, mas a validação definitiva deve ocorrer no servidor para impedir que outras interfaces ou chamadas diretas contornem a regra.

## Princípios obrigatórios

1. **Não realizar cascata de inativação automaticamente por padrão.**
   Inativar uma entidade pai não deve silenciosamente inativar seus filhos ou vínculos operacionais.

2. **Não quebrar fluxos em andamento.**
   Se existir pedido, estoque utilizável, lote válido, vínculo ativo, projeto em andamento ou qualquer outra dependência necessária à operação, a inativação deve ser bloqueada até a pendência ser resolvida.

3. **Histórico encerrado não é impedimento por si só.**
   Movimentações antigas, pedidos encerrados, lotes consumidos/descartados e demais registros históricos devem continuar preservados e consultáveis depois da inativação.

4. **Inativação não equivale a exclusão física.**
   Registros que já participaram de operações devem manter identidade, referências e rastreabilidade.

5. **A entidade deve desaparecer apenas dos fluxos de nova utilização quando a inativação for válida.**
   O Solicitante não deve receber opções que já estejam inativas nem descobrir o problema somente ao finalizar uma solicitação.

6. **Reativação também deve ser analisada.**
   Nem toda entidade poderá necessariamente ser reativada sem validação; ciclos de vida definitivos devem ser respeitados.

7. **Estados de domínio têm prioridade sobre um booleano genérico.**
   Entidades com ciclo próprio podem exigir `ENCERRADO`, `DESCARTADO`, `FINALIZADO`, `INATIVO` ou equivalente em vez de apenas `ativo=false`.

## Entidades a revisar

A Etapa 8.2 deve revisar, no mínimo:

- Laboratório;
- Produto;
- Projeto;
- Estagiário/vínculos institucionais aplicáveis;
- Estoque central;
- Lote;
- modelos/cadastros de Resíduos criados nas etapas anteriores;
- Soluções criadas na Etapa 7;
- cadastros auxiliares reutilizáveis;
- demais entidades operacionais que possuam ação de inativação, encerramento ou exclusão lógica.

Unidade e Usuário devem respeitar as regras institucionais definitivas do projeto e não devem ganhar um CRUD/inativação manual comum apenas por causa desta revisão.

## Verificações esperadas por domínio

Os itens abaixo são critérios iniciais para a análise da Etapa 8.2. A implementação final deve ser definida entidade por entidade de acordo com o domínio estabilizado nas etapas anteriores.

### Produto

Antes de inativar um Produto, avaliar pelo menos:

- existência de estoque ativo com saldo utilizável;
- lotes ativos/válidos ainda disponíveis;
- pedidos pendentes ou aprovados que ainda dependam do Produto;
- movimentações/operações em andamento que ainda exijam o Produto;
- participação em Soluções ativas, após a Etapa 7;
- participação em modelos operacionais ativos que dependam dele.

Exemplo de regra-alvo:

```text
produto possui 6480 unidades válidas em estoque
→ inativação bloqueada
→ Gestão/Admin deve primeiro concluir o destino operacional desse saldo
```

Não é desejável permitir a inativação e deixar o Solicitante selecionar um produto que será rejeitado apenas no envio do pedido.

### Laboratório

Antes de inativar um Laboratório, avaliar pelo menos:

- usuários/vínculos institucionais ativos dependentes do laboratório;
- Estagiários ativos vinculados;
- Projetos ainda ativos/em andamento;
- Pedidos pendentes ou aprovados;
- Resíduos ainda em fluxo operacional;
- estoques, movimentações ou outros recursos ativos pertencentes ao laboratório, quando a modelagem definitiva utilizar essa relação;
- demais responsabilidades institucionais ainda atribuídas ao laboratório.

A existência de histórico antigo não deve impedir a inativação após todos os fluxos ativos serem encerrados.

### Projeto

Antes de encerrar/inativar um Projeto, avaliar pelo menos:

- vínculos ativos de participantes/Estagiários;
- Pedidos pendentes ou aprovados relacionados ao Projeto;
- operações ainda abertas que exijam a referência do Projeto;
- regra de ciclo de vida definida na Etapa 5.

Preferir o ciclo de vida próprio (`ENCERRADO` ou equivalente) quando ele representar melhor o domínio do que `ativo=false`.

### Estagiário e vínculos

A revisão deve considerar a modelagem definida na Etapa 5, distinguindo:

- inativação temporária;
- inativação por prazo indeterminado;
- encerramento definitivo;
- vínculo ativo com Projeto;
- preservação dos períodos e atividades históricas.

Não encerrar silenciosamente vínculos de Projeto como efeito colateral de uma ação genérica.

### Estoque central e Lote

Antes de inativar, avaliar:

- saldo disponível;
- lotes ainda utilizáveis;
- pedidos/aprovações que dependam daquele saldo;
- necessidade de descarte, transferência ou outra destinação operacional antes do encerramento;
- movimentações que estejam em processamento, quando aplicável.

Um lote consumido, vencido e devidamente descartado ou encerrado pode permanecer apenas para rastreabilidade histórica.

### Cadastros auxiliares, modelos e Soluções

Antes de inativar um cadastro reutilizável, verificar se ele está sendo utilizado por definições ou operações ativas.

Registros históricos devem preservar o dado necessário para interpretação futura. Quando o domínio exigir snapshot, alterações/inativações do cadastro-base não devem reescrever ocorrências antigas.

## Pendência ativa x histórico

A implementação deve distinguir explicitamente:

```text
DEPENDÊNCIA ATIVA
= impede a inativação

HISTÓRICO ENCERRADO
= permanece preservado, mas não impede a inativação por si só
```

Exemplos de dependência ativa:

- saldo de Produto ainda utilizável;
- lote ainda disponível;
- Pedido PENDENTE/APROVADO;
- Projeto em andamento;
- vínculo de Estagiário ativo;
- Resíduo ainda no ciclo operacional;
- Solução ativa que dependa de Produto que se pretende desativar.

Exemplos de histórico que normalmente deve apenas ser preservado:

- Pedido ENTREGUE/REJEITADO/CANCELADO;
- movimentação já concluída;
- lote totalmente consumido ou devidamente descartado;
- vínculo de Projeto já encerrado;
- Projeto encerrado;
- ocorrência operacional finalizada.

Os estados exatos devem ser revistos com o domínio definitivo disponível na Etapa 8.

## Experiência da Gestão/Admin

Quando a inativação for bloqueada, evitar erro genérico. A interface deve explicar **por que** a ação não pode ocorrer.

Exemplo conceitual:

```text
Não é possível inativar este produto.

Pendências encontradas:
- 6.480 unidades disponíveis em estoque;
- 2 lotes com saldo;
- 1 pedido pendente.
```

Quando fizer sentido, a interface poderá oferecer atalhos para visualizar as pendências, sem resolvê-las automaticamente.

Antes de confirmar uma inativação permitida, a interface deve deixar claro que:

- o registro deixará de estar disponível para novos fluxos;
- o histórico continuará preservado;
- a operação não exclui os registros anteriores.

## Experiência do Solicitante

O Solicitante deve receber somente opções efetivamente disponíveis para novas operações.

A regra-alvo é:

```text
cadastro inativo
→ não aparece como opção para nova solicitação/operação
```

Não depender de uma rejeição tardia no backend como comportamento normal da interface.

Mesmo assim, o backend continua validando o estado no momento da operação para proteger contra concorrência e dados desatualizados.

## Concorrência e consistência

A verificação de dependências e a inativação devem ser projetadas para evitar a situação:

```text
verifica que não há pendência
→ outra transação cria uma nova dependência
→ cadastro é inativado logo depois
```

Na implementação da Etapa 8.2, avaliar transação, locks e/ou outras garantias adequadas ao domínio para que a decisão seja atômica quando necessário.

## Auditoria mínima da ação

Na fase definitiva de autenticação/autorização/auditoria, a ação de inativação deve ser rastreável.

Avaliar registro de:

- entidade afetada;
- identificador público;
- usuário responsável;
- data/hora;
- motivo, quando exigido pelo domínio;
- estado anterior e novo estado.

A decisão sobre a estrutura definitiva de auditoria permanece alinhada à fase posterior de autenticação/autorização/auditoria do roadmap.

## Resultado obrigatório da Etapa 8.2

Antes de considerar a revisão de delete lógico concluída, produzir uma matriz por entidade contendo:

```text
Entidade
→ ação correta: inativar / encerrar / descartar / outra
→ quem pode executar
→ dependências que bloqueiam
→ históricos que não bloqueiam
→ efeito sobre novos fluxos
→ possibilidade/regra de reativação
→ mensagem apresentada quando bloqueada
→ necessidade de auditoria
```

Somente depois dessa matriz estar aprovada implementar ou consolidar as regras definitivas de inativação.

## Decisão registrada

A direção aprovada para o SGL é **inativação preventiva e segura**:

```text
não deixar o Administrador/Gestor quebrar uma operação ativa
em vez de
permitir a inativação e transferir o problema para o Solicitante
```

Essa revisão deve acontecer no fim das alterações funcionais, como parte da Etapa 8.2, pois depende do domínio definitivo de Resíduos, Projetos/Estagiários, Soluções/Pedidos e demais entidades construídas nas etapas anteriores.
