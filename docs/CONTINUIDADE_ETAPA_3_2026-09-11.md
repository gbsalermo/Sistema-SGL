# Continuidade SGL — Etapa 3

**Checkpoint inicial:** 11/09/2026  
**Fechamento:** 17/09/2026

## Estado final

As Etapas 1, 2 e 3 da pré-produção foram concluídas e validadas.

```text
Etapa 1 — padrão visual global       ✅ concluída
Etapa 2 — Dark Mode definitivo       ✅ concluída
Etapa 3 — Resíduos                   ✅ concluída e validada
Etapa 4 — expansão de Resíduos       ⏭ próxima
```

O acabamento visual definitivo dos rótulos, templates adaptados e infraestrutura Zebra continua fora deste escopo e permanece na **Etapa 10 — Rótulos e impressão operacional**.

---

## 3.1 — Remover redundância de análise ✅

Fechada e validada em 16/09/2026.

Alterações consolidadas:

- removido o pseudo-filtro `PENDENTES_ANALISE` da Gestão de Resíduos;
- removida a navegação `filtro=pendentes-analise`;
- preservadas as abas baseadas nos status reais `INFORMADO`, `EM_ANALISE`, `LIBERADO_PARA_ARMAZENAMENTO`, `ARMAZENADO_TEMPORARIAMENTE` e `DESPACHADO`;
- Dashboard preserva o KPI agregado de resíduos pendentes/em análise;
- nenhum DTO, migration ou regra de domínio foi alterado nesta subetapa.

Frontend:

```text
11f420c  feat: remover redundância da análise de resíduos
833c59d  feat: alinhar navegação do dashboard de resíduos
```

---

## 3.2 — Dados, classes, segurança e responsabilidade ✅

### 3.2.1 — Tratamento, estado físico e responsabilidade inicial

Implementado e validado:

- `EstadoFisicoResiduo` estruturado;
- tratamento realizado + descrição condicional;
- migration `V13__expand_basic_residuo_data.sql`;
- `gestorResponsavel` substituído por `gestorRecebedorInicial`;
- Gestor que recebe inicialmente é preservado como responsável inicial da conferência;
- armazenamento e despacho podem ser executados posteriormente por outro Gestor;
- histórico continua registrando o ator real de cada transição.

### 3.2.2 — Classes de Resíduo

Implementado e validado:

- catálogo `ClasseResiduo` por Unidade;
- código, descrição e estado ativo;
- classes iniciais A, B, F e H pela migration `V14__create_residue_classes.sql`;
- isolamento por tenant;
- múltiplas classes por Resíduo;
- classes informadas pelo Solicitante e classes confirmadas pela Gestão separadas;
- snapshot de código/descrição em `ResiduoClasse` para preservar histórico;
- CRUD/inativação disponível na Administração.

### 3.2.3 — Segurança/EPI + snapshot

Implementado e validado:

- medidas estruturadas: `LUVAS`, `OCULOS_PROTECAO`, `PROTECAO_RESPIRATORIA`, `JALECO_AVENTAL`, `OUTRO`;
- recomendações de segurança no Produto;
- snapshots independentes de segurança informada e confirmada no Resíduo;
- migration `V15__add_residue_safety_information.sql`;
- `OUTRO` exige observação;
- Produtos associados aos componentes podem sugerir EPI, mas não sobrescrevem a decisão do usuário;
- alterações futuras no Produto não modificam Resíduos históricos.

### 3.2.4 — Integração frontend

Implementado e validado:

- `processoOrigem` apresentado como **Procedência / uso do Resíduo**;
- estado físico;
- tratamento realizado;
- Classes de Resíduo;
- Segurança/EPI;
- sugestões de EPI vindas dos Produtos;
- confirmação independente pela Gestão;
- visão comparativa consolidada em dois cards:
  - Informado pelo laboratório;
  - Aprovado pela Gestão;
- card da Gestão mostra o Gestor que realmente liberou e a data/hora, obtidos do evento `RISCO_CONFERIDO_E_RESIDUO_LIBERADO`;
- formulário de informar Resíduo teve escala visual revisada para uso em 100% de zoom, com maior largura útil, tipografia e campos.

A validação também confirmou que armazenamento e despacho podem ser feitos por outro Gestor sem perder a rastreabilidade dos responsáveis anteriores.

---

## 3.3 — Identificação, prévia e permissão de impressão ✅

A identificação do Resíduo foi separada da autorização de impressão.

Regras finais:

```text
INFORMADO
→ Código SGL disponível
→ QR disponível
→ prévia disponível
→ impressão bloqueada

EM_ANALISE
→ prévia disponível
→ impressão bloqueada

LIBERADO_PARA_ARMAZENAMENTO
→ impressão liberada

ARMAZENADO_TEMPORARIAMENTE
→ impressão liberada

DESPACHADO
→ impressão liberada
```

Detalhes:

- Código SGL continua sendo gerado no registro inicial;
- QR passa a existir desde o registro inicial;
- Resíduos antigos sem QR recebem a identificação faltante ao abrir a prévia;
- a prévia usa dados informados enquanto a classificação ainda não foi confirmada;
- depois da análise, o rótulo prioriza os dados confirmados;
- botão de impressão permanece desabilitado antes da liberação;
- impressão via navegador também é bloqueada enquanto o Resíduo estiver apenas em prévia;
- texto da análise foi corrigido para deixar claro que a liberação **autoriza a impressão**, e não gera o código;
- template definitivo, Zebra e infraestrutura física permanecem na Etapa 10.

---

## Validação final da Etapa 3 — 17/09/2026

Validação manual considerada satisfatória pelo responsável do projeto.

Foram conferidos:

- criação de Resíduo com os novos campos;
- classes e EPI;
- análise e liberação;
- comparação entre declaração original e dados aprovados;
- identificação do Gestor que liberou;
- armazenamento por outro Gestor;
- despacho por outro Gestor;
- rastreabilidade/histórico atualizado;
- prévia do rótulo antes da liberação;
- bloqueio de impressão antes da liberação;
- liberação de impressão após análise;
- ajustes de escala e legibilidade do formulário.

**Etapa 3 encerrada.**

---

## Decisão para etapa futura — correções administrativas do ciclo

Durante a validação surgiu a necessidade de avaliar ações administrativas para corrigir o ciclo de um Resíduo sem apagar seu histórico.

Isso **não pertence à Etapa 3** e não deve ser resolvido como simples delete.

Levar para a Etapa 4 como refinamento operacional:

```text
ADMINISTRADOR
→ cancelar Resíduo com justificativa
→ ou retornar para análise/liberação quando a regra permitir
→ preservar histórico
→ registrar ator, data/hora e motivo
```

Pontos a definir antes de implementar:

- de quais status é permitido retornar;
- se `DESPACHADO` pode ou não ser revertido;
- status e semântica de `CANCELADO`, se adotado;
- efeito sobre permissão de impressão;
- necessidade de revalidação pela Gestão após retorno.

A **decisão geral sobre delete lógico** continua na Etapa 11. Cancelamento operacional de Resíduo e delete lógico são conceitos diferentes.

---

## Próximo passo exato

Iniciar **Etapa 4 — Expansão operacional de Resíduos** em branch própria, depois que a Etapa 3 estiver integrada à `main`.

Ordem prevista:

```text
4.1 locais de armazenamento cadastráveis
→ 4.2 modelos de Resíduos pré-cadastrados pela Gestão
→ 4.3 uso de modelo ou preenchimento manual pelo Solicitante
→ 4.4 correções administrativas do ciclo de vida (cancelar/retornar), após fechar regras
```

Handoff da próxima etapa:

`docs/CONTINUIDADE_ETAPA_4_2026-09-17.md`

Plano canônico:

`docs/PLANO_PRE_PRODUCAO.md`
