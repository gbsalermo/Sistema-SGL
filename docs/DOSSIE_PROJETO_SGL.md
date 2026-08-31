# Dossiê do Projeto SGL — Handoff para IA

**Projeto:** SGL — Sistema de Gestão de Laboratórios  
**Backend:** `gbsalermo/Sistema-SGL`  
**Frontend:** `gbsalermo/SGL-FRONTEND`  
**Snapshot:** 31/08/2026  
**Objetivo deste arquivo:** permitir que outra pessoa ou IA entenda rapidamente o projeto, o estado real, as decisões já tomadas, o que não deve ser reaberto sem motivo e qual é a próxima etapa.

---

# 1. Como ler o projeto

Quando houver conflito entre fontes, usar esta ordem de precedência:

```text
1. código da branch main
2. Swagger/OpenAPI do backend para contratos HTTP
3. CONTINUIDADE.md do repositório em que se está trabalhando
4. este DOSSIE_PROJETO_SGL.md
5. documentos específicos de decisão/módulo
6. documentos de etapas antigas, roteiros de teste e exemplos históricos
```

Documentos antigos não devem ser apagados apenas por estarem defasados: vários registram decisões e evolução do projeto. Porém, textos como “próxima etapa” dentro de documentos de fases antigas não representam necessariamente o planejamento atual.

---

# 2. Objetivo do SGL

O SGL é um sistema corporativo para operação de laboratórios, com foco em:

- solicitação e gestão de pedidos de materiais;
- estoque central por unidade;
- rastreabilidade física por lote;
- FIFO/FEFO;
- embalagens, multiplicadores e fracionamento;
- movimentações auditáveis;
- fiscalização de produtos controlados;
- relatórios operacionais e gerenciais;
- exportação oficial PDF/XLSX;
- gestão futura de resíduos;
- cadastros administrativos;
- futura autenticação/autorização/auditoria integrada ao ambiente corporativo.

O sistema é dividido em dois repositórios independentes, mas coordenados:

```text
gbsalermo/Sistema-SGL
→ API, domínio, persistência, regras de negócio, Swagger, relatórios e exportações

gbsalermo/SGL-FRONTEND
→ SPA Vue, experiência do solicitante, Gestão e Administração
```

---

# 3. Stack atual

## Backend

```text
Java 17
Spring Boot 4.1.0
Spring Data JPA / Hibernate
PostgreSQL
Flyway
Spring Validation
Spring Security / OAuth2 Client como dependências de base
SpringDoc OpenAPI / Swagger
JUnit / H2 em testes
Apache POI 5.5.1 para XLSX
OpenPDF 2.0.5 para PDF
Maven
```

Importante: a presença de Spring Security/OAuth no projeto **não significa que a autenticação corporativa definitiva esteja concluída**. Essa etapa continua planejada para depois do fechamento funcional do frontend.

## Frontend

```text
Vue 3.5
Vite 8
TypeScript 5.9
Vue Router 5
Pinia 4
Axios
Vuetify 3
Node >= 20.19
```

---

# 4. Estado executivo em 31/08/2026

```text
BACKEND
Fundação Spring/PostgreSQL/Flyway                 ✅
UUID público + Long interno                       ✅
Pedidos / urgência                                ✅
Estoque / lotes                                   ✅
FIFO / FEFO                                       ✅
Embalagem / multiplicador / fracionamento         ✅
Movimentações / rastreabilidade                   ✅
Swagger/OpenAPI                                   ✅
Relatórios                                        ✅
Fiscalização de produtos                          ✅
Exportação PDF/XLSX                               ✅
Resíduos                                          🟡 implementado em branch antiga; precisa reconciliar com main
Autenticação/autorização/auditoria definitiva     ⏳ pós-frontend

FRONTEND
Login visual + sessão temporária DEV              ✅
Pedidos do solicitante                            ✅
Pedidos da gestão                                 ✅
Estoque / lotes                                   ✅
Movimentações                                     ✅
Relatórios / fiscalização                         ✅
Exportação PDF/XLSX                               ✅
Página 404 animada                                ✅
Administração / Cadastros                         ⏳ PRÓXIMA ETAPA
Resíduos operacional                              ⏳ depende da reconciliação do backend e integração frontend
Documentos/upload                                 ⏳
Dashboard final / alertas / robustez              ⏳
Autenticação/autorização/auditoria definitiva     ⏳
Padronização técnica do código para inglês        ⏳ pós-protótipo
```

---

# 5. Regra de identificadores

Decisão consolidada:

```text
Long id
→ interno ao backend e banco
→ JPA, FKs, locks e detalhes técnicos

UUID publicId
→ fronteira pública
→ endpoints
→ DTOs
→ frontend
```

Não reintroduzir IDs numéricos em novos contratos públicos apenas porque migrations, entidades ou exemplos históricos ainda os exibem internamente.

Fluxo esperado:

```text
Controller recebe UUID
→ Service resolve por publicId
→ domínio usa Long internamente quando necessário
```

---

# 6. Regras centrais de estoque

Conceitos:

```text
Produto
→ catálogo e unidade-base

EstoqueCentral
→ saldo consolidado do produto em uma unidade

Lote
→ quantidade física, validade, embalagem, multiplicador e rastreabilidade

MovimentacaoEstoque
→ trilha das alterações físicas de saldo
```

Seleção para saída:

```text
produto perecível     → FEFO
produto não perecível → FIFO
```

Invariantes importantes:

```text
EstoqueCentral.quantidadeAtual deve refletir a soma operacional dos lotes
Pedido só baixa estoque na aprovação
Entrega não realiza segunda baixa
Cancelamento de pedido aprovado restaura os lotes exatos consumidos
Lote vencido não é usado na aprovação
Movimentação registra o lote realmente afetado
```

---

# 7. Embalagem e fracionamento

O saldo interno é mantido em unidade-base/individual.

Exemplo:

```text
2 kits × 50 reações = 100 reações no saldo
```

Formas de retirada atuais:

```text
UNITARIO
KIT
CAIXA
GARRAFA
GALAO
```

Compatibilidade resumida:

```text
UNITARIO
→ lote UNITARIO ou lote fracionável

KIT/CAIXA/GARRAFA/GALAO
→ embalagem compatível
→ multiplicador compatível
```

Fracionamento é irreversível no sentido funcional já aprovado:

```text
false → true  permitido
true  → false não permitido
```

Não reescrever dados históricos de apresentação, multiplicador ou Código SGL de lote de modo a quebrar rastreabilidade.

---

# 8. Pedidos

Fluxo:

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

Regras já consolidadas:

- aprovação executa a baixa física;
- entrega registra a conclusão sem nova baixa;
- cancelamento após aprovação restaura os lotes consumidos;
- urgência é atributo do pedido e não altera automaticamente FIFO/FEFO;
- `Pedido.dataEntrega` registra o evento real de entrega;
- pedidos históricos entregues antes da inclusão do campo podem permanecer com `dataEntrega = null`.

---

# 9. Fiscalização de produtos

Fiscalização é uma classificação explícita do cadastro de Produto.

Campos de domínio:

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
→ observação de fiscalização limpa

fiscalizado = true
→ pelo menos um órgão fiscalizador obrigatório
```

**Não inferir fiscalização por risco, tipo de risco ou perecibilidade.**

A próxima tela de `Administração → Cadastros → Produtos` deve permitir configurar essa classificação na criação e edição.

---

# 10. Relatórios e exportações

Relatórios funcionais na `main`:

```text
1. Estagiários
2. Produtos
3. Movimentações
4. Resumo operacional
5. Estoque e lotes
6. Fiscalização
```

Resíduos está reservado como sétimo relatório, mas depende da integração do módulo de resíduos.

Decisão importante:

```text
Pedidos entregues NÃO possui relatório próprio.
```

Quando necessário, usar Movimentações com recorte semelhante a:

```text
origem = PEDIDO
tipo = SAIDA
```

Exportação oficial:

```text
prévia JSON
PDF
XLSX
→ mesma consulta e mesmos filtros
```

Cada arquivo representa um único tipo de relatório. Relatórios compostos podem ter várias seções/abas internas.

Ciclo encerrado e integrado em 28/08/2026:

```text
Backend  PR #9 → exportação PDF/XLSX
Frontend PR #14 → interface de exportação
```

---

# 11. Resíduos — decisão de domínio e alerta técnico

Decisão:

```text
Produto ≠ Resíduo
```

Produto representa catálogo/estoque. Resíduo representa material gerado pelo laboratório.

Um resíduo pode conter um ou vários produtos/reagentes, porém essa composição **não altera automaticamente o estoque** dos produtos citados.

Fluxo conceitual:

```text
laboratório gera
→ informa composição, uso, recipiente e riscos
→ gestor recebe/ficha
→ analisa/confirma riscos
→ rotula/libera
→ armazenamento temporário
→ despacho/destinação final
```

Existe implementação de backend em:

```text
feat/gestao-residuos
```

**Atenção:** em 31/08/2026 essa branch está divergente da `main`: possui 2 commits próprios e está 91 commits atrás. Ela contém controller, DTOs, entidades, repository, service, migration e documentação de resíduos, mas **não deve ser mergeada diretamente sem reconciliação**.

Procedimento correto quando chegar ao módulo:

```text
1. partir da main atual
2. revisar os 2 commits exclusivos da branch
3. portar/reaplicar a modelagem para a sequência atual de migrations
4. resolver conflitos de enums e contratos
5. compilar e testar o backend
6. atualizar Swagger
7. integrar frontend operacional
8. implementar relatório de Resíduos
9. implementar exportação PDF/XLSX de Resíduos
```

A migration antiga da branch usa numeração incompatível com a sequência atual da `main`; não copiar seu número cegamente.

---

# 12. Frontend — situação real de rotas

Rotas existentes na `main` em 31/08/2026:

```text
/login
/meus-pedidos
/pedidos/novo
/pedidos
/estoque
/estoque/:id
/movimentacoes
/relatorios
/solicitacoes/novo
/solicitacoes/meus-pedidos
/:pathMatch(.*)*  → NotFoundView
```

Não documentar `/dashboard` ou rotas de `/cadastros/*` como já implementadas enquanto não existirem no router.

A página 404 customizada/animada já está concluída e usa o asset:

```text
public/animations/folder-not-found.lottie
```

---

# 13. Login e autenticação

O frontend possui a interface de login finalizada visualmente, porém o fluxo atual é **sessão de desenvolvimento**.

O store atual:

```text
GET /v1/usuarios
→ procura usuário ativo
→ exige preenchimento de senha na interface
→ NÃO valida a senha no backend
→ persiste sessão DEV no localStorage
```

Isso é intencional até a etapa oficial de autenticação.

Não confundir:

```text
login visual/fluxo DEV        ✅
autenticação local definitiva ⏳
autorização real              ⏳
auditoria por usuário logado  ⏳
integração corporativa/SSO    ⏳
```

---

# 14. Unidade institucional

Decisão aprovada no frontend:

```text
Unidade NÃO terá CRUD manual na interface administrativa.
```

A origem futura será a API corporativa durante autenticação/sincronização.

Portanto, a Administração planejada é:

```text
Cadastros
├── Produtos
├── Laboratórios
├── Projetos
├── Usuários
└── Estagiários
```

O CRUD técnico de Unidade existente no backend pode permanecer enquanto necessário para DEV, testes e compatibilidade. Não criar `/cadastros/unidades` no frontend como fluxo normal.

---

# 15. Próxima etapa oficial

A próxima grande etapa funcional é:

```text
Administração / Cadastros
```

Ordem atual:

```text
1. Produtos
   └── incluir fiscalização na criação/edição
2. Laboratórios
3. Projetos
4. Usuários
5. Estagiários
```

Tipos de unidade/embalagem só devem virar cadastro quando o backend tiver domínio próprio para isso; hoje não devem ser inventados no frontend.

---

# 16. Planejamento após Administração

Sem criar um novo roadmap, a sequência consolidada é:

```text
1. Administração / Cadastros                    ← próximo
2. Reconciliação + integração de Resíduos
3. Relatório + PDF/XLSX de Resíduos
4. Documentos/upload e rotulagem ainda pendentes
5. Dashboard final, alertas e robustez
   └── 404 já concluída
6. Autenticação + autorização + auditoria local definitiva
7. Integração corporativa / sincronização de Unidade
8. Refactor pós-protótipo: nomenclatura técnica para inglês
```

A ordem pode receber ajustes de dependência, mas não deve ser reorganizada silenciosamente por outra IA. Se surgir uma nova necessidade, registrar a decisão antes de alterar o planejamento oficial.

---

# 17. Pendência pós-protótipo — código em inglês

Está registrada em `docs/PENDENCIAS_POS_PROTOTIPO.md`.

Objetivo:

```text
classes, métodos, DTOs, services, repositories,
controllers e nomes técnicos do frontend
→ padronizados em inglês
```

A interface para o usuário permanece em português.

Esse refactor **não deve ser executado agora junto com novas funcionalidades**. Deve preservar, sempre que possível:

```text
tabelas/colunas existentes
migrations antigas
paths públicos da API
campos JSON
valores persistidos de enum
```

Executar apenas depois de o protótipo funcional estar fechado e com testes de regressão.

---

# 18. Guardrails para outra IA

Não fazer sem decisão explícita:

- não criar novo roadmap substituindo o atual;
- não reabrir módulos já validados só porque documentos antigos dizem “próximo”;
- não renomear contratos públicos durante um refactor interno;
- não mudar migrations Flyway antigas já aplicadas;
- não usar IDs numéricos como padrão público novo;
- não duplicar regras FIFO/FEFO no frontend;
- não inferir fiscalização por risco;
- não criar CRUD manual de Unidade no frontend;
- não tratar a branch antiga de resíduos como pronta para merge;
- não criar relatório próprio de pedidos entregues;
- não tratar o login DEV como autenticação segura;
- não fazer baixa de estoque novamente na entrega.

---

# 19. Checklist de retomada para outra IA

Antes de alterar código:

```text
[ ] ler este dossiê
[ ] ler CONTINUIDADE.md do repositório atual
[ ] conferir a branch main e commits recentes
[ ] conferir Swagger se a tarefa envolve API
[ ] identificar a etapa atual antes de implementar
[ ] criar branch própria
[ ] preservar decisões de domínio
[ ] validar o bloco antes do merge
[ ] atualizar CONTINUIDADE e documentação afetada
```

Para o próximo trabalho funcional esperado:

```text
[ ] Administração → Produtos
[ ] conferir contratos reais de Produto no Swagger
[ ] manter UUID público
[ ] incluir fiscalização
[ ] não criar Unidade em Cadastros
[ ] validar visual e integração
[ ] só depois seguir para Laboratórios
```

---

# 20. Documentos que devem ser lidos junto com este dossiê

Backend:

- `CONTINUIDADE.md`
- `docs/README.md`
- `docs/RELATORIOS.md`
- `docs/EXPORTACAO_RELATORIOS.md`
- `docs/PENDENCIAS_POS_PROTOTIPO.md`
- Swagger/OpenAPI em execução

Frontend:

- `CONTINUIDADE.md`
- `docs/README.md`
- `docs/ROADMAP_INTERFACE_GESTAO.md`
- `docs/DECISAO_UNIDADES_CORPORATIVAS.md`
- `docs/IDENTIDADE_VISUAL.md`
- `docs/PADROES_PAGINA.md`

---

# 21. Resumo em uma frase

**O SGL já possui os fluxos operacionais principais de pedidos, estoque/lotes, movimentações, fiscalização e relatórios com exportação; a próxima etapa é Administração/Cadastros, enquanto Resíduos precisa ser reconciliado com a `main` antes da integração e autenticação definitiva permanece para depois do fechamento funcional do frontend.**
