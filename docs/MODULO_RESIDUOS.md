# Módulo de Resíduos Laboratoriais — SGL

**Estado em 18/09/2026:** Etapa 3 ✅ concluída e validada; Etapa 4 🔧 em andamento; 4.1 ✅ concluída; 4.2 — ModeloResiduo é o próximo foco.  
**Migrations aplicadas:** `V11__create_residuo_module.sql`, `V12__backfill_codigo_sgl_residuos.sql`, `V13__expand_basic_residuo_data.sql`, `V14__create_residue_classes.sql`, `V15__add_residue_safety_information.sql` e `V16__create_residue_storage_locations.sql`.  
**Migration da 4.1:** V16 — locais de armazenamento de Resíduos ✅ aplicada e imutável.  
**Branch atual:** `feat/etapa-4-residuos`.

## 1. Regra central

```text
Produto != Resíduo
```

Produto representa catálogo/estoque. Resíduo representa uma ocorrência operacional real gerada no laboratório e encaminhada à Gestão.

Um componente pode referenciar opcionalmente um Produto para rastreabilidade e sugestões de segurança, mas isso **não baixa, repõe ou altera EstoqueCentral, Lote ou MovimentacaoEstoque**.

---

## 2. Fluxo operacional

```text
INFORMADO
   ↓ receber
EM_ANALISE
   ↓ analisar/liberar
LIBERADO_PARA_ARMAZENAMENTO
   ↓ armazenar
ARMAZENADO_TEMPORARIAMENTE
   ↓ despachar
DESPACHADO
```

Transições fora de ordem são rejeitadas.

---

## 3. Experiências de interface

### Solicitante

```text
/residuos/novo   → Informar resíduo
/meus-residuos   → acompanhar resíduos do usuário
```

O Solicitante informa a ocorrência real. A Gestão pode confirmar/corrigir classificação, classes e segurança sem apagar a declaração original.

### Gestão

```text
/residuos
```

A Gestão recebe, confere, analisa/classifica, libera, consulta rótulo, registra armazenamento temporário e despacho.

### Administração

A área de Cadastros já permite manter **Classes de Resíduo** e recomendações de segurança em Produtos.

A Etapa 4.1 adicionará **locais de armazenamento**. A Etapa 4.2 adicionará **Modelos de Resíduo** reutilizáveis.

---

## 4. Criação do Resíduo

```text
POST /api/v1/residuos
```

Dados principais consolidados:

```text
usuarioGeradorId
laboratorioId
projetoId opcional
descricao
processoOrigem
estadoFisico
tratamentoRealizado
descricaoTratamento condicional
recipiente
quantidade
unidadeMedida
nivelRiscoInformado
riscosInformados[]
classesInformadasIds[]
medidasSegurancaInformadas[]
observacaoSegurancaInformada
observacaoGerador
componentes[]
```

Na interface, `processoOrigem` é apresentado como **Procedência / uso do Resíduo**. Não existe campo redundante de procedência.

Tratamento:

```text
tratamentoRealizado = false
→ descricaoTratamento não é obrigatória

tratamentoRealizado = true
→ descricaoTratamento obrigatória
```

Estado físico atual:

```text
LIQUIDO
SOLIDO
SEMISSOLIDO
GASOSO
OUTRO
```

Componente:

```text
produtoId opcional
nomeComponente opcional se produtoId existir
principal
concentracaoOuQuantidade
observacao
```

É obrigatório identificar o componente por `produtoId` ou `nomeComponente`.

---

## 5. Responsabilidade operacional

```text
usuarioGerador
→ quem informou/gerou a ocorrência

gestorRecebedorInicial
→ Gestor que recebeu inicialmente o Resíduo

HistoricoResiduo
→ ator real de cada transição
```

O Gestor recebedor inicial conduz a análise/liberação. Após a liberação, armazenamento e despacho podem ser executados por outro Gestor autorizado sem apagar responsabilidades anteriores.

---

## 6. Risco declarado x confirmado

```text
Laboratório
nivelRiscoInformado
riscosInformados[]

Gestão
nivelRiscoConfirmado
riscosConfirmados[]
```

`TipoRisco` atual:

```text
NENHUM
INFLAMAVEL
RADIOATIVO
TOXICO
CORROSIVO
BIOLOGICO
IRRITANTE
PERIGO_SAUDE
OXIDANTE
EXPLOSIVO
GAS_PRESSURIZADO
PERIGO_AMBIENTAL
```

---

## 7. Classes de Resíduo e snapshot

Classes são catálogo editável por Unidade, não enum rígido.

Catálogo inicial:

```text
A — Solventes ou soluções de substâncias orgânicas que não contenham halogênios
B — Solventes ou soluções orgânicas que contenham halogênios
F — Resíduos sólidos de produtos químicos orgânicos
H — Outros
```

Fluxo:

```text
Solicitante
→ classes informadas

Gestão
→ confirma/adiciona/remove
→ classes confirmadas
```

`ResiduoClasse` mantém snapshot de código e descrição.

```text
ClasseResiduo
= catálogo atual/editável

ResiduoClasse
= fotografia histórica da classificação usada naquela ocorrência
```

Renomear ou inativar uma classe não altera a classificação de Resíduos antigos.

---

## 8. Segurança/EPI e snapshot

Medidas estruturadas atuais:

```text
LUVAS
OCULOS_PROTECAO
PROTECAO_RESPIRATORIA
JALECO_AVENTAL
OUTRO
```

Quando `OUTRO` é utilizado, uma observação descritiva é obrigatória.

Produto pode manter recomendações de segurança. Quando um Produto participa da composição, o frontend pode sugerir essas medidas ao Solicitante.

```text
Produto
→ recomendação atual

Residuo.medidasSegurancaInformadas
→ snapshot do que foi registrado pelo Solicitante

Residuo.medidasSegurancaConfirmadas
→ snapshot do que foi confirmado pela Gestão
```

Alterar as recomendações do Produto depois não altera Resíduos históricos.

---

## 9. Operações da Gestão

```text
PUT /api/v1/residuos/{id}/receber
PUT /api/v1/residuos/{id}/analisar-liberar
PUT /api/v1/residuos/{id}/armazenar
PUT /api/v1/residuos/{id}/despachar
```

Na análise/liberação, a Gestão confirma risco, Classes, Segurança/EPI, armazenamento temporário, destino previsto, observação técnica e data prevista quando informada.

Enquanto a autenticação definitiva não existe, contratos ainda podem receber identificadores do usuário responsável. A autenticação futura deve derivar identidade e tenant da sessão/token confiável.

---

## 10. Código SGL, QR, prévia e impressão

Código:

```text
SGL-RES-AAAA-NNNNNN
```

O Código SGL e o QR técnico existem desde o registro inicial.

```text
identificação
≠
pré-visualização
≠
autorização para impressão
```

Regra final:

```text
INFORMADO / EM_ANALISE
→ código + QR técnico existem
→ prévia disponível à Gestão
→ impressão bloqueada

LIBERADO_PARA_ARMAZENAMENTO ou posterior
→ impressão liberada
```

Endpoint:

```text
GET /api/v1/residuos/{id}/rotulo
```

O contrato pode transportar `qrCodeConteudo`. O template físico atual do frontend não precisa renderizar o QR; definição visual final e infraestrutura Zebra permanecem na **Etapa 10**.

---

## 11. Visualização comparativa da análise

```text
Informado pelo laboratório
→ classes
→ risco
→ segurança/EPI
→ observação do gerador

Aprovado pela Gestão
→ classes confirmadas
→ risco confirmado
→ segurança/EPI confirmada
→ observação técnica
→ Gestor que liberou
→ data/hora da liberação
```

O Gestor que liberou é identificado pelo evento histórico `RISCO_CONFERIDO_E_RESIDUO_LIBERADO`.

---

## 12. Histórico

```text
GET /api/v1/residuos/{id}/historico
```

Cada transição registra usuário responsável, status resultante, ação, observação e data/hora.

A validação da Etapa 3 confirmou armazenamento e despacho por Gestores diferentes sem perda de rastreabilidade.

---

## 13. Consultas

```text
GET /api/v1/residuos
GET /api/v1/residuos/{id}
GET /api/v1/residuos/por-status?status=...
GET /api/v1/residuos/por-laboratorio?laboratorioId=...
GET /api/v1/residuos/por-gerador?usuarioGeradorId=...
```

`/por-gerador` sustenta **Meus resíduos**. A autorização real futura deverá limitar essa consulta conforme a identidade autenticada.

---

## 14. Relatório de Resíduos

Preview:

```text
GET /api/v1/relatorios/residuos
```

Filtros atuais:

```text
status
laboratorioId
nivelRisco
dataInicio
dataFim
```

Exportação:

```text
GET /api/v1/relatorios/residuos/exportar?formato=PDF
GET /api/v1/relatorios/residuos/exportar?formato=XLSX
```

Frontend:

```text
/relatorios/residuos
```

---

## 15. Etapa 3 — encerrada

Validada em 17/09/2026 com criação, análise/liberação, armazenamento, despacho, Gestores diferentes, histórico, Classes, EPI, comparação informado/aprovado, prévia antecipada, bloqueio/liberação de impressão e revisão de escala visual.

---

## 16. Etapa 4.1 — locais de armazenamento ✅

A modelagem já foi aprovada.

```text
LocalArmazenamentoResiduo
= catálogo mutável por Unidade

Residuo.localArmazenamentoResiduo
= referência opcional ao catálogo

Residuo.complementoLocalArmazenamento
= complemento opcional da ocorrência

Residuo.localArmazenamentoTemporario
= snapshot textual histórico completo
```

Exemplos:

```text
catálogo: Almoxarifado Químico
complemento: Prateleira B2
snapshot: Almoxarifado Químico - Prateleira B2
```

Ou caminho manual:

```text
localArmazenamentoResiduo = null
complementoLocalArmazenamento = null
localArmazenamentoTemporario = texto manual
```

Regras:

- catálogo pertence à Unidade;
- cadastro pode ser ativado/inativado;
- local inativo sai das novas seleções sem invalidar ocorrências antigas;
- renomear o catálogo não altera snapshots antigos;
- catálogo + complemento e caminho manual são modos alternativos;
- payload ambíguo deve ser rejeitado;
- lookup deve restringir pelo tenant/Unidade do Resíduo;
- análise define armazenamento planejado;
- confirmação física pode manter ou corrigir;
- correções precisam permanecer rastreáveis;
- rótulo/relatório continuam usando o snapshot textual existente.

### Implementação 4.1-A–E — backend concluído ✅

Implementado no backend:

```text
V16__create_residue_storage_locations.sql
LocalArmazenamentoResiduo.java
LocalArmazenamentoResiduoRepository.java
CRUD + tenant
integração com análise/liberação
confirmação física/correção
histórico da correção
```

Estrutura aplicada pela V16:

```text
locais_armazenamento_residuo
→ id
→ public_id
→ unidade_id
→ nome
→ ativo

residuos
→ local_armazenamento_residuo_id nullable
→ complemento_local_armazenamento nullable
```

Backend da 4.1 revisado e fechado em 18/09/2026.

Sequência atual:

```text
4.1-B CRUD + tenant ✅
4.1-C análise/liberação ✅
4.1-D confirmação física/correção ✅
4.1-E revisão backend ✅
4.1-F frontend Cadastros ✅
4.1-G frontend Gestão ✅
4.1-H regressão/fechamento ✅
```

---

## 17. Etapas 4.2–4.4 — sequência atual

### 4.2 Modelos de Resíduo

```text
ModeloResiduo = definição/padrão
Residuo       = ocorrência real
```

Alterar o modelo depois não pode alterar ocorrências históricas.

### 4.3 Uso do modelo pelo Solicitante

Permitir escolha entre modelo pré-cadastrado e preenchimento manual.

### 4.4 Correções administrativas

Avaliar cancelamento/retorno para análise com justificativa, ator, data e histórico. Regras de status, `DESPACHADO`, eventual `CANCELADO`, dados confirmados e rótulo devem ser fechadas antes de codar.

Isso não substitui a decisão geral de delete lógico da Etapa 11.

---

## 18. Refactor estrutural futuro

`Residuo.java` cresceu significativamente. A revisão de tamanho, coesão e legibilidade foi deliberadamente movida para a **Etapa 13**, depois dos testes automatizados da Etapa 12.
