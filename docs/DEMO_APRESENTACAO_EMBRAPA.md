# Demo de apresentação — Embrapa Mandioca e Fruticultura

> Cenário criado exclusivamente para demonstração do SGL. Os nomes de pessoas, e-mails, pedidos, lotes, movimentações e ocorrências são fictícios. Os nomes/áreas dos laboratórios foram inspirados na infraestrutura pública da Embrapa Mandioca e Fruticultura.

## Branch

`demo/apresentacao-embrapa-30-dias`

A `main` não foi alterada.

## Como funciona

O profile `demo` usa um banco H2 em memória separado do PostgreSQL de desenvolvimento. Ao iniciar o backend, `DemoDataInitializer` cria automaticamente um cenário equivalente a aproximadamente 30 dias de uso.

As datas são relativas ao dia atual. Isso significa que alertas como "vence em 4 dias", movimentações de hoje, pedidos recentes e resíduos aguardando análise continuam coerentes mesmo se a demonstração for executada em outro dia.

Ao encerrar o backend, o banco H2 da demonstração é descartado. Ao iniciar novamente, o cenário é reconstruído do zero.

## Executar a demonstração

No repositório `Sistema-SGL`:

### Git Bash / Linux / macOS

```bash
git checkout demo/apresentacao-embrapa-30-dias
cd backend/sgl-backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

### Windows PowerShell / CMD

```powershell
git checkout demo/apresentacao-embrapa-30-dias
cd backend\sgl-backend
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=demo
```

O backend continua na porta padrão `8080`.

## Logins principais

Todos usam a senha `123456`.

| Papel | Usuário fictício | Login |
|---|---|---|
| Administradora | Mariana Souza | `mariana.souza@demo.sgl.local` |
| Gestor | Ricardo Almeida | `ricardo.almeida@demo.sgl.local` |
| Pesquisador / solicitante | Paulo Nascimento | `paulo.nascimento@demo.sgl.local` |
| Estagiária | Ana Clara Barbosa | `ana.barbosa@demo.sgl.local` |

Também existem outros técnicos, pesquisadores, analistas e estagiários vinculados aos laboratórios.

## Unidade e laboratórios

Unidade principal:

- Embrapa Mandioca e Fruticultura (`CNPMF`)

Laboratórios/áreas usados na simulação:

- Laboratório de Biologia Molecular
- Laboratório de Fitopatologia
- Laboratório de Entomologia
- Laboratório de Solos e Nutrição de Plantas
- Laboratório de Virologia
- Laboratório de Ecofisiologia Vegetal e Meteorologia
- Central de Soluções

## O que a massa de demonstração cobre

### Usuários e estagiários

Há perfis de ADMINISTRADOR, GESTOR, TÉCNICO, ANALISTA, PESQUISADOR e ESTAGIÁRIO, incluindo usuários ativos e inativos.

Os estagiários incluem vínculos CNPq, CAPES, bolsa institucional, voluntário e contratual, com exemplos de estágio ativo e estágio já encerrado.

### Produtos e estoque

A massa contém reagentes, materiais de consumo e produtos controlados, incluindo:

- Etanol 70%
- Hipoclorito de Sódio 2,5%
- Ácido Clorídrico 37%
- Kit de Extração de DNA Vegetal
- Master Mix PCR 2X
- Agarose Grau Molecular
- Ponteiras 1000 µL com filtro
- Luvas Nitrílicas
- Meio BDA preparado
- Metanol Grau HPLC
- Tampão PBS 10X
- Cloreto de Potássio P.A.
- Substrato ELISA TMB
- Formaldeído 37%

Há produtos sem risco, inflamáveis, corrosivos, tóxicos, biológicos e produtos fiscalizados.

### Lotes

O cenário inclui:

- lotes normais;
- lote vencendo em poucos dias;
- lotes vencendo em até 30 dias;
- lote vencido já descartado;
- lote vencido ainda ativo aguardando descarte, para gerar alerta crítico;
- produto com estoque abaixo do mínimo;
- lote novo recebido no dia da apresentação.

### Pedidos

O enum atual do SGL é:

`PENDENTE -> APROVADO -> ENTREGUE`

Também existem `REJEITADO` e `CANCELADO`.

Por isso, no cenário de apresentação, **ENTREGUE representa o pedido concluído**. Não foi criado um status artificial `CONCLUIDO`, porque ele não existe no domínio atual.

A massa contém pedidos:

- pendentes;
- pendentes urgentes;
- aprovados aguardando retirada;
- entregues/concluídos;
- rejeitados;
- cancelados com devolução de material.

### Movimentações

Há movimentações ao longo de aproximadamente um mês e também movimentações no dia atual:

- ENTRADA por compra;
- SAÍDA vinculada a pedido;
- AJUSTE de inventário;
- DEVOLUÇÃO após cancelamento;
- DESCARTE_VENCIMENTO.

As movimentações estão vinculadas a produto, estoque, lote, usuário e, quando aplicável, laboratório e pedido.

### Resíduos

Há exemplos em todas as etapas reais do módulo:

1. INFORMADO
2. EM_ANALISE
3. LIBERADO_PARA_ARMAZENAMENTO
4. ARMAZENADO_TEMPORARIAMENTE
5. DESPACHADO

Os resíduos possuem exemplos químicos e biológicos, componentes associados a produtos, nível/tipo de risco, recipiente, quantidade, projeto de origem, local temporário, destino previsto, código SGL, QR após liberação e histórico de mudanças de status.

## Situações prontas para aparecer no Dashboard

Ao entrar como gestor, o cenário foi preparado para alimentar os indicadores reais do frontend:

- pedidos pendentes e urgentes;
- Master Mix com estoque abaixo do mínimo e validade próxima;
- Luvas Nitrílicas abaixo do estoque mínimo;
- lote de Substrato TMB vencido aguardando ação;
- resíduos recém-informados e em análise;
- lotes vencendo nos próximos 7 e 30 dias;
- entradas e saídas registradas hoje;
- últimas movimentações distribuídas entre diferentes laboratórios.

## Roteiro sugerido para a apresentação

1. Entrar como **Ricardo Almeida (GESTOR)** e começar pelo Dashboard.
2. Mostrar os alertas: pedido urgente, estoque baixo, lote vencido e resíduo aguardando análise.
3. Abrir Pedidos e filtrar PENDENTE, APROVADO, REJEITADO, ENTREGUE e CANCELADO.
4. Abrir Estoque/Lotes e mostrar um produto normal, um estoque baixo, um lote próximo do vencimento e o TMB vencido.
5. Abrir Movimentações e demonstrar entrada, saída, ajuste, devolução e descarte.
6. Abrir Resíduos e percorrer um registro de cada status até DESPACHADO, mostrando o histórico e a rastreabilidade.
7. Mostrar usuários/estagiários, destacando vínculos ativos e encerrados.
8. Encerrar em Relatórios, usando a massa do mês para mostrar que os dados operacionais alimentam os relatórios do sistema.

## Observação importante

Essa carga é intencionalmente isolada pelo profile `demo`. Ela não deve ser mesclada à configuração normal de desenvolvimento sem uma decisão explícita. O objetivo é permitir uma apresentação rica e repetível sem poluir o banco PostgreSQL usado no desenvolvimento.
