# Etapa 10 — Rótulos e impressão | preparação e auditoria inicial

**Aberta em:** 27/10/2026
**Branches:** `collab/etapa-10-rotulos-impressao` no backend `gbsalermo/Sistema-SGL` e frontend `gbsalermo/SGL-FRONTEND`, derivadas das respectivas `main` após integração da Etapa 9.
**Estado:** preparação documental; nenhuma mudança de layout ou contrato aplicada; referência visual do cliente ainda será fornecida pelo mantenedor.
**Escopo inicial confirmado:** melhorar o rótulo **atual** aproveitando seletivamente elementos do rótulo de referência do cliente, mantendo dados reais, legibilidade, padrão visual do SGL e impressão funcional. Não copiar cegamente campos ou formas sem aderência aos contratos.

## Inventário atual

### Frontend — produtos
- Rota `/produtos/:id/rotulo` e `src/modules/produtos/views/RotuloProdutoView.vue`.
- Template `src/modules/produtos/components/rotulo/ProdutoRotuloModelo.vue`.
- Dados do Produto por `produtoService.buscarPorId(id)` / `GET /api/v1/produtos/{id}`; sem endpoint separado de rótulo de Produto.
- Prévia com tamanhos 90/135/180/190 mm e largura customizável 70–190 mm; proporção 180 × 125 mm; folha A4 retrato; impressão via navegador.
- Referência técnica existente: `docs/ROTULO_PRODUTOS.md`.

### Frontend — resíduos
- Rota `/residuos/:id/rotulo` e modelos `ResiduoRotuloModelo.vue` / `ResiduoRotuloModeloAjustado.vue`; conferir no router qual é o componente ativo antes de editar. Existem views `RotuloResiduoView.vue` e `RotuloResiduoAjustadoView.vue`.
- Prévia e impressão seguem controle `impressaoPermitida` do contrato; o usuário pode visualizar antes da liberação, mas impressão só ocorre quando autorizada.
- Há documentação de versões com **180 × 108 mm** e da versão ajustada **180 × 125 mm**; não misturar dimensões ou modelos sem verificar a rota efetiva.
- `docs/ROTULO_RESIDUOS.md` contém trechos históricos sobre QR Code e versões, que não devem ser tratados automaticamente como comportamento atual.

### Backend
- `RotuloResiduoResponseDTO` inclui `codigoRastreio`, `status`, `impressaoPermitida`, confirmação de classificação, riscos, composição, laboratório/gerador, recipiente, quantidade/unidade, origem, armazenagem e destino.
- Produto usa dados do catálogo, sem duplicação de lógica em endpoint de etiqueta.
- Alterações visuais devem primeiro reutilizar campos já existentes. Novos dados persistidos, se realmente indispensáveis, exigem contrato explícito e testes; não criar migrations preventivamente.

## Ordem de trabalho

1. **Receber e analisar o rótulo de referência do cliente.** Comparar campos, hierarquia, tipografia, pictogramas, sinalização e dimensões com os layouts SGL existentes. Anotar o que aproveitar, adaptar e descartar; não presumir aprovação de reprodução literal.
2. **Confirmar rótulo-alvo e view ativa:** Produto, Resíduo ou ambos. Inspecionar a rota real, componentes, estilos `@media print`, campos disponíveis e regras de impressão.
3. **Fechar contrato visual e dados** antes de alterações estruturais: campos obrigatórios, opcionais, ausência de dados, condições de riscos, nome longo, composição extensa, identificação rastreável e logo institucional.
4. **Implementação incremental por componente**, preservando padrões do sistema e funcionalidades atuais de escala/impressão. Validar em claro/escuro e em prévia/impressão.
5. **Testes:** build TypeScript/Vue, testes existentes, fluxos de permissões (Resíduos), roteamento, dados faltantes/longos, impressão A4, tamanhos e quebras; comparar prévia e PDF/diálogo de impressão. Mudanças no backend exigem `./mvnw clean verify` e testes de contrato.
6. **Sincronizar documentação** e encerrar após validação do mantenedor. Sem merge automático.

## Regras de preservação

- Não alterar FIFO/FEFO, consumo de lotes, pedidos, Resíduos ou autorização de impressão apenas por exigência visual.
- Não inferir dados regulatórios ou pictogramas que não estejam confirmados no cadastro/contrato.
- Não incluir QR Code, Zebra ou integrações físicas sem decisão explícita.
- Rótulos devem permanecer legíveis e rastreáveis nas dimensões efetivamente suportadas; não declarar certificação regulamentar sem avaliação própria.
- GitLab institucional continua fonte canônica; trabalho nas branches colaborativas do GitHub; merge pela pessoa responsável após build/testes e conferência.

## Pendência de entrada

**Aguardar imagem/arquivo do rótulo de referência do cliente**, para iniciar o comparativo visual e fechar o recorte de alterações. Até esse momento, preservar o layout atual sem mudanças.

## Decisão visual aprovada — capacidade da embalagem (28/10/2026)

**Não exibir “Capacidade da embalagem” nos rótulos de Produtos, Resíduos ou Soluções.** O elemento constava apenas dos esboços de referência, não dos componentes de rótulo atualmente implementados. A inspeção dos componentes `ProdutoRotuloModelo.vue` e `ResiduoRotuloModeloAjustado.vue` confirmou que eles já não apresentam esse campo; portanto, não adicionar campo novo nem modificar o estoque por causa desta decisão.

Preservar, quando aplicável, **quantidade real e unidade**, **estado físico**, **apresentação**, identificação e rastreabilidade, utilizando exclusivamente dados existentes e corretos para cada domínio. `Lote.conteudoPorApresentacao` e `RecipienteEstoque.capacidadeInicial` continuam necessários às regras internas do estoque e não foram alterados. A implementação visual futura dos modelos da Etapa 10 seguirá essa decisão.

## Revisão incremental do rótulo de Resíduo — 28/10/2026

Após validação visual inicial, o layout foi ajustado **sobre o componente atual do SGL**, preservando proporção 180 × 125 mm e controles de escala, em vez de substituir a identidade por um template genérico. Alterações: logotipo SGL ampliado; tipografia e hierarquia reforçadas; composição destacada; estado físico proveniente de `Residuo.estadoFisico`; “Gerador registrado” para não atribuir incorretamente a identidade de quem informou; observações vindas do registro (prioridade à observação da Gestão, com fallback à do gerador) e data de rotulagem no rodapé. Removida da etiqueta a instrução técnica sobre frases H/P.

**Responsável pela liberação:** o fluxo de liberação recebe um `Usuario gestor`, mas o DTO de rótulo atual não oferece um histórico imutável de quem liberou. Não apresentar nomes inferidos. A eventual captura/exibição exige verificar a trilha de auditoria antes de modificar contratos. **Frases H/P e palavra de advertência GHS:** ainda não são campos confirmados no contrato e não podem ser inventados a partir de risco alto/baixo ou somente pictogramas. **Orientação vertical:** discutida, mas não implantada; esta revisão mantém a proporção física atual para comparar antes de alterar impressão e escala.

**Validação necessária:** `./mvnw clean verify` após alteração do DTO, `npm run build`, impressão em tamanhos pequeno/médio/grande/A4, casos com descrições/observações extensas e ausência de estado físico, liberação de impressão em Resíduos, visual claro/escuro. Nenhum merge realizado.


## 28/10/2026 — Modelo de rótulo de Resíduos aprovado para validação

O rótulo ativo `ResiduoRotuloModeloAjustado.vue` foi atualizado na branch da Etapa 10 para seguir o esboço aprovado, mantendo **SGL somente no rodapé direito** e **Embrapa no rodapé esquerdo**. O cabeçalho exibe o nome institucional em texto, sem duplicar a imagem da marca SGL. Permanecem nome do resíduo, código de rastreio, quantidade e informações de Unidade/Laboratório/gerador registrado.

A área central contém pictogramas e risco identificado (com distinção entre informado e confirmado), composição, processo, estado físico, armazenamento, recipiente e destino previsto. A faixa inferior contém medidas de segurança e observações efetivamente registradas. O rodapé contém a descrição e as duas marcas. Os elementos do esboço (solventes, concentrações, EPI, observações e datas) eram **fictícios**: no SGL são exibidos somente valores provenientes do cadastro real, não valores fixos.

Contrato acrescido em `RotuloResiduoResponseDTO`: `dataInformacao`, `estadoFisico`, `medidasSeguranca`, `observacaoSeguranca`, `observacoesRotulo`. A fonte das medidas e da observação de segurança é **confirmada** quando a classificação está confirmada; antes, é **informada**. Não inferir confirmação a partir da presença de medidas. Se a Gestão confirmar ausência de risco, não reintroduzir pictogramas preliminares por fallback. As observações operacionais incluem indicação de autoria Gerador/Gestão conforme os campos existentes.

Não implementar frases H/P, palavra de advertência, telefone, QR Code, capacidade de embalagem, nome do gestor liberador ou identidade de quem clicou em “informar” sem dados validados. `geradorNome` é o gerador vinculado ao registro, não necessariamente o operador que submeteu a solicitação. `dataRotulagem` continua sendo a data de liberação existente no contrato; `dataInformacao` é a data original do cadastro. O fluxo de impressão e a restrição `impressaoPermitida` foram preservados.

**Validação pendente pelo mantenedor:** `./mvnw clean verify` (backend) e `npm run build` (frontend), rótulos informados/confirmados sem riscos, quantidade de componentes/pictogramas variável, campos vazios, observações e textos longos, tamanhos 90/135/180/190 mm, impressão A4 e bloqueio de impressão antes da liberação. A altura é dinâmica para evitar cortes; conferir se etiquetas extensas cabem na mídia física escolhida. Sem merge.

## Refinamento da hierarquia do rótulo de Resíduos (28/10/2026)

Após validação do rótulo DEMO de Meio BDA, o cabeçalho foi simplificado para manter **nome do SGL, Unidade e Laboratório**, com **Projeto, SCI e Atividade exibidos condicionalmente** quando presentes no contexto de origem do resíduo. O backend adicionou `projetoOrigemNome`, `sciOrigemNome` e `atividadeOrigemNome` ao `RotuloResiduoResponseDTO`, reutilizando snapshots já persistidos da Etapa 8. Para resíduos antigos com Projeto mas sem snapshot, o nome do Projeto associado serve como fallback; não inferir SCI ou Atividade ausentes.

**Gerador registrado, informado em, situação, indicador de prévia, data e responsável pela liberação** foram movidos do cabeçalho para um painel de rastreabilidade **ao lado do nível de risco e acima da composição/estado físico**, sem eliminar informações. A distinção entre o responsável pela liberação (evento auditado) e o gerador permanece. Foram preservados pictogramas, notas de segurança, descrição, logotipos e bloqueio de impressão.

**Pendência de validação:** executar `./mvnw clean verify` e `npm run build`; comparar rótulos com e sem Projeto/SCI/Atividade, nomes longos e prévias de 90/135/180/190 mm. A altura pode se expandir quando necessária, devendo ser conferida na impressão física. Nenhum merge executado.

## Implementação dos dois últimos documentos — 29/10/2026

### Rótulo de Solução
- Tela `/solucoes/:id/rotulo`, acessível em **Central de Soluções → Detalhes → Visualizar / imprimir rótulo** exclusivamente para pedidos `EM_PREPARACAO` ou `ENTREGUE`.
- A consulta usa `GET /v1/solucoes/pedidos/{id}` (já existente) e `GET /v1/movimentacoes/pedido?pedidoId={id}`. O nome exibido como **Preparação aprovada por** é o usuário associado às saídas de estoque `SAIDA` efetuadas na aprovação, não o solicitante nem a pessoa que entregou. O horário indicado é o da **movimentação de aprovação**, não uma data de preparo físico.
- Se o histórico não possuir saída, o responsável aparece como **Não identificado nas movimentações**. Dados contraditórios de vários aprovadores também são explicitamente identificados; não se inventa aprovação. A massa DEMO pode conter registros de status criados como fixture sem movimentos correspondentes.
- Rótulo com componentes efetivamente registrados e quantidades aprovadas quando disponíveis; sem presumir volume final, concentração, validade, capacidade, solvente, pictogramas ou frases H/P.
- Para pedidos pendentes, rejeitados ou cancelados, a prévia identifica o estado e o botão de impressão é bloqueado. O layout usa largura 90/135/180/190 mm, altura variável e marcas SGL/Embrapa.

### Documento de Entrada de Lote
- Tela `/lotes/:id/documento-entrada`, acessível em **Estoque → Detalhe → Lote → Documento de entrada** e no aviso de **Entrada registrada** logo após uma nova entrada.
- Dados dos endpoints `GET /v1/lotes/{id}`, `GET /v1/estoque-central/{id}` e `GET /v1/movimentacoes/lote?loteId={id}`, já existentes. Documento informativo **USO INTERNO — SEM VALOR FISCAL**, sem substituição de documento fiscal e sem novas autorizações.
- Exibe código do lote SGL e referência de fornecedor, apresentação e tipo, quantidade original de embalagens, conteúdo por embalagem, quantidade original na unidade-base, fracionamento, datas, origem/autor do primeiro movimento de entrada quando registrados e observações. **Não somar saldo atual como quantidade recebida**, e não inventar fornecedor nominal ou nota fiscal.
- O conteúdo refletirá o cadastro atual do lote e as movimentações preservadas; campos de lote que o sistema permite editar não são apresentados como snapshots imutáveis da entrada original.

### Regressão e limites
- Não foram criadas migrations, endpoints nem aprovações adicionais; todos os ajustes funcionais estão no frontend, na branch `collab/etapa-10-rotulos-impressao`.
- **Testes pendentes de execução local:** `npm run build`; impressões com diferentes larguras; Solução pendente, aprovada e entregue; Documento após entrada e pela consulta posterior; lotes fracionáveis e não fracionáveis; campos nulos/textos longos; responsividade e isolamento de Unidade.
- A altura aumenta conforme o conteúdo; antes de imprimir em mídia adesiva pequena, verificar se o conteúdo inteiro cabe na altura física disponível.
- Não há QR Code operacional nestas versões: a referência visual do cliente não justifica gerar conteúdo de QR sem endereço/código de leitura implementado e validado. Sem merge.

## Validação do rótulo de Soluções e acabamento de ações (29/10/2026)

O mantenedor **validou visualmente o rótulo de Soluções** durante a revisão da Etapa 10. Preservar o modelo, os dados e a regra de impressão a partir do status `EM_PREPARACAO`; não redesenhar sem nova solicitação. Essa validação de layout não substitui o `npm run build`, testes de fluxo nem conferência da impressão física.

No frontend, corrigido o botão **Cancelar preparação** em **Central de Soluções → Detalhes → Ações da solução**: o seletor `.admin-cancel-action` estava formatado apenas no rodapé do modal, e na área expandida aparecia com altura/padding incoerentes. A nova regra no CSS de Soluções aplica altura, espaçamento, raio, tipografia, borda, contraste vermelho e foco aos botões do conjunto operacional; o rodapé do modal e a lógica de cancelamento permanecem inalterados. Validar em tema claro/escuro e telas estreitas; sem merge.

## 10.5 — Primeira integração Zebra / ZPL (29/10/2026)

Iniciado suporte opcional à Zebra em `SGL-FRONTEND`, sem alterar a impressão A4 aprovada. O novo `zpl.ts` gera arquivo ZPL com medidas da mídia e resolução (203/300 dpi) e interrompe a exportação se os dados ultrapassarem a etiqueta. O `ZebraExportPanel.vue` aparece no Documento de Entrada e no Rótulo de Soluções, respeitando neste último a restrição de impressão para pedidos não aprovados. **O arquivo é baixado localmente; não há conexão direta ou envio automático para impressora.**

**Limitação crítica:** versão piloto textual, sem logotipos gráficos/pictogramas; não disponibilizada em Produto e Resíduo enquanto a representação de riscos e gráficos não estiver correta. Dimensões e desempenho devem ser validados com modelo real de impressora Zebra, resolução, largura de impressão, tipo de mídia e driver. Não declarar a Etapa 10 Zebra homologada somente porque o arquivo foi exportado.

**Arquivo principal de detalhamento:** `SGL-FRONTEND/docs/ETAPA_10_ZEBRA_PILOTO_ZPL_2026-10-29.md`. Teste de motor disponível em `src/modules/rotulos/zebra/zpl.test.mjs` (`node --experimental-strip-types --test src/modules/rotulos/zebra/zpl.test.mjs`). `npm run build` e validação física ainda pendentes. Sem merge.

## 10.5 — Redesenho da prévia Zebra em ZPL (03/11/2026)

A prévia gerada para a Solução foi conferida em um visualizador ZPL e o mantenedor pediu **mais hierarquia e acabamento visual**, mantendo os dados informativos. Aplicado no frontend um layout térmico monocromático com marcas oficiais SGL/Embrapa via rasterização local `^GFA`, cabeçalho, faixa de identificação em negativo, campos estruturados, tabela de componentes, observações e rodapé. O mesmo motor atende o Documento de Entrada, preservando diferença de conteúdo e o aviso sem valor fiscal. A exportação continua opcional, por download, sem envio à impressora e sem alterar os rótulos HTML.

**Aceite pendente:** novo build e testes automatizados (o aceite do piloto anterior não cobre o redesenho), comparação visual em simulador ZPL e impressão física em equipamento real quando disponível. Produtos/Resíduos não recebem ZPL incompleto sem pictogramas GHS válidos. Detalhes: `SGL-FRONTEND/docs/ETAPA_10_ZEBRA_PILOTO_ZPL_2026-10-29.md`. Nenhum merge.

## 10.5 — Alinhamento das logos Zebra (03/11/2026)

Após prévia visual do ZPL, corrigida no frontend a diferença de alinhamento entre SGL e Embrapa: o rasterizador recorta margens claras/transparentes dos PNGs oficiais antes de ajustar as artes nas caixas simétricas do cabeçalho. Adicionada função pura `recorteMarca.ts` com testes de detecção de conteúdo; não foram alterados os rótulos HTML, os dados nem as regras de impressão. **Revalidar renderização 90×125 mm / 203 dpi, build e testes locais; a impressão física continua pendente.** Detalhes: `SGL-FRONTEND/docs/ETAPA_10_ZEBRA_PILOTO_ZPL_2026-10-29.md`. Sem merge.

## 10.5 — Extensão da impressão Zebra para Produto e Resíduo (03/11/2026)

Após o aceite visual do ZPL de Soluções, o modelo térmico institucional passou a ser oferecido também nas telas de **Rótulo de Produto** e **Rótulo de Resíduo** (`SGL-FRONTEND`). Os dados são derivados dos contratos existentes, sem novas migrations/endpoints e sem alterar os rótulos A4. Os pictogramas cadastrados de risco agora são rasterizados integralmente a partir dos arquivos oficiais em `/assets/residuos/pictogramas/` e incluídos como gráficos ZPL; caso algum não seja carregado, o arquivo não é gerado. Para Resíduos, `impressaoPermitida` continua impedindo exportação antes da liberação. O cabeçalho conserva os ajustes de marcas/legenda aprovados no piloto de Soluções.

**Atenção:** não implica certificação regulatória, inclusão de dados GHS não cadastrados ou homologação física em qualquer modelo Zebra. Validar `npm run build`, os testes em `src/modules/rotulos/zebra/zpl.test.mjs`, prévias com diferentes riscos e comprimentos, resolução 203/300 dpi, bloqueio de Resíduos ainda não liberados e impressão real quando houver acesso ao equipamento. Referência: `SGL-FRONTEND/docs/ETAPA_10_ZEBRA_PILOTO_ZPL_2026-10-29.md`. Nenhum merge realizado.

## 10.5 — Tamanho Zebra automático para uso intuitivo (03/11/2026)

Após pedido do mantenedor para não exigir ajuste manual constante das medidas, o frontend passou a oferecer **Automático (recomendado)** por padrão e **Personalizado** como alternativa, no painel comum aos rótulos de Soluções, Produtos, Resíduos e Documento de Entrada. A altura é escolhida pelo próprio algoritmo de encaixe do ZPL entre `125/160/200/250/300 mm`, começando de `125 mm` para Solução/Entrada e `200 mm` para Produto/Resíduo. A largura configurada (inicialmente 90 mm) e a resolução (203 dpi) não são alteradas sem escolha do usuário. O tamanho sugerido é mostrado antes de baixar o arquivo; sem opção que caiba, a exportação automática é bloqueada com mensagem objetiva, preservando todos os dados e pictogramas.

A mudança é de **usabilidade no frontend**, não de mídia física: o usuário ainda precisa confirmar medidas da etiqueta instalada e compatibilidade da impressora. Implementação em `SGL-FRONTEND/src/modules/rotulos/zebra/ZebraExportPanel.vue`, `zpl.ts` e `marcasZpl.ts`; testes de recomendação acrescidos a `zpl.test.mjs`. **Aguardando build, suíte local e validação visual dos quatro documentos; sem merge.**

## 10.5 — Revisão compacta do Zebra de Resíduos (03/11/2026)

Após avaliação de prévia longa do resíduo `Master Mix vencido`, a impressão Zebra de Resíduos foi refinada: os pictogramas passam a aparecer com o título **RISCO**, ao lado do nome/identificador do Resíduo para etiquetas de largura a partir de 75 mm, respeitando todos os símbolos do cadastro. As seções operacionais utilizam espaçamento mais enxuto, e o modo Automático procura o **menor comprimento de mídia** que caiba sem truncar, começando em 125 mm, com alturas intermediárias até 300 mm. O cálculo e a conversão dos pictogramas compartilham as mesmas dimensões.

A alteração é específica da saída ZPL de Resíduos; não mexe na impressão A4 nem nas demais etiquetas. Continua obrigatório bloquear exportação sem a liberação da Gestão, sem todos os pictogramas ou quando o conteúdo exceder o tamanho. **Aguardando compilação/testes locais e prévia completa até o rodapé.** Detalhes no documento Zebra do frontend. Sem merge.

## 10.5 — Layout Zebra compacto estendido a Produtos (03/11/2026)

Por solicitação do mantenedor, a **mesma configuração Zebra compacta de Resíduos** foi aplicada também a **Produtos**. O gerador `SGL-FRONTEND/src/modules/rotulos/zebra/zpl.ts` considera `categoria: PRODUTO` no modo compacto: títulos, grades e seções usam o espaçamento reduzido já adotado em Resíduos. Quando houver pictograma registrado e a mídia tiver largura a partir de 75 mm, o símbolo e seu nome passam à coluna lateral ao lado do nome do produto, com cabeçalho **RISCO**, dispensando a seção vertical de pictogramas. Em mídias mais estreitas o pictograma permanece em sua seção própria.

A altura inicial recomendada de Produtos passa de 200 para **125 mm**, usando a mesma lista de tamanhos intermediários até 300 mm e o cálculo de encaixe do próprio gerador. Largura e DPI permanecem configuráveis e todos os dados de segurança, apresentação, armazenamento, fiscalização e observações continuam presentes. Exportação é bloqueada se um pictograma obrigatório não puder ser convertido ou se a etiqueta não comportar o texto. Ajustes exclusivamente no ZPL, **sem mudanças no rótulo A4 nem no backend**.

Testes de regressão adicionados em `src/modules/rotulos/zebra/zpl.test.mjs`: Produto com pictograma lateral e fiscalização, Produto sem pictograma e altura mínima automática. **Pendente executar `npm run build`, a suíte Node e conferir prévia completa (incluindo rodapé), especialmente em 90 mm / 203 dpi.** Sem merge.
