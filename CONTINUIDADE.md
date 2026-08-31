# Continuidade do Projeto SGL — Backend

**Projeto:** Sistema de Gestão de Laboratórios  
**Backend:** `gbsalermo/Sistema-SGL`  
**Frontend:** `gbsalermo/SGL-FRONTEND`  
**Última atualização:** 31/08/2026  
**Branch estável:** `main`  
**Fase atual:** backend operacional estável; frontend segue para Administração/Cadastros.  
**Próximo bloco funcional:** `Administração → Cadastros → Produtos` no frontend, com suporte backend já existente e fiscalização no cadastro.  
**Handoff completo:** `docs/DOSSIE_PROJETO_SGL.md`

Este arquivo é o checkpoint de retomada do backend. Para contratos HTTP, o Swagger/OpenAPI continua sendo a fonte viva. Para entender o projeto inteiro, ler o dossiê.

---

# 0. Regra de trabalho

```text
branch própria
→ implementação
→ validação
→ refinamento
→ Pull Request
→ main
→ atualizar CONTINUIDADE/documentação afetada
```

Não avançar uma etapa declarada como concluída sem necessidade concreta e não reorganizar o roadmap silenciosamente.

---

# 1. Estado geral em 31/08/2026

## Backend

```text
Spring Boot / PostgreSQL / Flyway                     ✅
Long interno + UUID público                           ✅
DTOs request/response                                 ✅
Tratamento global de erros                            ✅
Concorrência de aprovação                             ✅
FIFO / FEFO                                           ✅
Lotes / rastreabilidade                               ✅
Embalagens / multiplicador / fracionamento            ✅
Pedidos e urgência                                    ✅
Swagger / OpenAPI                                     ✅
Movimentações                                         ✅
Relatórios consolidados                               ✅
Produtos fiscalizados                                 ✅
Fiscalização / rastreabilidade controlada             ✅
Exportação PDF                                        ✅ validada
Exportação XLSX                                       ✅ validada
Resíduos                                              🟡 branch divergente; reconciliar antes de integrar
Autenticação / autorização / auditoria definitiva     ⏳ pós-frontend
Integração futura com autenticação corporativa        ⏳
Refactor técnico para inglês                          ⏳ pós-protótipo
```

## Frontend já integrado

```text
Login visual / sessão DEV                             ✅
Pedidos do solicitante                                ✅
Pedidos da gestão                                     ✅
Estoque e lotes                                       ✅
Movimentações                                         ✅
Central de Relatórios                                 ✅
PDF / XLSX por relatório                              ✅
Fiscalização                                          ✅
Página 404 animada                                    ✅
Administração / Cadastros                             ⏳ PRÓXIMA ETAPA
```

Importante: o login atual do frontend ainda é de desenvolvimento. A senha é exibida/exigida pela interface, mas não é validada por um endpoint de autenticação definitivo.

---

# 2. Arquitetura e identificadores

```text
Long id
→ chave interna, JPA, FKs, locks e consultas técnicas

UUID publicId
→ DTOs, endpoints e frontend
```

Fluxo padrão:

```text
Controller recebe UUID
→ Service resolve por publicId
→ domínio usa Long internamente quando necessário
```

Separação de responsabilidades:

```text
Controller = contrato HTTP
Service = orquestração/transação/regra de aplicação
Repository = persistência
Model = estado e regras diretamente ligadas à entidade
RequestDTO = entrada
ResponseDTO = saída
```

---

# 3. Estoque, lotes e pedidos — invariantes

```text
Produto = catálogo
EstoqueCentral = saldo consolidado por produto/unidade
Lote = validade + saldo + embalagem + rastreabilidade
MovimentacaoEstoque = trilha das operações físicas
```

Saída:

```text
perecível     → FEFO
não perecível → FIFO
```

Regras que não devem ser quebradas:

```text
EstoqueCentral.quantidadeAtual = soma operacional dos lotes
aprovação de pedido baixa estoque
entrega NÃO baixa estoque novamente
cancelamento de pedido aprovado restaura lotes exatos
lote vencido não participa de aprovação
movimentação registra o lote realmente utilizado
```

Formas de retirada:

```text
UNITARIO
KIT
CAIXA
GARRAFA
GALAO
```

Fracionamento:

```text
false → true  permitido
true  → false não permitido
```

Código/embalagem/multiplicador históricos não devem ser reescritos de forma a quebrar rastreabilidade.

---

# 4. Fluxo de Pedido

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

```text
aprovação → baixa estoque
entrega → registra conclusão, sem segunda baixa
cancelamento após aprovação → restaura os lotes exatos
urgência → atributo do pedido, sem alterar automaticamente FIFO/FEFO
```

`Pedido.dataEntrega` registra a entrega real. Pedidos antigos já entregues podem permanecer com `dataEntrega = null`; não inventar dados históricos.

---

# 5. Fiscalização de Produto

Campos:

```text
fiscalizado
orgaosFiscalizadores
observacaoFiscalizacao
```

Órgãos iniciais:

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
→ observação limpa

fiscalizado = true
→ ao menos um órgão obrigatório
```

Não inferir fiscalização por risco ou perecibilidade.

A classificação deve ser mantida no cadastro/edição de Produto e alimenta o relatório especializado de Fiscalização.

---

# 6. Flyway

A `main` possui migrations de `V1` a `V10`.

Resumo recente:

```text
V5  → apresentação/fracionamento do lote
V6  → observação do lote
V7  → Código SGL + sequência
V8  → tipo de embalagem do lote
V9  → forma de retirada no ItemPedido
V10 → data de entrega + fiscalização de Produto
```

Regra obrigatória: migrations já aplicadas são imutáveis.

A branch antiga de Resíduos contém uma migration `V5__create_residuo_module.sql`; essa numeração conflita com a sequência atual. Ao portar o módulo, criar uma nova migration compatível com a `main`, em vez de copiar a V5 antiga.

---

# 7. Relatórios — concluídos

Relatórios funcionais:

```text
1. Estagiários
2. Produtos
3. Movimentações
4. Resumo operacional
5. Estoque e lotes
6. Fiscalização
```

Resíduos permanece reservado e será ativado após integração do módulo.

Endpoints de consulta:

```text
GET /api/v1/relatorios/estagiarios
GET /api/v1/relatorios/produtos
GET /api/v1/relatorios/movimentacoes
GET /api/v1/relatorios/resumo-operacional
GET /api/v1/relatorios/estoque-lotes
GET /api/v1/relatorios/fiscalizacao
```

Decisão consolidada:

```text
Pedidos entregues não possui relatório próprio.
```

Quando necessário, consultar Movimentações com recorte de pedido, por exemplo:

```text
origem = PEDIDO
tipo = SAIDA
```

Detalhes: `docs/RELATORIOS.md`.

---

# 8. Exportação PDF/XLSX — concluída e integrada

Ciclo integrado em 28/08/2026:

```text
Backend  PR #9
Frontend PR #14
```

Regra:

```text
prévia JSON
PDF
XLSX
→ mesma consulta e mesmos filtros
```

Um arquivo representa um relatório por vez. Relatórios compostos podem usar múltiplas abas/seções.

Bibliotecas:

```text
Apache POI 5.5.1 → XLSX
OpenPDF 2.0.5     → PDF
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

Validação manual concluída para prévias, filtros, Fiscalização, PDF e XLSX.

Detalhes: `docs/EXPORTACAO_RELATORIOS.md`.

---

# 9. Resíduos — estado real

Decisão de domínio:

```text
Produto = catálogo/estoque
Resíduo = material gerado pelo laboratório
```

A composição do resíduo pode citar produtos/reagentes sem alterar automaticamente o estoque desses produtos.

Fluxo conceitual:

```text
laboratório gera
→ informa composição/uso/recipiente/riscos
→ gestor recebe/ficha
→ analisa e confirma riscos
→ rotula/libera
→ armazena temporariamente
→ despacha/destina
```

Existe implementação em:

```text
feat/gestao-residuos
```

Snapshot de 31/08/2026:

```text
2 commits próprios à frente
91 commits atrás da main
status: diverged
```

A branch contém controller, DTOs, entidades, repository, service, migration e documentação, mas **não está pronta para merge direto**.

Quando a etapa começar:

```text
main atual
→ revisar/portar os 2 commits exclusivos
→ adaptar migration à sequência atual
→ resolver conflitos de enum/contrato
→ testes + Swagger
→ frontend operacional
→ relatório Resíduos
→ PDF/XLSX Resíduos
```

---

# 10. Autenticação, autorização e auditoria

Decisão preservada:

```text
backend funcional + Swagger             ✅
frontend operacional principal          ✅ em grande parte
Administração/Resíduos/robustez          ⏳
→ autenticação/autorização/auditoria definitiva
→ integração corporativa futura
```

Não tratar as dependências Spring Security/OAuth como implementação concluída.

A Unidade institucional futuramente virá da integração corporativa; o frontend não terá CRUD manual de Unidade.

---

# 11. Próximos passos oficiais

Sem criar novo roadmap:

```text
1. Administração / Cadastros no frontend             ← PRÓXIMO
   ├── Produtos + fiscalização
   ├── Laboratórios
   ├── Projetos
   ├── Usuários
   └── Estagiários

2. reconciliar e integrar Resíduos
   ├── backend
   ├── frontend operacional
   └── relatório + exportação

3. Documentos/upload e rotulagem ainda pendentes
4. Dashboard final / alertas / robustez
   └── página 404 já concluída
5. autenticação + autorização + auditoria local definitiva
6. integração corporativa / sincronização de Unidade
7. refactor pós-protótipo para nomenclatura técnica em inglês
```

Tipos de unidade/embalagem não devem ser inventados como cadastro no frontend sem suporte de domínio backend.

---

# 12. Refactor pós-protótipo para inglês

Registrado em:

```text
docs/PENDENCIAS_POS_PROTOTIPO.md
```

É um refactor estrutural, não funcional. Deve ocorrer depois do fechamento do protótipo e preservar contratos externos sempre que possível.

Não misturar essa tradução com a implementação de Administração, Resíduos ou autenticação.

---

# 13. Validações já consolidadas

```text
PostgreSQL + Flyway
UUID público
entrada de lote
FIFO / FEFO
lote vencido fora da aprovação
estoque utilizável insuficiente
descarte por vencimento
cancelamento restaurando lotes exatos
entrega sem segunda baixa
histórico/rastreabilidade
consultas por projeto/laboratório/período
consistência EstoqueCentral = soma dos lotes
concorrência de aprovação
Swagger/OpenAPI
Movimentações
Relatórios
Fiscalização
PDF
XLSX
```

---

# 14. Documentação de referência

Começar por:

- `docs/DOSSIE_PROJETO_SGL.md`
- `docs/README.md`
- `README.md`
- `docs/RELATORIOS.md`
- `docs/EXPORTACAO_RELATORIOS.md`
- `docs/PENDENCIAS_POS_PROTOTIPO.md`

Para payloads/endpoints, confirmar sempre no Swagger antes de copiar exemplos históricos.

---

# 15. Regra de retomada

**O backend principal não precisa ser refeito. A próxima etapa do produto está em Administração/Cadastros no frontend; qualquer trabalho de backend agora deve responder a uma necessidade concreta desse bloco, à futura reconciliação de Resíduos ou às etapas posteriores já documentadas.**
