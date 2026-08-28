# Continuidade do Projeto SGL

**Projeto:** Sistema de Gestão de Laboratórios  
**Backend:** `gbsalermo/Sistema-SGL`  
**Frontend:** `gbsalermo/SGL-FRONTEND`  
**Última atualização:** 28/08/2026  
**Branch de fechamento deste ciclo:** `feat/relatorios-exportacao`  
**Fase atual:** frontend de Gestão em evolução; backend estável e ampliado com Relatórios, Fiscalização e exportação PDF/XLSX.  
**Próximo grande bloco:** Administração/Cadastros e integração do módulo de Resíduos; autenticação/autorização/auditoria definitiva permanece pós-frontend.

Este arquivo é a referência de retomada do backend. O Swagger/OpenAPI continua sendo a fonte viva do contrato HTTP; este documento registra arquitetura, decisões de negócio, estado do projeto e próximos passos.

---

# 0. Regra de trabalho

Toda alteração relevante deve seguir:

```text
branch própria
→ implementação
→ validação
→ refinamento
→ Pull Request
→ main
→ atualizar CONTINUIDADE
```

Não avançar uma etapa declarada como concluída sem validação funcional.

---

# 1. Estado geral do sistema em 28/08/2026

## Backend

```text
Base Spring Boot / PostgreSQL / Flyway                 ✅
Long interno + UUID público                            ✅
DTOs request/response                                  ✅
Tratamento global de erros                             ✅
Concorrência de aprovação                              ✅
FIFO / FEFO                                            ✅
Lotes / rastreabilidade                                ✅
Embalagens / multiplicador / fracionamento             ✅
Pedidos e urgência                                     ✅
Swagger / OpenAPI                                      ✅
Movimentações                                          ✅
Relatórios consolidados                                ✅
Produtos fiscalizados                                  ✅
Fiscalização / rastreabilidade controlada              ✅
Exportação PDF                                         ✅ validada manualmente
Exportação XLSX                                        ✅ validada manualmente
Resíduos                                               🟡 desenvolvido em branch própria; integrar
Administração / novos cadastros                        ⏳
Autenticação / autorização / auditoria definitiva      ⏳ pós-frontend
Integração futura com autenticação corporativa         ⏳
```

## Frontend já suportado pelo backend

```text
Login atual / sessão de desenvolvimento                ✅
Pedidos do solicitante                                 ✅
Pedidos da gestão                                      ✅
Estoque e lotes                                        ✅
Movimentações                                          ✅
Central de Relatórios                                  ✅
PDF / Excel por relatório                              ✅
Fiscalização                                           ✅
```

---

# 2. Arquitetura consolidada

## Identificadores

```text
Long id
→ chave primária interna
→ relacionamentos JPA
→ foreign keys
→ locks
→ consultas técnicas
→ nunca atravessa a API

UUID publicId
→ identificador público
→ DTOs
→ endpoints
→ frontend
→ não sequencial
→ único e imutável
```

Fluxo padrão:

```text
Controller recebe UUID
→ Service resolve por findByPublicId(UUID)
→ domínio trabalha internamente com Long quando necessário
```

## Separação de responsabilidades

```text
Controller = contrato HTTP
Service = orquestração/transação
Repository = persistência
Model = regras diretamente ligadas ao estado da entidade
RequestDTO = entrada da API
ResponseDTO = saída da API
```

---

# 3. Regras centrais de estoque

```text
Produto = catálogo
EstoqueCentral = posição consolidada por produto/unidade
Lote = validade + quantidade disponível + rastreabilidade
MovimentacaoEstoque = trilha das operações físicas
```

Saída:

```text
produto perecível     → FEFO
produto não perecível → FIFO
```

Invariantes:

```text
EstoqueCentral.quantidadeAtual = soma operacional dos lotes
Pedido só baixa estoque na aprovação
Entrega não baixa estoque novamente
Cancelamento de pedido aprovado restaura os lotes exatos consumidos
Movimentação de saída registra o lote efetivamente utilizado
```

---

# 4. Embalagens e forma de retirada

O saldo interno permanece em unidades individuais.

Exemplo:

```text
2 kits de 50
→ saldo = 100 unidades
```

Forma solicitada no pedido:

```text
UNITARIO
KIT
CAIXA
GARRAFA
GALAO
```

Compatibilidade:

```text
UNITARIO
→ lote UNITARIO ou lote fracionável

KIT/CAIXA/GARRAFA/GALAO
→ mesmo tipo de embalagem
→ mesmo multiplicador
```

Fracionamento:

```text
false → true  ✅
true  → false ❌
```

O tipo original da embalagem, Código SGL e multiplicador histórico não devem ser reescritos de modo a quebrar rastreabilidade.

---

# 5. Fluxo de Pedido

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

Regras:

```text
aprovação → baixa estoque
entrega   → apenas registra conclusão; não baixa novamente
cancelamento aprovado → restaura lotes exatos
urgência → atributo do pedido, não altera automaticamente a regra de estoque
```

`Pedido.dataEntrega` foi adicionado em 28/08/2026 para registrar o momento real da entrega e permitir auditoria/consultas futuras.

Pedidos antigos já `ENTREGUE` podem permanecer com `dataEntrega = null`; não inventar timestamp histórico.

---

# 6. Fiscalização de Produto — concluída

Em 28/08/2026 o domínio de Produto passou a possuir classificação explícita para controle externo.

Campos:

```text
fiscalizado
orgaosFiscalizadores
observacaoFiscalizacao
```

Órgãos suportados inicialmente:

```text
POLICIA_FEDERAL
VIGILANCIA_SANITARIA
ANVISA
EXERCITO
OUTRO
```

Regra:

```text
fiscalizado = false
→ órgãos vazios
→ observação de fiscalização limpa

fiscalizado = true
→ ao menos um órgão obrigatório
```

**Não inferir fiscalização a partir de risco, perecibilidade ou tipo de risco.**

A classificação deve ser feita no cadastro/edição de Produto e é a fonte oficial do relatório de Fiscalização.

---

# 7. Migration V10 — Relatórios/Fiscalização

Arquivo:

```text
V10__add_dados_relatorios.sql
```

Adiciona:

```text
pedidos.data_entrega
produtos.fiscalizado
produtos.observacao_fiscalizacao
produto_orgaos_fiscalizadores
```

A tabela `produto_orgaos_fiscalizadores` preserva a relação de um Produto com um ou mais órgãos de controle.

Migrations anteriores permanecem imutáveis.

Resumo recente:

```text
V5 → apresentação/fracionamento do lote
V6 → observação do lote
V7 → Código SGL + sequência
V8 → tipo de embalagem do lote
V9 → forma de retirada no ItemPedido
V10 → data de entrega + fiscalização de Produto
```

---

# 8. Relatórios — concluído e validado

A central usa consultas específicas por relatório, evitando um endpoint genérico difícil de manter.

Relatórios funcionais:

```text
1. Estagiários
2. Produtos
3. Movimentações
4. Resumo operacional
5. Estoque e lotes
6. Fiscalização
```

`Resíduos` está previsto na central, mas depende da integração do módulo existente em `feat/gestao-residuos`.

## Estagiários

```text
GET /api/v1/relatorios/estagiarios
```

Filtros principais:

```text
ativo
laboratorioId
dataInicio
dataFim
```

Retorna total, ativos, inativos e dados do vínculo.

## Produtos

```text
GET /api/v1/relatorios/produtos
```

Filtros:

```text
ativo
fiscalizado
perecivel
risco
orgaoFiscalizador
```

Serve como visão geral do catálogo. O filtro `fiscalizado=true` não substitui o relatório de Fiscalização.

## Movimentações

```text
GET /api/v1/relatorios/movimentacoes
```

Filtros:

```text
tipo
origem
produtoId
laboratorioId
usuarioId
loteId
dataInicio
dataFim
```

Consolida entradas, saídas, devoluções, descartes e ajustes.

### Pedidos entregues

Foi decidido **não manter relatório separado de Pedidos entregues**.

Quando necessário, usar Movimentações com recorte como:

```text
origem = PEDIDO
tipo = SAIDA
```

`Pedido.dataEntrega` continua existindo como dado de domínio e auditoria.

## Resumo operacional

```text
GET /api/v1/relatorios/resumo-operacional
```

Apresenta:

```text
total de movimentações
entradas
saídas
descartes
produtos movimentados
lotes movimentados
ranking de entradas
ranking de saídas
lotes mais movimentados
```

## Estoque e lotes

```text
GET /api/v1/relatorios/estoque-lotes
```

Filtros principais:

```text
unidadeId
produtoId
ativoEstoque
abaixoMinimo
ativoLote
validade
diasVencimento
```

Classificações utilizadas:

```text
VALIDO
PROXIMO_VENCIMENTO
VENCIDO
SEM_VALIDADE
ESGOTADO
INATIVO
```

## Fiscalização

```text
GET /api/v1/relatorios/fiscalizacao
```

Filtros:

```text
produtoId
orgaoFiscalizador
unidadeId
dataInicio
dataFim
diasVencimento
```

Cruza:

```text
Produto fiscalizado
+ EstoqueCentral
+ Lote
+ MovimentacaoEstoque
+ Pedido
```

Permite rastrear:

```text
saldo atual
órgão(s) fiscalizador(es)
lotes ativos
lotes vencidos
próximos do vencimento
entradas
saídas
laboratório de destino
projeto
solicitante
pedido
responsável pela operação
saldo após movimentação
```

---

# 9. Exportação PDF/XLSX — concluída e validada

Branch de desenvolvimento:

```text
feat/relatorios-exportacao
```

Regra principal:

```text
prévia JSON
PDF
XLSX
→ usam a mesma consulta e os mesmos filtros
```

Não existe exportação em lote de vários relatórios ao mesmo tempo.

```text
1 seleção de relatório
→ 1 arquivo exportado
```

Para relatórios compostos, um único XLSX pode ter várias abas internas sem misturar módulos diferentes.

## Bibliotecas

```text
Apache POI 5.5.1 → XLSX
OpenPDF 2.0.5     → PDF compatível com Java 17
```

## PDF

Preparado para impressão:

```text
logo SGL no canto superior esquerdo
A4
orientação adequada ao conteúdo
paisagem para tabelas largas
margens compactas
quebra de texto
cabeçalhos de tabela repetidos
resumo e filtros no topo
paginação
```

## XLSX

Preparado para leitura e impressão:

```text
logo SGL no canto superior esquerdo
título + filtros
resumo
cabeçalho congelado
autofiltro
quebra de texto
largura de coluna limitada
A4
ajuste para uma página de largura
paisagem quando necessário
```

Relatórios compostos:

```text
Estoque e Lotes.xlsx
├── Posição de estoque
└── Lotes

Fiscalização.xlsx
├── Produtos controlados
└── Rastreabilidade
```

Endpoints:

```text
GET /api/v1/relatorios/estagiarios/exportar?formato=PDF|XLSX
GET /api/v1/relatorios/produtos/exportar?formato=PDF|XLSX
GET /api/v1/relatorios/movimentacoes/exportar?formato=PDF|XLSX
GET /api/v1/relatorios/resumo-operacional/exportar?formato=PDF|XLSX
GET /api/v1/relatorios/estoque-lotes/exportar?formato=PDF|XLSX
GET /api/v1/relatorios/fiscalizacao/exportar?formato=PDF|XLSX
```

Validação manual informada pelo usuário em 28/08/2026:

```text
prévia de relatórios                  ✅
filtros                               ✅
Fiscalização                          ✅
PDF                                   ✅
XLSX                                  ✅
layout/uso da exportação              ✅
```

---

# 10. Resíduos — estado atual

A modelagem de resíduos foi separada da lógica de Produto/Estoque.

Decisão de domínio:

```text
Produto = produto do catálogo/estoque
Resíduo = material gerado pelo laboratório
```

Um resíduo pode conter um ou vários produtos/reagentes, mas isso não altera automaticamente o estoque desses produtos.

Fluxo conceitual:

```text
laboratório gera resíduo
→ informa composição/uso/recipiente/riscos
→ gestor identifica e ficha
→ gestor confirma riscos e rotula
→ armazenamento temporário
→ despacho/destinação final
```

O módulo está sendo desenvolvido em:

```text
feat/gestao-residuos
```

Após integração, deve ser conectado também ao relatório `Resíduos` e às exportações.

---

# 11. Swagger/OpenAPI

Permanece concluído e é a referência operacional dos contratos HTTP.

```text
Swagger UI                    ✅
/v3/api-docs                  ✅
Controllers / Operations      ✅
DTO schemas                    ✅
Erros HTTP                     ✅
```

Toda nova API deve manter documentação Swagger coerente.

---

# 12. Autenticação, autorização e auditoria

Decisão preservada:

```text
backend estrutural              ✅
Swagger                         ✅
frontend principal              🟡 em andamento
→ autenticação/autorização/auditoria definitiva
→ integração corporativa futura
```

Não antecipar a autenticação corporativa enquanto os fluxos de frontend ainda estão sendo fechados.

---

# 13. Estado de validação histórica

Já foram validados em ciclos anteriores:

```text
PostgreSQL + Flyway
UUID público
entrada de lote
FEFO
FIFO
lote vencido fora da aprovação
estoque utilizável insuficiente
descarte por vencimento
cancelamento restaurando lotes exatos
entrega sem segunda baixa
Histórico de laboratório
consultas por projeto/laboratório/período
consistência EstoqueCentral = soma dos lotes
concorrência de aprovação
Swagger/OpenAPI
```

Em 28/08/2026 foram ainda validados manualmente:

```text
Central de Relatórios
Relatório de Estagiários
Relatório de Produtos
Relatório de Movimentações
Resumo Operacional
Estoque e Lotes
Fiscalização
Exportação PDF
Exportação XLSX
```

---

# 14. Próximos passos oficiais

Ordem recomendada sem criar novo roadmap:

```text
1. concluir merge da exportação de Relatórios                 ← fechamento deste ciclo
2. Administração / Cadastros
   ├── Produtos
   │   └── incluir fiscalização no formulário
   ├── Laboratórios
   ├── Projetos
   ├── Usuários
   └── Estagiários
3. integrar módulo de Resíduos
   └── conectar relatório/exportação de Resíduos
4. revisar Dashboard final / alertas / robustez / página 404
5. autenticação + autorização + auditoria local definitiva
6. preparar integração futura com autenticação corporativa
```

Ponto importante para Produtos em Administração:

```text
Fiscalizado?              toggle
Órgãos fiscalizadores    seleção múltipla
Observação fiscalização  opcional
```

Se `Fiscalizado = Sim`, ao menos um órgão deve ser informado.

---

# 15. Documentos de referência

- [`README.md`](README.md)
- [`docs/RELATORIOS.md`](docs/RELATORIOS.md)
- [`docs/EXPORTACAO_RELATORIOS.md`](docs/EXPORTACAO_RELATORIOS.md)
- [`docs/ENDPOINTS_INTERNOS.md`](docs/ENDPOINTS_INTERNOS.md)
- [`docs/JSON_EXEMPLOS.md`](docs/JSON_EXEMPLOS.md)
- [`docs/testes.md`](docs/testes.md)
- [`docs/FLUXO_DO_SISTEMA.md`](docs/FLUXO_DO_SISTEMA.md)
- [`docs/GUIA_ESTRUTURAL.md`](docs/GUIA_ESTRUTURAL.md)
- [`docs/CODIGOS_REFERENCIA_LOTE.md`](docs/CODIGOS_REFERENCIA_LOTE.md)
- [`docs/diagrama-uml-completo.puml`](docs/diagrama-uml-completo.puml)

---

# 16. Histórico recente

| Data | Decisão / validação |
|---|---|
| 20/08/2026 | Backend estrutural anterior encerrado; Swagger/OpenAPI validado |
| 28/08/2026 | Movimentações consolidadas como base de auditoria e relatórios |
| 28/08/2026 | Produto recebeu classificação explícita de fiscalização externa |
| 28/08/2026 | Criada V10 com `data_entrega` e dados de fiscalização de Produto |
| 28/08/2026 | Implementados relatórios de Estagiários, Produtos, Movimentações, Resumo Operacional, Estoque/Lotes e Fiscalização |
| 28/08/2026 | Decidido que Pedidos entregues é recorte de Movimentações, não relatório próprio |
| 28/08/2026 | Resíduos reservado como relatório próprio após integração do módulo correspondente |
| 28/08/2026 | Relatórios e Fiscalização integrados à `main` via PR #8 |
| 28/08/2026 | Exportação individual PDF/XLSX implementada com logo SGL e foco em impressão |
| 28/08/2026 | Exportações PDF e XLSX validadas manualmente pelo usuário |
