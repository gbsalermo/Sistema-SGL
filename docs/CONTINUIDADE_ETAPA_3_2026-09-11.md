# Continuidade SGL — Etapa 3

**Checkpoint inicial:** 11/09/2026  
**Atualizado em:** 16/09/2026

## Estado atual

As Etapas 1 e 2 da pré-produção foram concluídas e validadas.

```text
Etapa 1 — padrão visual global       ✅
Etapa 2 — Dark Mode definitivo       ✅
Etapa 3 — Resíduos                   ⏭ atual
```

## Fechamento da Etapa 2

Frontend:

```text
repositório: gbsalermo/SGL-FRONTEND
PR: #50
squash merge: a3fff4fa8edb6b8900c4a5b359dbfc0245afb87c
```

O fechamento incluiu:

- arquitetura única de tema por `themeService.ts`;
- persistência de `sgl.theme`;
- sincronização DOM/body + Vuetify;
- paleta Dark em tokens;
- cobertura completa das interfaces autenticadas;
- Login, 404 e rótulos de impressão preservados em Light;
- remoção de `dark-mode.css`, `dark-mode-runtime.css`, `dark-mode-coverage.css` e `dark-mode-consistency.css`;
- correção de cards/estados vazios claros remanescentes;
- azul de ações primárias escurecido no Dark;
- semântica final de movimentações também em Relatórios:
  - Entrada/Devolução → verde;
  - Saída → azul;
  - Ajuste → âmbar;
  - Descarte por vencimento → vermelho.

Nenhuma regra de negócio, contrato HTTP ou payload do backend foi alterado.

## Etapa atual

**Etapa 3 — Refinamentos do fluxo atual de Resíduos.**

Ordem:

```text
3.1 remover redundância de análise                              ✅ concluída e validada
→ 3.2 ampliar dados/classes/segurança/responsabilidade do Resíduo 🔧 em execução
   3.2.1 tratamento + estado físico + responsabilidade inicial    ✅ concluída
   3.2.2 classes de Resíduo                                       ✅ concluída
   3.2.3 segurança/EPI + snapshot                                 🧪 implementada / validar
   3.2.4 integração dos novos dados no frontend                   🧪 implementada / validar
→ 3.3 separar geração/visualização da permissão de impressão      🧪 implementada / validar
```

O acabamento visual definitivo, os templates adaptados e a infraestrutura Zebra não pertencem mais à Etapa 3. Eles foram consolidados na **Etapa 10 — Rótulos e impressão operacional**, após a estabilização de Produto, Resíduo e Solução.

## Fechamento da 3.1 — 16/09/2026

A subetapa 3.1 foi implementada no frontend e validada manualmente na interface.

Alterações consolidadas:

- removido o pseudo-filtro `PENDENTES_ANALISE` da Gestão de Resíduos;
- removida a navegação `filtro=pendentes-analise`;
- preservadas as abas baseadas nos status reais `INFORMADO`, `EM_ANALISE`, `LIBERADO_PARA_ARMAZENAMENTO`, `ARMAZENADO_TEMPORARIAMENTE` e `DESPACHADO`;
- Dashboard preserva o KPI agregado de resíduos pendentes/em análise, mas navega para a tela geral ou usa `status=<StatusResiduo>` quando aponta para um resíduo específico;
- nenhum código funcional de backend foi alterado;
- nenhum DTO, contrato HTTP, migration ou regra de domínio foi alterado.

Frontend — commits da 3.1:

```text
11f420c  feat: remover redundância da análise de resíduos
833c59d  feat: alinhar navegação do dashboard de resíduos
```

## Fechamento da 3.2.1 — 16/09/2026

A primeira parte da 3.2 foi concluída no backend.

Alterações consolidadas:

- novo estado físico estruturado do Resíduo por `EstadoFisicoResiduo`;
- registro de tratamento realizado e descrição condicional;
- migration `V13__expand_basic_residuo_data.sql`;
- substituição de `gestorResponsavel` por `gestorRecebedorInicial`;
- o Gestor que recebe inicialmente passa a ser o mesmo responsável por analisar/liberar;
- armazenamento e despacho podem ser executados por outros Gestores sem perder o responsável inicial;
- histórico continua registrando o ator real de cada transição;
- contrato de resposta já utiliza a nomenclatura definitiva `gestorRecebedorInicial`.

## Fechamento da 3.2.2 — 16/09/2026

A subetapa de Classes de Resíduo foi concluída no backend.

Alterações consolidadas:

- catálogo `ClasseResiduo` por Unidade;
- códigos e descrições editáveis, com inativação em vez de enum rígido;
- classes iniciais A, B, F e H cadastradas pela migration `V14__create_residue_classes.sql`;
- isolamento por tenant preservado;
- múltiplas classes permitidas;
- classes informadas pelo Solicitante e confirmadas pela Gestão preservadas separadamente;
- snapshot de código/descrição mantido em `ResiduoClasse`, evitando alteração retroativa do histórico;
- DTOs e contratos do backend preparados para `classesInformadasIds` e `classesConfirmadasIds`.

## Implementação da 3.2.3 e 3.2.4 — 16/09/2026

Implementado no backend:

- enum estruturado de medidas de segurança: luvas, óculos, proteção respiratória, jaleco/avental e outro;
- recomendações de segurança no Produto;
- snapshots independentes de segurança informada e confirmada no Resíduo;
- migration `V15__add_residue_safety_information.sql`;
- observação obrigatória quando a medida `OUTRO` é usada.

Implementado no frontend:

- Produto permite manter recomendações de segurança;
- nova aba administrativa para Classes de Resíduo;
- formulário do Solicitante inclui procedência/uso, estado físico, tratamento, classes e segurança;
- Produtos associados aos componentes fornecem sugestões de EPI sem sobrescrever a escolha do Solicitante;
- análise da Gestão confirma classes e segurança separadamente;
- telas de consulta mostram valores informados e confirmados preservando o histórico.

Validação manual integrada permanece pendente.

## Implementação da 3.3 — 16/09/2026

A identificação e a disponibilidade do rótulo foram separadas da autorização de impressão.

Regras implementadas:

- código SGL continua sendo gerado na criação do Resíduo;
- QR também passa a existir desde o registro inicial;
- Resíduos antigos sem QR recebem a identificação faltante ao abrir a prévia;
- a Gestão pode visualizar a prévia em `INFORMADO` e `EM_ANALISE`;
- a prévia utiliza os dados disponíveis naquele momento e identifica classificação ainda não confirmada;
- impressão permanece bloqueada enquanto o Resíduo estiver `INFORMADO` ou `EM_ANALISE`;
- impressão é liberada em `LIBERADO_PARA_ARMAZENAMENTO`, `ARMAZENADO_TEMPORARIAMENTE` e `DESPACHADO`;
- template definitivo, Zebra e infraestrutura física continuam exclusivamente na Etapa 10.

## Próximo passo exato

Executar a **validação integrada da Etapa 3**. Se 3.2 e 3.3 passarem, marcar a Etapa 3 como concluída e iniciar a Etapa 4.

O plano canônico permanece:

`docs/PLANO_PRE_PRODUCAO.md`
