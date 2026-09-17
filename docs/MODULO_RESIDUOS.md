# Módulo de Resíduos Laboratoriais — SGL

**Estado em 17/09/2026:** ✅ fluxo atual refinado e validado até a Etapa 3 da pré-produção.  
**Migrations principais:** `V11__create_residuo_module.sql`, `V12__backfill_codigo_sgl_residuos.sql`, `V13__expand_basic_residuo_data.sql`, `V14__create_residue_classes.sql` e `V15__add_residue_safety_information.sql`.  
**Próxima evolução:** Etapa 4 — expansão operacional de Resíduos.

## 1. Regra central

```text
Produto != Resíduo
```

Produto representa catálogo/estoque. Resíduo representa material gerado no laboratório e encaminhado à Gestão.

Um componente pode referenciar opcionalmente um Produto para rastreabilidade e para sugestões de segurança, mas isso **não baixa, repõe ou altera EstoqueCentral, Lote ou MovimentacaoEstoque**.

---

## 2. Fluxo operacional

```text
PEDIDO
usuário solicita
→ Gestão atende
→ material sai do estoque
→ material chega ao laboratório

RESÍDUO
laboratório gera
→ usuário informa
→ recipiente chega à Gestão
→ Gestão recebe/confere
→ analisa/classifica
→ libera
→ armazena temporariamente
→ despacha/destina
```

O fluxo de status permanece:

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

A Etapa 4 adicionará locais de armazenamento e modelos reutilizáveis de Resíduo.

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

O sistema preserva separadamente:

```text
usuarioGerador
→ quem informou/gerou a ocorrência

gestorRecebedorInicial
→ Gestor que recebeu inicialmente o Resíduo
```

O Gestor recebedor inicial conduz a análise/liberação. Após a liberação, armazenamento e despacho podem ser executados por outro Gestor autorizado.

O histórico registra o ator real de cada transição, portanto trocar o executor nas etapas posteriores não apaga responsabilidades anteriores.

---

## 6. Risco declarado x confirmado

A declaração original permanece separada da classificação da Gestão.

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

Regra arquitetural:

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

Produto pode manter **recomendações** de segurança. Quando um Produto participa da composição, o frontend pode sugerir essas medidas ao Solicitante.

Isso não cria dependência histórica:

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

Na análise/liberação, a Gestão confirma:

- nível de risco;
- riscos;
- Classes de Resíduo;
- Segurança/EPI;
- local de armazenamento temporário;
- destino previsto;
- observação técnica;
- data prevista de despacho, quando informada.

Enquanto a autenticação definitiva não existe, contratos ainda podem receber identificadores do usuário responsável. A autenticação futura deve derivar identidade e tenant da sessão/token confiável.

---

## 10. Código SGL, QR, prévia e impressão

Código:

```text
SGL-RES-AAAA-NNNNNN
```

O Código SGL existe desde o registro inicial.

A Etapa 3 separou três conceitos:

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
→ código + QR existem
→ prévia disponível à Gestão
→ impressão bloqueada

LIBERADO_PARA_ARMAZENAMENTO ou posterior
→ impressão liberada
```

Endpoint:

```text
GET /api/v1/residuos/{id}/rotulo
```

A resposta informa, além dos dados do rótulo, se a impressão está autorizada.

Resíduos antigos sem QR podem receber a identificação técnica faltante ao abrir a prévia.

A tela de rótulo bloqueia tanto o botão quanto a impressão pelo navegador enquanto o Resíduo ainda estiver apenas em prévia.

A definição visual definitiva, templates finais e infraestrutura Zebra permanecem na **Etapa 10**.

---

## 11. Visualização comparativa da análise

A interface da Gestão consolida a conferência em dois blocos:

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

O Gestor que liberou é identificado pelo evento histórico `RISCO_CONFERIDO_E_RESIDUO_LIBERADO`, evitando inferência baseada no usuário atual ou em etapas posteriores.

---

## 12. Histórico

```text
GET /api/v1/residuos/{id}/historico
```

Cada transição registra usuário responsável, status resultante, ação, observação e data/hora.

A validação da Etapa 3 confirmou que armazenamento e despacho podem ser executados por Gestores diferentes e o histórico permanece correto.

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

Resumo:

```text
total
informados
emAnalise
liberados
armazenados
despachados
altoRisco
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

## 15. Validação da Etapa 3

Em 17/09/2026 foi considerada satisfatória a validação manual integrada da Etapa 3.

Foram verificados:

- criação com novos dados;
- estado físico e tratamento;
- Classes de Resíduo;
- EPI/segurança;
- análise/liberação;
- comparação informado x aprovado;
- identificação do Gestor que liberou;
- armazenamento e despacho por outro Gestor;
- histórico/rastreabilidade;
- prévia antecipada do rótulo;
- bloqueio/liberação de impressão;
- escala/legibilidade do formulário em 100% de zoom.

**Etapa 3 encerrada.**

---

## 16. Etapa 4 — próxima evolução

A próxima etapa expande o domínio sem reabrir o que foi validado.

### 4.1 Locais de armazenamento cadastráveis

Permitir local reutilizável + complemento livre, preservando opção manual.

### 4.2 Modelos de Resíduo

Criar definição reutilizável para padrões recorrentes.

```text
ModeloResiduo = definição/padrão
Residuo       = ocorrência real
```

Alterar o modelo depois não pode alterar ocorrências históricas.

### 4.3 Uso do modelo pelo Solicitante

Permitir escolha entre modelo pré-cadastrado e preenchimento manual.

### 4.4 Correções administrativas do ciclo

Avaliar ações administrativas específicas, com justificativa e histórico:

```text
cancelar Resíduo
retornar para análise/liberação
```

Antes de implementar, definir status permitidos, irreversibilidade de `DESPACHADO`, eventual `CANCELADO`, efeitos no rótulo e necessidade de nova liberação.

Isso não substitui a decisão geral de delete lógico, que permanece na **Etapa 11**.

Detalhes: `docs/CONTINUIDADE_ETAPA_4_2026-09-17.md`.

---

## 17. Refactor estrutural futuro

`Residuo.java` cresceu significativamente com as novas regras.

A revisão de tamanho, coesão e legibilidade foi deliberadamente movida para a **Etapa 13**, depois dos testes automatizados da Etapa 12.

O objetivo será refatorar sem alterar comportamento ou contratos e reexecutar a suíte de regressão após as mudanças.
