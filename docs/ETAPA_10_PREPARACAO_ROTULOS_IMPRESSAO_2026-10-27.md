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
