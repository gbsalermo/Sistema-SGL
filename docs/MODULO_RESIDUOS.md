# Módulo de Resíduos Laboratoriais — SGL

**Branch backend:** `feat/residuos`  
**Branch frontend:** `feat/residuos-interface`  
**Base de banco:** `V11__create_residuo_module.sql`  
**Estado em 01/09/2026:** fluxo operacional completo implementado; relatório e exportações adicionados; aguardando nova execução de testes após o fechamento do módulo.

## 1. Regra central

```text
Produto != Resíduo
```

Produto representa catálogo/estoque. Resíduo representa material gerado no laboratório e encaminhado à Gestão.

Um componente de resíduo pode referenciar opcionalmente um Produto para rastreabilidade, mas essa associação **não baixa, repõe ou altera EstoqueCentral, Lote ou MovimentacaoEstoque**.

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

## 3. Duas frentes de interface

### Usuário comum

```text
Informar resíduo
Meus resíduos
```

O usuário informa o que foi efetivamente gerado no laboratório e acompanha o ciclo. Ele não define a classificação técnica final nem executa as etapas operacionais da Gestão.

### Gestão

```text
Resíduos
├── a receber
├── em análise
├── liberados
├── armazenados
└── despachados
```

A Gestão recebe fisicamente, confere, confirma/corrige riscos, libera o rótulo, registra armazenamento temporário e despacho.

## 4. Fluxo de status

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

## 5. Criação pelo laboratório

`POST /api/v1/residuos`

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

Cada componente aceita:

```text
produtoId opcional
nomeComponente opcional se produtoId existir
principal
concentracaoOuQuantidade
observacao
```

É obrigatório identificar o componente por `produtoId` ou `nomeComponente`.

## 6. Risco declarado x confirmado

A informação original do laboratório nunca é sobrescrita silenciosamente.

```text
Laboratório
nivelRiscoInformado
riscosInformados[]

Gestão
nivelRiscoConfirmado
riscosConfirmados[]
```

Isso permite comparar declaração de origem e classificação técnica final.

`TipoRisco` contempla:

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

## 7. Operações da Gestão

```text
PUT /api/v1/residuos/{id}/receber
PUT /api/v1/residuos/{id}/analisar-liberar
PUT /api/v1/residuos/{id}/armazenar
PUT /api/v1/residuos/{id}/despachar
```

Enquanto a autenticação definitiva não existe, os DTOs recebem `usuarioGestorId`. O Service exige `GESTOR` ou `ADMINISTRADOR` para as transições de Gestão.

Depois da autenticação real, a identidade deverá vir da sessão/token sem alterar o domínio do Resíduo.

## 8. Rótulo e rastreabilidade

Depois da análise/liberação:

```text
codigoRastreio = SGL-RES-AAAA-NNNNNN
```

O backend atualmente também mantém `qrCodeConteudo` por compatibilidade técnica, porém o **QR Code foi retirado do rótulo visual do primeiro protótipo** por decisão de produto.

Endpoint:

```text
GET /api/v1/residuos/{id}/rotulo
```

O frontend usa o DTO consolidado para montar e imprimir o rótulo com:

```text
código SGL
pictogramas de periculosidade
classificação confirmada
composição
laboratório e gerador
processo de origem
recipiente
armazenamento/destino
quantidade
marca Embrapa
```

## 9. Histórico

Endpoint:

```text
GET /api/v1/residuos/{id}/historico
```

Cada transição registra:

```text
usuário responsável
status resultante
ação
observação
data/hora
```

O frontend exibe esses eventos como timeline no detalhe do resíduo.

## 10. Consultas operacionais

```text
GET /api/v1/residuos
GET /api/v1/residuos/{id}
GET /api/v1/residuos/por-status?status=...
GET /api/v1/residuos/por-laboratorio?laboratorioId=...
GET /api/v1/residuos/por-gerador?usuarioGeradorId=...
```

`/por-gerador` é o contrato da frente **Meus resíduos**. Enquanto a autenticação definitiva não existe, o frontend envia o UUID do usuário da sessão DEV. Depois, a autorização real deverá limitar essa consulta à identidade autenticada ou às regras explicitamente permitidas.

## 11. Relatório de Resíduos

Implementado na mesma arquitetura dos demais relatórios do SGL.

### Preview JSON

```text
GET /api/v1/relatorios/residuos
```

Filtros opcionais:

```text
status
laboratorioId
nivelRisco
dataInicio
dataFim
```

O retorno contém:

```text
geradoEm
total
informados
emAnalise
liberados
armazenados
despachados
altoRisco
itens[]
```

### Exportação

```text
GET /api/v1/relatorios/residuos/exportar?formato=PDF
GET /api/v1/relatorios/residuos/exportar?formato=XLSX
```

Os mesmos filtros do preview são aceitos pela exportação.

PDF:
- OpenPDF;
- A4 paisagem;
- marca SGL;
- resumo operacional;
- tabela de rastreabilidade.

XLSX:
- Apache POI;
- resumo;
- dados completos do ciclo;
- autofiltro;
- cabeçalho congelado;
- largura de colunas ajustada.

Arquivos adicionados:

```text
RelatorioResiduosResponseDTO
RelatorioResiduosService
RelatorioResiduosExportacaoService
RelatorioResiduosController
```

## 12. Reconciliação com a main

O módulo experimental anterior estava em `feat/gestao-residuos`, muito atrás da `main`, e possuía uma migration chamada V5 que passou a conflitar com a evolução de Lotes.

Decisão:

```text
não mergear feat/gestao-residuos diretamente
não substituir arquivos atuais de Pedido/Produto/Lote
portar apenas código específico de Resíduos
usar V11 para a nova estrutura
```

## 13. Validações já executadas antes do fechamento atual

```text
mvn clean test                                      ✅ 01/09/2026
subida PostgreSQL com V1 → V11                     ✅ 01/09/2026
Hibernate validate                                  ✅ 01/09/2026
Swagger UI                                          ✅ 01/09/2026
POST resíduo simples                                ✅
Meus resíduos                                       ✅ backend
histórico inicial                                   ✅ backend
recebimento                                         ✅
análise/liberação                                   ✅
código SGL                                          ✅
consulta DTO do rótulo                              ✅
armazenamento temporário                            ✅ backend
DESPACHADO + histórico completo                     ✅ backend
transição inválida rejeitada                        ✅
perfil comum bloqueado em ação de Gestão            ✅
mistura com Produto + componente livre              ✅ cadastro/composição
```

A conferência explícita de saldo/lotes antes/depois do Produto referenciado deve continuar na bateria integrada para comprovar documentalmente que não houve efeito colateral em estoque.

## 14. Implementação atual a validar novamente

Como relatório/exportações e frontend completo foram adicionados depois dos testes acima, executar novamente:

```text
backend: mvn clean test
backend: subida + Swagger
frontend: npm run build
frontend: fluxo completo no navegador
rótulo + print preview
armazenamento
despacho
histórico visual
relatório preview
PDF
XLSX
```

Nenhuma dessas validações novas deve ser marcada como concluída antes da execução local.

## 15. Etapas do módulo

```text
R1 contrato final e consultas específicas             ✅
R2 Informar Resíduo — usuário comum                   ✅ implementação
R3 Meus Resíduos — usuário comum                      ✅ implementação
R4 Central Resíduos — Gestão                          ✅ implementação
R5 análise/classificação                              ✅ implementação e validação visual
R6 rótulo + impressão (sem QR visual)                 ✅ implementação
R7 armazenamento temporário                           ✅ implementação
R8 despacho/destinação                                ✅ implementação
R9 histórico visual                                   ✅ implementação
R10 relatório de Resíduos                             ✅ implementação
R11 PDF/XLSX                                          ✅ implementação
R12 validação integrada                               ⏳ próxima validação
```
