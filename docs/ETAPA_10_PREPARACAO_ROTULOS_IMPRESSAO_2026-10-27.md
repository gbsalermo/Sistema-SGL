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
