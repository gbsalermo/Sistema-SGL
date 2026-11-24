# Demo de apresentação — Embrapa Mandioca e Fruticultura

> Cenário criado exclusivamente para demonstração do SGL. Os nomes de pessoas, e-mails, pedidos, lotes, movimentações e ocorrências são fictícios. Os nomes/áreas dos laboratórios foram inspirados na infraestrutura pública da Embrapa Mandioca e Fruticultura.

## Integração e vigência

A massa de demonstração foi criada originalmente na branch `demo/apresentacao-embrapa-30-dias` e integrada à `main` pelo PR #15. Esse é o histórico de origem, **não uma instrução para usar `main` como referência atual**: o estado funcional vigente do SGL é a `release`, com GitLab institucional como fonte canônica.

**Atualização de autenticação (24/11/2026):** as instruções antigas de login com e-mail e senha `123456` foram substituídas pelo fluxo de **Entrar como desenvolvedor**, com login curto **sem senha**, disponível somente em `dev` e `demo`.

## Como funciona

O perfil `demo` usa **H2 em memória** e não acessa o PostgreSQL de desenvolvimento ou a base institucional. Ao iniciar, `DemoDataInitializer` cria um cenário fictício de aproximadamente 30 dias de operação, com datas relativas à execução. Ao encerrar a API, os dados da demonstração são descartados e reconstruídos no próximo início.

## Executar a demonstração

Na **raiz do repositório backend**, que contém `pom.xml` e `mvnw` (use uma cópia local da versão desejada; não é necessário mudar de branch para ativar o perfil):

### Git Bash / Linux / macOS

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

### Windows PowerShell / CMD

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=demo
```

Alternativamente, configure `SPRING_PROFILES_ACTIVE=demo` na execução local da IDE. Confirme nos logs que o perfil ativo é `demo`. A API usa a porta `8080`.

### Verificar a configuração de login

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/auth/config
```

Resultado esperado:

```json
{"devLoginAtivo":true}
```

No **frontend local**, configure `VITE_API_BASE_URL=http://localhost:8080/api` (por exemplo, no arquivo `.env.local`), reinicie o Vite e abra a tela de acesso. O botão **Entrar como desenvolvedor** aparece quando a API consultada informa `devLoginAtivo: true`.

## Contas de demonstração — autenticação atual

Selecione **Entrar como desenvolvedor**, preencha o **login curto sem o domínio de e-mail** e **não informe senha**. O login normal **Entrar** chama a autenticação corporativa e não é o caminho apropriado para contas fictícias.

| Papel | Pessoa fictícia | Login curto |
|---|---|---|
| Administradora | Mariana Souza | `mariana.souza` |
| Gestor | Ricardo Almeida | `ricardo.almeida` |
| Pesquisador / solicitante | Paulo Nascimento | `paulo.nascimento` |
| Estagiária | Ana Clara Barbosa | `ana.barbosa` |

O endpoint local é `POST /api/v1/auth/dev-login`, exclusivo dos perfis `dev` e `demo`, e só autentica usuários ativos com origem `DEV`. A antiga senha `123456` ainda aparece na origem histórica da massa, mas **não é usada no login atual da interface**.

Teste opcional do endpoint, sem imprimir o JWT:

```powershell
$sessao = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/dev-login" -Method POST -ContentType "application/json" -Body '{"login":"ricardo.almeida"}'
$sessao.usuario.perfil
```

A resposta esperada para esse exemplo é `GESTOR`. A sessão recebe um JWT como no restante da aplicação.

### Problemas comuns

- **O botão não aparece:** consulte `/api/v1/auth/config` e confira perfil ativo e `VITE_API_BASE_URL`. Reinicie o Vite após alterar o ambiente.
- **`dev-login` retorna 404:** a API provavelmente não está com `dev` ou `demo` ativo, ou o frontend está consultando outra instância.
- **`dev-login` retorna 401:** confira o login curto exato, o estado ativo da conta fictícia e os logs da carga `DemoDataInitializer`.
- **O botão `Entrar` retorna erro corporativo/503:** esse botão usa `POST /api/v1/auth/login`. Sem credenciais do gateway institucional no ambiente local, o login corporativo pode ficar indisponível; use o caminho DEV para a demonstração.
- **Sessão expirada ou 401 após trocar ambiente:** saia da sessão anterior e autentique novamente na API DEMO, sem reaproveitar JWT de outra instância.

O DEMO é **exclusivamente local e fictício**. Não habilite login sem senha, H2 ou os inicializadores de dados em produção.

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

## Ampliação para a Etapa 10 — rótulos (28/10/2026)

Na branch `collab/etapa-10-rotulos-impressao`, a carga do profile `demo` também demonstra **Pedidos de Soluções** (pendente, em preparação, entregue) e rótulos de Resíduos com **estado físico, medidas informadas/confirmadas e observações**. Foi adicionado um resíduo sólido, sem pictograma, para exercitar versões menores da etiqueta.

O passo a passo, nomes dos registros e checklist de impressão estão em `docs/ETAPA_10_CENARIO_GESTAO_ROTULOS_2026-10-28.md`. Os dados continuam isolados no H2 e só ficam disponíveis ao executar o backend com profile `demo`; no profile `dev`, a carga não ocorre.

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

A carga de demonstração está integrada à `main`, porém continua isolada pelo profile `demo`. Ela não é executada no profile normal de desenvolvimento e não altera o PostgreSQL usado no dia a dia. O objetivo é permitir uma apresentação rica e repetível sem poluir o banco de desenvolvimento.
