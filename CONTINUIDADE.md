# Continuidade do Projeto SGL — Backend

**Projeto:** Sistema de Gestão de Laboratórios  
**Backend:** `gbsalermo/Sistema-SGL`  
**Frontend:** `gbsalermo/SGL-FRONTEND`  
**Última atualização:** 04/09/2026  
**Branch estável:** `main`  
**Fase atual:** ajustes de pré-produção pós-aprovação funcional.  
**Bloco atual:** planejamento consolidado → execução sequencial das etapas de pré-produção.  
**Etapa atual:** Etapa 1 — padronização e refinamento visual global.  
**Plano oficial da pré-produção:** `docs/PLANO_PRE_PRODUCAO.md`  
**Roadmap formal posterior:** matriz de permissões → congelamento → homologação final → segurança/integração corporativa.  
**Handoff completo:** `docs/DOSSIE_PROJETO_SGL.md`

Este arquivo é o checkpoint principal de retomada. Para o fluxo detalhado do bloco atual, ler `docs/PLANO_PRE_PRODUCAO.md`. Para contratos HTTP, usar sempre o Swagger/OpenAPI em execução.

---

# 0. Regra de trabalho

```text
branch própria
→ implementação/revisão
→ validação
→ refinamento
→ Pull Request
→ main
→ atualizar documentação afetada
```

Não reabrir módulos aprovados sem uma necessidade concreta. Mudanças de pré-produção devem preservar o comportamento funcional aprovado, salvo decisão explícita em contrário.

**Regra especial deste bloco:** alterações funcionais de backend serão implementadas manualmente pelo responsável do projeto. O apoio de IA deve analisar, modelar, orientar a implementação e revisar o resultado; não aplicar diretamente código funcional de backend sem nova autorização explícita.

---

# 1. Estado consolidado

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
PDF / XLSX                                            ✅
Resíduos — fluxo operacional completo                 ✅
Estagiários — vínculo + encerramento                   ✅
Pessoas por laboratório                               ✅
Suporte a Administração/Cadastros                     ✅
Alteração administrativa de perfil                    ✅
Isolamento operacional por Unidade                    ✅
Autenticação/autorização/auditoria definitiva         ⏳ roadmap formal
Integração corporativa                                ⏳ roadmap formal
```

## Frontend integrado

```text
Login visual / sessão DEV                             ✅
Expiração automática da sessão DEV em 5h              ✅
Pedidos do solicitante                                ✅
Pedidos da gestão                                     ✅
Estoque e lotes                                       ✅
Movimentações                                         ✅
Resíduos — solicitante e gestão                       ✅
Rótulos de Produto e Resíduo                          ✅
Estagiários                                           ✅
Relatórios + PDF/XLSX                                 ✅
Pessoas por laboratório                               ✅
Administração / Cadastros                             ✅
Dashboard Gestão                                      ✅
Dashboard Solicitante                                 ✅
Alertas operacionais                                  ✅
Busca global                                          ✅
Tema claro/escuro com persistência                    ✅ base atual; refinamento planejado
Página 404                                            ✅
Contexto de Unidade enviado à API                     ✅
Testes automatizados frontend                         ⏳ Etapa 9 — Vitest/Vue Test Utils + Cypress
Autenticação/autorização definitiva                   ⏳ roadmap formal
```

O produto foi aprovado funcionalmente. O trabalho atual é de pré-produção e refinamento, não de reconstrução do primeiro protótipo.

---

# 2. Ordem de precedência

Quando houver conflito entre documentos:

```text
1. código da main
2. Swagger/OpenAPI para contratos HTTP
3. CONTINUIDADE.md do repositório em trabalho
4. docs/PLANO_PRE_PRODUCAO.md durante o bloco atual
5. docs/DOSSIE_PROJETO_SGL.md
6. documentos específicos de decisão/módulo
7. roteiros, exemplos e documentos históricos
```

Datas de upload/commit de documentos históricos não transformam conteúdo antigo em fonte de verdade.

---

# 3. Arquitetura e identificadores

```text
Controller = contrato HTTP
Service = regra/transação/orquestração
Repository = persistência
Model = estado de domínio
RequestDTO = entrada
ResponseDTO = saída
```

Identificadores:

```text
Long id
→ banco, JPA, FKs e locks

UUID publicId
→ DTOs, endpoints e frontend
```

Fluxo padrão:

```text
Controller recebe UUID
→ Service resolve por publicId
→ domínio usa Long internamente
```

---

# 4. PostgreSQL e Flyway

Ambiente de desenvolvimento padrão:

```text
PostgreSQL
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

O Hibernate valida o schema; quem evolui o banco é o Flyway.

Migrations atuais:

```text
V1 ... V12
```

Regra obrigatória: migration já aplicada é imutável. Nova alteração de schema recebe uma nova versão.

---

# 5. Multitenancy por Unidade — estado atual

O isolamento por Unidade já está integrado à `main`.

Backend:

```text
TenantRequestFilter
→ lê X-SGL-Unidade-Id
→ valida UUID
→ define TenantContext durante a requisição
→ limpa o contexto no final
```

`TenantProvider` e consultas/services usam a Unidade atual para restringir dados em áreas como usuários, laboratórios, projetos, produtos, estoque, lotes, pedidos, movimentações, estagiários e resíduos.

Frontend:

```text
sessão DEV contém unidadeId/unidadeSigla
→ interceptor HTTP lê unidadeId
→ envia X-SGL-Unidade-Id em chamadas à API
```

Interpretação correta:

```text
isolamento funcional por Unidade              ✅
validação de cenários multitenant              ✅
fronteira definitiva de segurança              ❌ ainda não
```

Enquanto a Unidade vier de um header controlado pelo cliente e a autenticação definitiva não existir, esse mecanismo não deve ser tratado como autorização segura de produção. Na integração corporativa, Unidade/tenant deve ser derivado da identidade autenticada.

---

# 6. Estoque, lotes e pedidos

```text
Produto = catálogo
EstoqueCentral = saldo consolidado por produto/Unidade
Lote = validade + saldo + embalagem + rastreabilidade
MovimentacaoEstoque = trilha das operações físicas
```

Seleção de lotes:

```text
perecível     → FEFO
não perecível → FIFO
```

Invariantes:

```text
EstoqueCentral.quantidadeAtual acompanha os lotes
aprovação baixa estoque
entrega NÃO baixa novamente
cancelamento aprovado restaura os lotes exatos utilizados
lote vencido não participa da aprovação
movimentação identifica o lote efetivamente afetado
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

A Etapa 7 da pré-produção revisará a representação de unidades de medida e apresentações físicas antes da introdução de Soluções. Até lá, preservar as regras atuais.

---

# 7. Pedidos

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

Regras consolidadas:

```text
criação → não baixa estoque
aprovação → baixa física
entrega → conclusão sem segunda baixa
cancelamento aprovado → restaura quantidades dos lotes utilizados
urgência → não altera FIFO/FEFO
```

`Pedido.dataEntrega` registra o evento real de entrega.

A introdução de Soluções em Pedidos está planejada para a Etapa 7 e depende da normalização de unidades e do cadastro de Soluções.

---

# 8. Resíduos

Decisão central:

```text
Produto != Resíduo
```

Componente de Resíduo pode referenciar Produto para rastreabilidade sem alterar estoque automaticamente.

Fluxo atual:

```text
INFORMADO
→ EM_ANALISE
→ LIBERADO_PARA_ARMAZENAMENTO
→ ARMAZENADO_TEMPORARIAMENTE
→ DESPACHADO
```

Código SGL:

```text
SGL-RES-AAAA-NNNNNN
```

O código existe desde o registro inicial; V12 realizou backfill dos registros anteriores.

Pré-produção planejada:

- Etapa 3: remover redundância visual de análise, refinar o rótulo e separar geração/visualização de permissão de impressão;
- Etapa 4: locais de armazenamento cadastráveis e modelos de Resíduos pré-cadastrados pela Gestão, com escolha entre modelo padrão e preenchimento manual pelo Solicitante.

Distinção futura obrigatória:

```text
ModeloResiduo = definição reutilizável/padrão
Residuo       = ocorrência operacional real
```

Alterações posteriores no modelo não devem modificar retroativamente Resíduos já registrados.

Detalhes do plano: `docs/PLANO_PRE_PRODUCAO.md`. Detalhes do domínio atual: `docs/MODULO_RESIDUOS.md`.

---

# 9. Estagiários e estrutura institucional

Cobertura atual:

```text
listar / consultar
cadastrar
editar
ativos
por laboratório
unidade explícita no vínculo
período de estágio
tipo de vínculo
encerramento com data efetiva
```

Tipos atuais:

```text
BOLSA_CNPQ
BOLSA_CAPES
BOLSA_INSTITUCIONAL
VOLUNTARIO
CONTRATUAL
```

Regras atuais principais:

- usuário precisa ter perfil `ESTAGIARIO`;
- não há dois registros de estágio para o mesmo usuário;
- laboratório e usuário precisam pertencer à mesma Unidade;
- data final não pode anteceder a inicial;
- usuário vinculado não é trocado durante edição;
- encerramento grava data efetiva e não pode ser repetido.

A Etapa 5 da pré-produção reestruturará Projetos e vínculos de Estagiários. O vínculo Projeto–Estagiário deverá preservar histórico, atividade exercida, período, status, encerramento e renovação, permitindo múltiplos projetos ao longo do tempo.

---

# 10. Administração / Cadastros

A central administrativa usa suporte backend para:

```text
Laboratórios
Projetos
Produtos
Permissões/perfis de usuários existentes
```

Decisões vigentes:

- Unidade é dado institucional; não há CRUD manual normal no frontend;
- usuário não é criado manualmente na central administrativa;
- Administração pode alterar perfil de usuários existentes;
- Produto em Cadastros é catálogo, não estoque;
- responsável de Laboratório deve pertencer à mesma Unidade;
- `ESTAGIARIO` com vínculo ativo não deve perder esse perfil antes do encerramento.

Alterações planejadas:

- Etapa 4: liberar cadastro de modelos de Resíduos padrão e locais de armazenamento;
- Etapa 5: ampliar Projetos com código obrigatório, ciclo de vida e vínculos de pessoas;
- Etapa 7: incluir cadastro de Soluções padrão.

---

# 11. Fiscalização

Campos:

```text
fiscalizado
orgaosFiscalizadores
observacaoFiscalizacao
```

Órgãos atuais:

```text
POLICIA_FEDERAL
VIGILANCIA_SANITARIA
ANVISA
EXERCITO
OUTRO
```

Se `fiscalizado=true`, pelo menos um órgão deve existir. Não inferir fiscalização por risco ou perecibilidade.

---

# 12. Relatórios e exportações

Relatórios integrados atualmente:

```text
1. Estagiários
2. Produtos
3. Movimentações
4. Resumo operacional
5. Estoque e lotes
6. Fiscalização
7. Resíduos
8. Pessoas por laboratório
```

Pedidos entregues são recorte de Movimentações, não relatório próprio.

```text
prévia JSON
PDF
XLSX
→ mesma consulta e mesmos filtros
```

A Etapa 6 avaliará uma visão consolidada de Laboratórios e Projetos após a estabilização do novo domínio de Projetos/Estagiários.

Detalhes atuais: `docs/RELATORIOS.md` e `docs/EXPORTACAO_RELATORIOS.md`.

---

# 13. Dashboard, alertas e busca

O frontend compõe dados reais já expostos por pedidos, estoque, lotes, resíduos, movimentações, laboratórios e usuários.

Indicadores usados incluem:

```text
pedidos pendentes/urgentes
estoque baixo
lotes vencidos
lotes vencendo em 7/30 dias
resíduos INFORMADO/EM_ANALISE
movimentações recentes
resumo por laboratório
```

Novo KPI com regra oficial complexa deve preferir endpoint/serviço backend próprio, evitando duplicação de regra no frontend.

---

# 14. Segurança e sessão

Estado correto:

```text
Spring Security como base técnica                    ✅
guardas de rota no frontend                          ✅ UX
sessão DEV com expiração                             ✅ temporária
isolamento por Unidade via header                    ✅ desenvolvimento
autenticação definitiva                              ⏳
autorização global real                              ⏳
auditoria por identidade autenticada                 ⏳
integração corporativa/SSO                           ⏳
```

A configuração atual do backend ainda usa `permitAll()` de forma temporária. A futura autenticação deverá retirar dos payloads e headers controláveis pelo cliente a responsabilidade por identidade, perfil e tenant sempre que esses dados puderem vir da sessão/token confiável.

---

# 15. Fase atual — pré-produção pós-aprovação

O primeiro protótipo foi funcionalmente aprovado. O levantamento realizado durante a apresentação com o cliente foi consolidado em `docs/PLANO_PRE_PRODUCAO.md`.

Situação:

```text
limpeza/revisão documental                            ✅ concluída
planejamento dos ajustes                              ✅ consolidado
Etapa 1 — refinamento visual global                   ⏭ ATUAL / próxima implementação
Etapa 2 — Dark Mode definitivo                        ⏳
Etapa 3 — refinamentos do fluxo atual de Resíduos     ⏳
Etapa 4 — expansão operacional de Resíduos            ⏳
Etapa 5 — Projetos + vínculos de Estagiários          ⏳
Etapa 6 — relatórios de Projetos/Laboratórios         ⏳
Etapa 7 — unidades + Soluções + Pedidos               ⏳
Etapa 8 — Manual do Usuário + decisão delete lógico  ⏳
Etapa 9 — testes automatizados do Frontend            ⏳
```

Dependências centrais:

```text
padrão visual → Dark Mode
Resíduos atuais → expansão/modelos de Resíduos
Projetos/Estagiários → relatório de Projetos
unidades → Soluções → Pedidos com Soluções
Etapas 1 a 8 estabilizadas → testes automatizados frontend
```

A Etapa 9 adotará `Vitest + Vue Test Utils` para testes unitários/componentes e `Cypress` como ferramenta E2E principal. Selenium não é o padrão escolhido para o SGL neste planejamento.

O detalhe, escopo, regras e impacto de cada etapa estão em `docs/PLANO_PRE_PRODUCAO.md`.

---

# 16. Roadmap formal posterior

Depois de concluído o bloco atual de pré-produção:

```text
1. consolidar diretrizes/matriz de permissões
2. congelar o comportamento funcional
3. executar homologação integrada final
4. corrigir falhas encontradas
5. autenticação + autorização + auditoria definitiva
6. integração corporativa / SSO / Unidade confiável
7. demais contratos/documentos de produção necessários
8. refactors técnicos planejados
```

Esse roadmap continua válido; apenas não é o bloco em execução neste momento.

---

# 17. Documentação de referência

Começar por:

```text
README.md
CONTINUIDADE.md
docs/PLANO_PRE_PRODUCAO.md
docs/DOSSIE_PROJETO_SGL.md
docs/README.md
```

Depois consultar o documento específico da área em trabalho.

Para endpoints e payloads, confirmar sempre no Swagger/OpenAPI.

---

# 18. Regra final de retomada

**O SGL está funcionalmente aprovado. A fase atual é a execução do plano de pré-produção registrado em `docs/PLANO_PRE_PRODUCAO.md`, começando pela Etapa 1 — padronização/refinamento visual e terminando na Etapa 9 — testes automatizados do Frontend. Não tratar a matriz de permissões como tarefa imediata até que as etapas atuais sejam encerradas. Preservar regras consolidadas, usar a `main` como verdade e lembrar que alterações funcionais de backend neste bloco serão implementadas manualmente pelo responsável do projeto.**