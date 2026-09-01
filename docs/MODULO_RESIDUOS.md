# Módulo de Resíduos Laboratoriais — SGL

**Branch atual:** `feat/residuos`  
**Base:** `main` atual  
**Migration:** `V11__create_residuo_module.sql`  
**Estado:** R0 — backend reconciliado; primeiro fluxo completo ponta a ponta validado; faltam validações negativas e mistura com Produto antes do frontend.

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
→ rotula
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

Transições fora de ordem devem ser rejeitadas.

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

`TipoRisco` passa a contemplar, além das classificações já existentes:

```text
IRRITANTE
PERIGO_SAUDE
OXIDANTE
EXPLOSIVO
GAS_PRESSURIZADO
PERIGO_AMBIENTAL
```

## 7. Gestão

Endpoints:

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
qrCodeConteudo = SGL-RESIDUO:<UUID público>
```

Endpoint:

```text
GET /api/v1/residuos/{id}/rotulo
```

O rótulo só pode ser obtido depois da liberação.

A etapa frontend deve produzir uma visualização imprimível com QR e dados essenciais do recipiente.

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

## 10. Consultas

```text
GET /api/v1/residuos
GET /api/v1/residuos/{id}
GET /api/v1/residuos/por-status?status=...
GET /api/v1/residuos/por-laboratorio?laboratorioId=...
GET /api/v1/residuos/por-gerador?usuarioGeradorId=...
```

`/por-gerador` é o contrato da frente **Meus resíduos**. Enquanto a autenticação definitiva não existe, o frontend envia o UUID do usuário da sessão DEV. Depois, a autorização real deverá limitar essa consulta à identidade autenticada ou às regras explicitamente permitidas.

## 11. Reconciliação com a main

O módulo experimental anterior estava em `feat/gestao-residuos`, muito atrás da `main`, e possuía uma migration chamada V5 que passou a conflitar com a evolução de Lotes.

Decisão:

```text
não mergear feat/gestao-residuos diretamente
não substituir arquivos atuais de Pedido/Produto/Lote
portar apenas código específico de Resíduos
usar V11 para a nova estrutura
```

Arquivos portados nesta R0:

```text
ResiduoController
ResiduoService
Residuo / ComponenteResiduo / HistoricoResiduo
request/response DTOs específicos
ResiduoRepository / HistoricoResiduoRepository
StatusResiduo
extensão de TipoRisco
V11__create_residuo_module.sql
```

Também foi adicionado o recorte por gerador necessário para `Meus resíduos`.

## 12. Validação R0 obrigatória

Status atual:

```text
mvn clean test                                      ✅ validado em 01/09/2026
subida do PostgreSQL com V1 → V11                  ✅ validado em 01/09/2026
Hibernate validate                                  ✅ validado em 01/09/2026
Swagger UI                                          ✅ validado em 01/09/2026
POST resíduo simples com componente livre           ✅ validado em 01/09/2026
status inicial INFORMADO                            ✅ validado em 01/09/2026
campos de Gestão/rótulo nulos antes do recebimento  ✅ validado em 01/09/2026
Meus resíduos por gerador                           ✅ validado em 01/09/2026
histórico inicial RESIDUO_INFORMADO                 ✅ validado em 01/09/2026
recebimento pela Gestão                             ✅ validado em 01/09/2026
status EM_ANALISE                                   ✅ validado em 01/09/2026
histórico RECEBIDO_PELA_GESTAO                      ✅ validado em 01/09/2026
análise/liberação                                   ✅ validado em 01/09/2026
código SGL de rastreio                              ✅ validado em 01/09/2026
QR lógico do resíduo                                ✅ validado em 01/09/2026
consulta dos dados do rótulo                        ✅ validado em 01/09/2026
armazenamento temporário                            ✅ validado em 01/09/2026
status ARMAZENADO_TEMPORARIAMENTE                   ✅ validado em 01/09/2026
preservação dos dados após armazenamento            ✅ validado em 01/09/2026
despacho para destino final                         ✅ validado em 01/09/2026
status DESPACHADO                                   ✅ validado em 01/09/2026
histórico completo do ciclo                         ✅ validado em 01/09/2026
```

Fluxo funcional ainda a validar:

```text
1. tentar transição fora de ordem
2. tentar ação de Gestão com perfil comum
3. informar mistura
4. componente ligado a Produto
5. conferir que Estoque/Lote não mudou
```

Somente após essa validação começar `feat/residuos-interface`.

## 13. Etapas seguintes do módulo

```text
R1 contrato final e consultas específicas
R2 Informar Resíduo — usuário comum
R3 Meus Resíduos — usuário comum
R4 Central Resíduos — Gestão
R5 análise/classificação
R6 rótulo + QR + impressão
R7 armazenamento temporário
R8 despacho/destinação
R9 histórico visual
R10 relatório de Resíduos
R11 PDF/XLSX
R12 validação integrada
```
