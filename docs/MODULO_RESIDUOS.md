# Módulo de Resíduos Laboratoriais — SGL

**Estado em 03/09/2026:** ✅ módulo reconciliado, implementado e integrado à `main`.  
**Migrations:** `V11__create_residuo_module.sql` e `V12__backfill_codigo_sgl_residuos.sql`.  
**Branches `feat/gestao-residuos` / `feat/residuos`:** histórico de desenvolvimento; não representam trabalho pendente.

## 1. Regra central

```text
Produto != Resíduo
```

Produto representa catálogo/estoque. Resíduo representa material gerado no laboratório e encaminhado à Gestão.

Um componente pode referenciar opcionalmente um Produto para rastreabilidade, mas isso **não baixa, repõe ou altera EstoqueCentral, Lote ou MovimentacaoEstoque**.

---

## 2. Fluxo inverso a Pedidos

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
→ libera e rotula
→ armazena temporariamente
→ despacha/destina
```

---

## 3. Experiências de interface

### Solicitante

```text
/residuos/novo   → Informar resíduo
/meus-residuos   → acompanhar resíduos do usuário
```

O usuário informa o material efetivamente gerado. Ele não define a classificação técnica final nem executa as transições operacionais da Gestão.

### Gestão

```text
/residuos
```

A Gestão recebe, confere, confirma/corrige riscos, libera rótulo, registra armazenamento temporário e despacho.

---

## 4. Status

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

## 5. Criação

```text
POST /api/v1/residuos
```

Dados principais:

```text
usuarioGeradorId
laboratorioId
projetoId opcional
descricao
processoOrigem
recipiente
quantidade
unidadeMedida
nivelRiscoInformado
riscosInformados[]
observacaoGerador
componentes[]
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

## 6. Risco declarado x confirmado

A declaração original do laboratório permanece separada da classificação da Gestão.

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

## 7. Operações da Gestão

```text
PUT /api/v1/residuos/{id}/receber
PUT /api/v1/residuos/{id}/analisar-liberar
PUT /api/v1/residuos/{id}/armazenar
PUT /api/v1/residuos/{id}/despachar
```

Enquanto a autenticação definitiva não existe, contratos ainda podem receber identificadores de usuário responsável. O domínio exige perfil compatível para ações de Gestão.

A autenticação futura deve derivar a identidade da sessão/token sempre que possível.

---

## 8. Código SGL, rótulo e rastreabilidade

Código atual:

```text
SGL-RES-AAAA-NNNNNN
```

Desde 02/09/2026 ele é gerado **no registro inicial do Resíduo**, não apenas após análise/liberação. `V12` realizou backfill para registros anteriores.

Rótulo:

```text
GET /api/v1/residuos/{id}/rotulo
```

O primeiro protótipo imprime/exibe:

```text
código SGL
pictogramas de riscos confirmados
classificação confirmada
composição
laboratório e gerador
processo de origem
recipiente
armazenamento/destino
quantidade
marca Embrapa/SGL
```

O campo técnico de QR pode existir por compatibilidade, mas **QR Code não faz parte do rótulo visual atual**.

Rota frontend:

```text
/residuos/:id/rotulo
```

---

## 9. Histórico

```text
GET /api/v1/residuos/{id}/historico
```

Cada transição registra usuário responsável, status resultante, ação, observação e data/hora.

---

## 10. Consultas

```text
GET /api/v1/residuos
GET /api/v1/residuos/{id}
GET /api/v1/residuos/por-status?status=...
GET /api/v1/residuos/por-laboratorio?laboratorioId=...
GET /api/v1/residuos/por-gerador?usuarioGeradorId=...
```

`/por-gerador` sustenta **Meus resíduos**. A autorização real futura deverá limitar essa consulta conforme a identidade autenticada.

---

## 11. Relatório de Resíduos

Preview:

```text
GET /api/v1/relatorios/residuos
```

Filtros:

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

## 12. Reconciliação histórica

A antiga `feat/gestao-residuos` estava muito atrás da `main` e possuía migration incompatível com a evolução de Lotes.

A decisão correta foi aplicada:

```text
não mergear a branch antiga cegamente
portar apenas código específico do domínio
preservar Pedido/Produto/Lote atuais
criar V11 para o módulo
integrar sobre a main atual
```

Esse trabalho está concluído. Não voltar a tratar “reconciliar Resíduos” como etapa futura.

---

## 13. Validações já registradas

Antes do fechamento completo foram registrados testes positivos para:

```text
mvn clean test
PostgreSQL + Flyway até V11
Hibernate validate
Swagger
POST de Resíduo
Meus resíduos
recebimento
análise/liberação
Código SGL
rótulo
armazenamento
despacho + histórico
transição inválida rejeitada
perfil comum bloqueado em ação de Gestão
mistura com Produto + componente livre
```

Depois foram adicionados relatório/exportações, frontend completo e `V12`.

Portanto, a homologação geral do primeiro protótipo deve repetir o fluxo ponta a ponta e conferir explicitamente:

```text
V1 → V12 em banco limpo
Código SGL já no registro inicial
estoque antes/depois de componente ligado a Produto
rótulo e print preview
armazenamento
despacho
histórico visual
relatório
PDF/XLSX
```

---

## 14. Evolução planejada na pré-produção

O planejamento canônico permanece em `docs/PLANO_PRE_PRODUCAO.md`. Para Resíduos, as decisões já incorporadas ao roadmap são:

### Etapa 3 — refinamento do fluxo atual

- manter `processoOrigem` como informação de procedência/uso, sem criar campo redundante;
- adicionar indicação de tratamento realizado e descrição obrigatória quando houver tratamento;
- preservar quem informou/gerou e quem recebeu inicialmente pela Gestão;
- exigir que o Gestor recebedor conduza análise e liberação, permitindo outros Gestores nas etapas posteriores;
- adicionar classes de Resíduo pré-cadastradas, com seleção múltipla pelo Solicitante e confirmação pela Gestão;
- adicionar informações estruturadas de segurança/EPI;
- adicionar estado físico estruturado para uso operacional e no rótulo;
- permitir sugestão/herança de segurança a partir de Produtos relacionados, com edição limitada e confirmação;
- preservar snapshot das informações confirmadas no Resíduo;
- separar visualização antecipada do rótulo da permissão de impressão.

O conteúdo visual definitivo e a infraestrutura de impressão não pertencem mais à Etapa 3. Eles serão consolidados na **Etapa 8 — Rótulos e impressão operacional**, junto dos rótulos adaptados de Produto e Solução.

### Etapa 4 — modelos reutilizáveis

A área de Administração/Informar Resíduo poderá oferecer **Modelos de Resíduo** reutilizáveis.

O modelo poderá sugerir/preencher:

- descrição;
- processo de origem/procedência e uso;
- recipiente;
- riscos;
- classes;
- informações de segurança;
- composição/produtos;
- demais dados padrão aprovados.

Regras:

- quantidade e dados específicos da ocorrência continuam pertencendo ao Resíduo real;
- laboratório/projeto/gerador pertencem à ocorrência real;
- o modelo não movimenta estoque;
- riscos/classes/segurança do modelo não eliminam a conferência da Gestão;
- alterar o modelo posteriormente não modifica Resíduos históricos já criados.

Não confundir `ModeloResiduo` com `Produto`: Produtos podem participar da composição e fornecer referências/sugestões, mas não são substituídos pelo modelo.

### Etapa 8 — rótulo adaptado de Resíduo

Depois que Produto, Resíduo e Solução estiverem estabilizados, o rótulo de Resíduo será finalizado como um dos templates adaptados do padrão transversal do SGL.

A referência visual do cliente servirá como inspiração de acabamento. O template poderá destacar estado físico, palavra de advertência quando aplicável, responsáveis, rastreabilidade, riscos, classes, segurança e demais dados específicos do domínio, sem obrigar a cópia integral do modelo externo.

---



## 15. Estado final do módulo no primeiro protótipo

```text
R1 contrato/consultas                 ✅
R2 Informar Resíduo                   ✅
R3 Meus Resíduos                      ✅
R4 Gestão de Resíduos                 ✅
R5 análise/classificação              ✅
R6 rótulo/print sem QR visual         ✅
R7 armazenamento                      ✅
R8 despacho                           ✅
R9 histórico                          ✅
R10 relatório                         ✅
R11 PDF/XLSX                          ✅
R12 integração à main                 ✅
R13 homologação geral do protótipo    ⏳ junto ao congelamento
```
