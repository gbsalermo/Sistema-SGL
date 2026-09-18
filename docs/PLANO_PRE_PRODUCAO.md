# Plano de Pré-Produção do SGL

**Projeto:** Sistema de Gestão de Laboratórios (SGL)  
**Data de consolidação:** 04/09/2026  
**Última atualização:** 18/09/2026  
**Status:** Etapas 1, 2 e 3 concluídas; Etapa 4 implementada e aguardando validação integrada; Etapa 5 é a próxima após homologação  
**Fase:** pré-produção pós-aprovação funcional

Este documento é a referência canônica do bloco de pré-produção. As etapas devem ser executadas em sequência, respeitando dependências de domínio, backend e frontend.

Roadmap formal posterior:

```text
pré-produção atual
→ matriz de permissões
→ congelamento funcional
→ homologação integrada final
→ autenticação/autorização/auditoria definitiva
→ integração corporativa
→ produção
```

---

# 1. Regra de execução

Fluxo recomendado:

```text
analisar main atual
→ confirmar escopo
→ fechar domínio/contratos
→ implementar
→ validar
→ refinar
→ documentar
→ integrar
→ iniciar próxima etapa
```

## Regra especial para backend

Alterações funcionais de backend são implementadas **manualmente pelo responsável do projeto**.

A IA deve:

- analisar código existente;
- propor modelagem/regras;
- indicar migrations, entidades, DTOs, repositories, services e controllers;
- fornecer código de referência quando solicitado;
- revisar a implementação feita pelo usuário;
- não aplicar backend funcional sem autorização explícita.

Frontend/documentação podem ser alterados diretamente quando autorizado.

---

# 2. Sequência oficial

## Etapa 1 — Padronização e refinamento visual global ✅

**Status:** concluída e validada.

Objetivo: eliminar inconsistências de cards, botões, selects, filtros, ícones, espaçamentos, estados visuais e semântica de cores.

---

## Etapa 2 — Dark Mode definitivo ✅

**Status:** concluída e validada em 11/09/2026.

Consolidado:

- `themeService.ts` como fonte única;
- persistência de tema;
- tokens Dark;
- Vuetify/DOM sincronizados;
- telas autenticadas cobertas;
- Login, 404 e rótulos de impressão preservados claros;
- CSS legado de tema removido.

Frontend: PR #50, squash merge `a3fff4fa8edb6b8900c4a5b359dbfc0245afb87c`.

---

## Etapa 3 — Refinamentos do fluxo atual de Resíduos ✅

**Status:** concluída e validada em 17/09/2026.

### 3.1 Redundância de análise ✅

- pseudo-filtro `PENDENTES_ANALISE` removido;
- navegação duplicada removida;
- status reais preservados.

### 3.2 Dados, classificação, segurança e responsabilidade ✅

#### 3.2.1 Estado físico, tratamento e responsabilidade

- `EstadoFisicoResiduo`;
- tratamento realizado + descrição condicional;
- `gestorRecebedorInicial`;
- armazenamento/despacho podem ser feitos por outro Gestor sem apagar responsabilidade inicial;
- migration V13.

#### 3.2.2 Classes de Resíduo

- catálogo editável por Unidade;
- códigos/descrições/ativo;
- classes A, B, F e H iniciais;
- classes informadas x confirmadas;
- snapshot histórico em `ResiduoClasse`;
- migration V14.

#### 3.2.3 Segurança/EPI

Medidas estruturadas:

```text
LUVAS
OCULOS_PROTECAO
PROTECAO_RESPIRATORIA
JALECO_AVENTAL
OUTRO
```

- recomendações no Produto;
- segurança informada x confirmada;
- snapshots no Resíduo;
- `OUTRO` exige descrição;
- migration V15.

#### 3.2.4 Frontend

- Procedência/uso usando `processoOrigem`;
- estado físico;
- tratamento;
- Classes de Resíduo;
- EPI;
- sugestões vindas dos Produtos;
- confirmação da Gestão;
- comparativo em dois cards: informado x aprovado;
- exibição do Gestor que liberou e data/hora pelo histórico;
- escala do formulário revisada para uso em 100% de zoom.

### 3.3 Identificação, prévia e impressão ✅

Regra final:

```text
INFORMADO / EM_ANALISE
→ código SGL + QR técnico existem
→ prévia disponível
→ impressão bloqueada

LIBERADO_PARA_ARMAZENAMENTO ou posterior
→ impressão permitida
```

Gerar/visualizar e permitir impressão são eventos distintos.

O QR técnico pertence à identificação/contrato. O template físico atual do frontend pode não renderizá-lo. Template definitivo, Zebra e infraestrutura física continuam na Etapa 10.

Validação confirmou criação, análise, armazenamento, despacho, uso de Gestores diferentes, histórico, prévia, bloqueio/liberação de impressão e legibilidade.

Detalhes: `docs/CONTINUIDADE_ETAPA_3_2026-09-11.md`.

---

## Etapa 4 — Expansão operacional de Resíduos 🔧 EM ANDAMENTO

**Impacto:** médio.

Handoff: `docs/CONTINUIDADE_ETAPA_4_2026-09-17.md`.

### 4.1 Locais de armazenamento cadastráveis ✅ CONCLUÍDA

A modelagem foi fechada antes da implementação.

```text
LocalArmazenamentoResiduo
= catálogo atual/editável por Unidade

Residuo.localArmazenamentoResiduo
= referência opcional ao catálogo

Residuo.complementoLocalArmazenamento
= complemento opcional da ocorrência

Residuo.localArmazenamentoTemporario
= snapshot textual histórico completo
```

Uso esperado:

```text
local cadastrado
+ complemento livre
```

Exemplo:

```text
Almoxarifado Químico
+ Prateleira B2
```

Também permitir texto manual quando necessário.

Regras fechadas:

- catálogo por Unidade;
- `ativo` para seleção/inativação sem apagar histórico;
- local inativo não aparece em novas seleções;
- alteração futura do nome do catálogo não altera o snapshot histórico;
- caminho por catálogo e caminho manual coexistem;
- modo catálogo aceita complemento opcional;
- modo manual usa o texto histórico existente;
- payload ambíguo com catálogo e manual ao mesmo tempo deve ser rejeitado;
- lookup deve validar a Unidade do Resíduo;
- análise/liberação define o local planejado;
- confirmação física pode manter ou corrigir o local;
- correção deve permanecer rastreável no histórico;
- rótulo/relatório continuam inicialmente usando `localArmazenamentoTemporario`;
- não fazer refactor amplo de `Residuo` durante esta etapa.

Plano em passos pequenos:

```text
4.1-A V16 + LocalArmazenamentoResiduo + repository ✅
4.1-B CRUD + tenant ✅
4.1-C integração com análise/liberação ✅
4.1-D confirmação física/correção estruturada ✅
4.1-E revisão e fechamento do backend ✅
4.1-F frontend Administração/Cadastros ✅
4.1-G frontend Gestão ✅
4.1-H regressão integrada e fechamento ✅
```

A 4.1 foi concluída e validada. As 4.2, 4.3 e 4.4 foram implementadas na branch da Etapa 4; a regressão final está documentada em `VALIDACAO_ETAPA_4.md`.

### 4.2 Modelos de Resíduos pré-cadastrados ✅ IMPLEMENTADA

Documento canônico da modelagem: `ETAPA_4_2_MODELO_RESIDUO.md`.

```text
ModeloResiduo = definição reutilizável/editável da Unidade
Residuo       = ocorrência operacional real e independente
```

Entregue:

- V17 e entidades `ModeloResiduo` / `ComponenteModeloResiduo`;
- CRUD por Unidade com inativação lógica;
- validação de nome, classes, Produtos, tratamento e segurança;
- Administração frontend para criar, editar, consultar e inativar modelos;
- testes backend das regras centrais.

Não foi criada FK de `Residuo` para `ModeloResiduo`. Alterar um modelo não modifica Resíduos históricos.

Subetapas:

```text
4.2-A contrato/modelagem                    ✅
4.2-B V17 + entidades + repositories        ✅
4.2-C CRUD + tenant + validações            ✅
4.2-D testes/revisão backend                ✅ testes adicionados
4.2-E Administração/Cadastros frontend      ✅
4.2-F fechamento de implementação           ✅
```

### 4.3 Uso pelo Solicitante ✅ IMPLEMENTADA

Ao informar um Resíduo:

```text
usar modelo pré-cadastrado
ou
preencher manualmente
```

O modelo preenche somente dados reutilizáveis. Quantidade, projeto e demais dados específicos continuam pertencendo à ocorrência. O usuário revisa os valores antes do envio, e o backend cria um `Residuo` normal, independente do modelo.

### 4.4 Correções administrativas do ciclo de vida ✅ IMPLEMENTADA

Foi adotado `CANCELADO` como status operacional para cancelamento administrativo, sem apagar o registro.

Ações exclusivas do Administrador no fluxo atual:

```text
CANCELAR
RETORNAR_ETAPA
```

Ambas exigem justificativa e geram histórico auditável.

Retorno de uma etapa:

```text
EM_ANALISE                   → INFORMADO
LIBERADO_PARA_ARMAZENAMENTO → EM_ANALISE
ARMAZENADO_TEMPORARIAMENTE  → LIBERADO_PARA_ARMAZENAMENTO
DESPACHADO                   → ARMAZENADO_TEMPORARIAMENTE
```

Regras:

- `INFORMADO` já é a primeira etapa e não retorna;
- `CANCELADO` não retorna;
- `DESPACHADO` não pode ser cancelado diretamente: precisa retornar para `ARMAZENADO_TEMPORARIAMENTE` primeiro;
- retorno preserva eventos históricos anteriores e limpa apenas dados que representam conclusão da etapa desfeita;
- nova execução da etapa gera novos eventos;
- cancelados são reconhecidos em filtros, dashboards e relatórios;
- isso não substitui a avaliação geral de delete lógico da Etapa 11.

**Validação integrada da Etapa 4:** `VALIDACAO_ETAPA_4.md`.

**Próxima etapa após validação:** Etapa 5 — Projetos e Atividades.

---

## Etapa 5 — Projetos e Atividades

**Impacto:** alto.

### 5.0 Portão de confirmação

Confirmar antes de alterar domínio:

- regra exata do Código SEG;
- se Atividade é entidade subordinada;
- se `SCI` é tipo de Projeto ou domínio distinto;
- lista oficial de situações de execução.

### 5.1 Projeto base

Relação:

```text
Laboratório 1 → N Projetos
```

Planejado:

- nome/descrição;
- Laboratório obrigatório;
- líder/responsável;
- início/fim;
- financiador;
- ciclo de vida;
- situação de execução;
- tipo, se confirmado;
- Código SEG;
- Código SGL/rastreabilidade interna.

Ciclo previsto:

```text
CRIADO
→ ATIVO
→ ENCERRADO_COM_AVALIACAO_PENDENTE
→ CONCLUIDO
```

### 5.2 Código SEG

Formato informado até o momento:

```text
AAAA.MM.DD.XX.XXX
```

Código SEG é institucional e não substitui Código SGL.

### 5.3 Atividades — condicional

Se confirmadas:

```text
Projeto 1 → N Atividades
```

### 5.4 Interface

Só fechar depois do domínio estabilizado.

---

## Etapa 6 — Estagiários e vínculos

**Dependência:** Etapa 5.

Confirmar antes de implementar:

- elegibilidade do Orientador;
- Orientador interno/externo;
- significado/cardinalidade de Cultura;
- Curso/Formação;
- múltiplas Atividades;
- regras de prorrogação.

Planejado:

- Orientador obrigatório;
- Projeto/Atividade;
- Bolsa/vínculo separado de Curso/Formação;
- Cultura/área temática;
- treinamento inicial de segurança;
- histórico de vínculos;
- prorrogações justificadas.

---

## Etapa 7 — Relatórios consolidados

**Dependência:** Etapas 5 e 6.

### 7.1 Filtros/dimensões

Laboratório, responsável, Projeto, Código SEG, líder, Atividade, Orientador, Bolsa, Curso, Cultura, situação e período.

### 7.2 Consultas/agregações

Contagens por Laboratório, Orientador, responsável, Bolsa, Curso, Cultura, Projeto e Atividade.

### 7.3 Prévia/telas

Visões consolidadas de Laboratórios, Projetos e Estagiários.

### 7.4 PDF/XLSX

Mesma consulta/filtros da tela.

### 7.5 Organização estrutural

Mover controllers/services/DTOs de Relatórios para packages específicos, sem alterar contratos ou comportamento.

---

## Etapa 8 — Unidades e Soluções

Ordem:

```text
normalização de unidades/apresentações
→ domínio de Soluções
→ composição/regras
→ interface/contrato estabilizados
```

### 8.1 Unidades

Separar:

```text
unidade de medida
≠
apresentação física
```

Conversões válidas:

```text
1 L = 1000 mL
1 kg = 1000 g
```

Não converter `g ↔ mL` sem densidade apropriada.

### 8.2 Soluções

Solução = receita/composição reutilizável de Produtos.

### 8.3 Fechamento do contrato

Estabilizar DTOs, composição, unidades, validações, edição/inativação, interface e histórico/snapshot antes da Etapa 9.

---

## Etapa 9 — Pedidos + Soluções

**Dependência:** Etapa 8.

Antes de alterar Pedido, confirmar se o fluxo atual será mantido ou se há mudanças adicionais solicitadas pelo cliente.

Pedido poderá conter Produto, Solução ou ambos, conforme escopo final.

Aprovação de Solução deve ser atômica: se qualquer componente for insuficiente, não aprovar parcialmente.

Preservar FIFO/FEFO, locks, lotes consumidos e regra de cancelamento/devolução.

---

## Etapa 10 — Rótulos e impressão operacional

Definir padrão-base SGL e templates adaptados de:

- Produto;
- Resíduo;
- Solução.

Também incluir Documento de Auditoria de Entrada de Lote, identificado como:

```text
DOCUMENTO INTERNO DO SGL
SEM VALOR FISCAL
```

Depois validar Zebra, dimensões, orientação, margens, driver/envio, ZPL e testes físicos.

---

## Etapa 11 — Manual do Usuário + avaliação de delete lógico

### 11.1 Manual

Disponibilizar materiais institucionais e de uso do SGL.

Definir contrato e armazenamento antes de criar upload/download permanente.

### 11.2 Delete lógico

Avaliar entidade por entidade no fim das alterações funcionais.

Não usar `ativo` indiscriminadamente em entidades que já possuem ciclo de vida próprio.

---

## Etapa 12 — Testes automatizados do Frontend

Stack:

```text
Vitest + Vue Test Utils
Cypress
```

Cobrir fluxos críticos finais, execução headless e scripts reproduzíveis.

---

## Etapa 13 — Revisão estrutural e legibilidade

Última etapa técnica.

Revisar especialmente:

- `Residuo`;
- Services grandes;
- DTOs extensos;
- Controllers concentrados;
- métodos longos;
- organização de packages;
- documentação de snapshots/invariantes;
- padronização tipográfica global do frontend conforme os tokens oficiais do SGL;
- remoção/consolidação de CSS legado que sobrescreve a escala tipográfica;
- revisão de textos operacionais abaixo de 12 px, preservando exceções justificadas de rótulos/impressão;
- adicionar checagem preventiva para evitar novos `font-size` fora do padrão sem justificativa.

Critério:

```text
sem mudança funcional intencional
+ contratos preservados
+ migrations preservadas
+ testes da Etapa 12 reexecutados
```

---

# 3. Dependências principais

```text
Etapa 1 → Etapa 2 → Etapa 3 → Etapa 4

Etapa 5 Projeto/Atividade
→ Etapa 6 Estagiários/vínculos
→ Etapa 7 Relatórios

Etapa 8 Unidades/Soluções
→ Etapa 9 Pedidos/Soluções

Produto + Resíduo + Solução estabilizados
→ Etapa 10 Rótulos

Etapas 1–11 estabilizadas
→ Etapa 12 testes
→ Etapa 13 refactor final
```

---

# 4. Estado de execução

```text
Limpeza/revisão documental                         ✅
Planejamento de pré-produção                        ✅
Etapa 1 — refinamento visual global                 ✅
Etapa 2 — Dark Mode                                 ✅
Etapa 3 — refinamentos de Resíduos                  ✅ concluída e validada
Etapa 4 — expansão operacional de Resíduos          🔧 em andamento
  4.1 — locais de armazenamento                     ✅ concluída
  4.2 — Modelos de Resíduo                          🔧 atual
Etapas 5–13                                         ⏳
```

A matriz de permissões não é a próxima tarefa enquanto este bloco estiver aberto.

---

# 5. Regra de continuidade

Ao encerrar cada etapa/subetapa, registrar:

```text
status
→ decisões
→ alterações
→ validações
→ pendências
→ próxima etapa
```

Nova necessidade deve ser posicionada neste roadmap antes da implementação.

Handoff atual: `docs/CONTINUIDADE_ETAPA_4_2026-09-17.md`.
