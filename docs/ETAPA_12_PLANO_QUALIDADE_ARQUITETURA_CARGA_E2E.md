# Etapa 12 — Plano transversal de qualidade: arquitetura, carga, fluxos e frontend

**Instituído em:** 20/10/2026  
**Status:** PLANEJADO — não foram executados testes nem medidos indicadores deste plano.  
**Fonte do cronograma:** `docs/PLANO_PRE_PRODUCAO.md`, Etapas 12 e 13.  
**Escopo:** SGL inteiro — frontend Vue, backend Spring Boot, PostgreSQL/Flyway, integrações e operação; não apenas componentes de UI.

## 1. Por que esta etapa foi ampliada

Evitar a situação em que builds, testes unitários e demonstrações locais estejam verdes, mas a aplicação sofra com consultas em cascata, recarregamentos excessivos, timeout de API, tela branca, pressão no pool de conexões/memória, migração incompatível ou diferenças entre código e artefato implantado.

**Aprendizados de referência do Rascomp (não são falhas verificadas no SGL):**

- Listagens e dashboards podem ficar lentos com poucas dezenas de registros quando o backend usa N+1, consultas repetidas ou agregação pouco eficiente.
- Mutação simples não deve disparar recarga completa de telas, endpoints pesados ou requisições duplicadas.
- Timeouts no navegador, indisponibilidade do banco, health checks inadequados, pool/heap mal dimensionados e inicialização Flyway devem ser validados em conjunto.
- Scripts/build de sucesso não comprovam que a revisão correta foi publicada, nem que a navegação mobile não terá scroll quebrado ou tela em branco.
- Funcionalidades de alto peso (ex.: galeria de mídia) devem passar por orçamento e testes de impacto antes de entrar na arquitetura; evitar escopo novo durante estabilização.
- Falhas de migração por número duplicado, divergência de configurações e assets não encontrados devem ser identificadas antes de implantação.
- Distinguir **hipótese, evidência medida, correção testada e aceite**. Sem números de SGL medidos, não alegar capacidade ou tempos específicos.

## 2. Premissas e limites

1. Executar em **ambiente isolado de homologação**, nunca teste destrutivo/de carga contra banco institucional, dados reais ou produção sem autorização específica e isolamento.
2. Congelar uma referência de Git/artefatos e configuração (sem segredos) para reproduzir resultados. Não misturar branches, GitLab/main e GitHub/collab.
3. Usar massas sintéticas reprodutíveis por Unidade, Laboratório, Projeto, SCI, Atividade, Estagiário, participação, Produto, Lote, Pedido, Solução, Resíduo e Relatório. Cobrir múltiplas Unidades sem vazamento de dados.
4. Definir primeiro perfil de uso esperado, volume de dados, concorrência e orçamento de hospedagem/DB; **medir a linha de base**; aprovar metas mensuráveis p95/p99, taxa de erros, utilização e tempo de recuperação antes do aceite. Nenhum limite numérico é presumido.
5. Aproveitar JUnit/Spring tests, Flyway, Vitest + Vue Test Utils e Cypress; adicionar k6 (ou equivalente) para carga se tecnicamente apropriado, após avaliação das dependências. Testes não substituem autorização real.
6. Preservar invariantes de domínio: FEFO/FIFO, fracionamento decidido pelo lote, baixa única na aprovação, snapshots, Estagiário por participação e regras de tenant/Unidade. Não criar funcionalidades novas para «facilitar» testes.

## 3. Blocos obrigatórios

### 12.1 — Auditoria de arquitetura e dependências

- Mapear fluxo navegador → roteador/store → serviço HTTP → Controller → Service → Repository → PostgreSQL e componentes externos.
- Inspecionar acoplamentos, ciclos, duplicações de chamadas, queries N+1, endpoints com carga desnecessária, índices, planos `EXPLAIN`, cardinalidade, paginação e filtros executados no banco quando apropriado.
- Medir comportamento com massa pequena, média e representativa do cenário aprovado, inclusive usuário com participações em múltiplas Atividades/Projetos e Laboratórios.
- Verificar limites de transação, locks, concorrência, consistência de estoque e consultas que crescem linearmente por item ou multiplicam associações.
- Revisar cache/invalidação, requests paralelos, requests cancelados, retries, estado das stores, carregamento inicial e tratamento de erros.
- Auditar componentes e rotas mais pesados, tamanho do bundle, renderização inicial, lazy loading, dependências e superfícies de falha.
- **Saída:** mapa de arquitetura atualizado + inventário de riscos com evidência, severidade e ação proposta. Não refatorar sem teste de regressão.

### 12.2 — Cobertura automática por camada

| Camada | Casos mínimos |
| --- | --- |
| Backend unitário | regras de domínio, validações, cálculos, estados, snapshots, autorização/escopo onde implementada |
| Backend integração | PostgreSQL real do ambiente de teste, Flyway, constraints, tenant, isolamento, rollback, concorrência e operações críticas |
| Frontend unitário | formatadores, seleção 0/1/N, conversões, guards de UI, tratamento de exceções e estados vazios |
| Frontend componentes | formulários, filtros, modais, drawers, seleção, focus, loading, erros, acessibilidade, responsividade funcional |
| Contrato API | request/response, mensagens de validação, compatibilidade legado, paginação, tipos numéricos/unidades, status HTTP |
| E2E | navegador real integrado à API/banco de teste, do login/sessão à confirmação e rastreabilidade |

Nunca usar mock para «comprovar» comportamento da API, banco ou estoque. Diferenciar testes isolados, integração e E2E.

### 12.3 — Matriz de fluxos ponta a ponta (E2E)

Automatizar cenários prioritários e complementar com inspeção manual quando interação física for indispensável.

1. Perfis e Unidade: Administrador, Gestor, Técnico, Analista, Pesquisador e Estagiário; permissões, visibilidade por Unidade, navegação e tentativa negativa entre tenants.
2. Gestão de Projeto: Laboratório → Projeto → SCI → Atividade; datas, situação operacional, encerramento/prorrogação e histórico.
3. Estágio: Usuário → Vínculo → Participação, 0/1/N contextos elegíveis, laboratório-base diferente do laboratório da Atividade, vínculo encerrado e histórico sem reescrita.
4. Estoque físico: Produto → Lote/entrada → embalagens completas ou medidas físicas → Pedido → aprovação → entrega → movimentação/rastreabilidade. Incluir FEFO/FIFO, validade, insuficiência, fracionável/não fracionável, recipientes abertos, bloqueio de abertura desnecessária, estoque mínimo, concorrência e cancelamentos.
5. Soluções: catálogo e composição personalizada → Projeto/contexto → aprovação integral atômica → preparo/entrega → cancelamento preparado/não preparado, sem segunda baixa ou devolução fictícia.
6. Resíduos: solicitação, análise, liberação, armazenagem, rastreio, cancelamento, EPI e histórico.
7. Relatórios/exports: Projeto, síntese operacional, movimentações, PDF/XLSX, filtros, dados históricos, visibilidade por Unidade e robustez com massas maiores.
8. Rótulos e Manual: geração, impressão/visualização e acesso conforme perfis, nos contratos aprovados nas Etapas 10/11.
9. Falha/recuperação: atualização de página, troca de rota, expiração de sessão, resposta 4xx/5xx, demora na API e banco indisponível; preservar feedback legível, sem tela branca.
10. Responsividade: mobile/tablet/desktop, zoom 100%, Light/Dark, scroll horizontal, drawer/modal, teclado/focus, listas longas e layout sem cortes.

Cada fluxo deve ter *caminho feliz*, caso negativo e verificação de estado persistido. Priorizar rastreabilidade, integridade e isolamento em relação ao volume de testes superficiais.

### 12.4 — Desempenho do navegador e da API

- Medir carregamento inicial e navegação entre rotas em cache frio/quente, contagem e tamanho de chamadas HTTP, interações comuns, gravação sem recarga completa e tempo de resposta percebido.
- Registrar tempo de renderização, métricas web relevantes, erros de console, requests redundantes, bundle/JS/CSS e crescimento de memória em navegação longa.
- Medir API por endpoint crítico, especialmente login/sessão DEV, dashboards, Pedidos, Soluções, Estagiários, Projetos, Estoque, alertas e relatórios.
- Registrar latência p50/p95/p99, throughput, timeouts, erros, consultas por requisição, queries lentas, uso de CPU/heap, GC, conexões Hikari, utilização do PostgreSQL, locks e I/O.
- Investigar N+1, falta de paginação/limites, serialização de grafos e agregações caras mesmo quando houver «poucos usuários».
- Confirmar cancelamento de requests, invalidação correta, ausência de re-fetch integral a cada ação e recuperação de tela sem recarregar dados invariantes.
- Para cada ajuste: *antes/depois* medido com o mesmo cenário, commit, massa e ambiente. Não declarar melhoria sem evidência.

### 12.5 — Testes de carga, estresse, concorrência e estabilidade

**Ferramenta candidata:** k6 ou equivalente. Cenários calibrados a partir do uso institucional esperado, sem inventar SLA/capacidade.

1. **Baseline:** 1 usuário e dados representativos; obter custos normais de cada fluxo.
2. **Carga nominal:** concorrência e volume planejados, com mistura realista de consultas e escritas.
3. **Picos:** aumento gradual até o pico acordado, com pausa/redução; detectar limiares e degradação.
4. **Estresse:** ambiente isolado, pressionando além da carga-alvo até encontrar falhas, sem atingir recursos compartilhados/institucionais.
5. **Soak/endurance:** carga sustentável por intervalo acordado, observando conexões não liberadas, vazamento de memória e acúmulo de threads.
6. **Concorrência de escrita:** dois aprovadores, mesmo lote, cancelamento em disputa, alterações de participação, geração simultânea de relatório; verificar atomicidade e idempotência quando aplicável.
7. **Recuperação:** reinício da API, latência/falha temporária do banco de testes, pool saturado, timeout e retry; aferir recuperação sem duplicar escrita ou corromper estoque.

Separar **teste de navegador com usuários virtuais** de **teste HTTP de carga**: um não demonstra sozinho a qualidade do outro. Salvar scripts e dados sintéticos reproduzíveis.

### 12.6 — Robustez, infraestrutura e implantação

- Executar `Flyway migrate` a partir de banco de teste limpo e de versão anterior; conferir numeração sem duplicatas, histórico de migrations imutável, schema atual e rollback operacional planejado.
- Validar startup, health/readiness, configuração de JVM/pool/heap, falha de DNS/TLS do banco, reconexão e comportamento quando serviços ficam temporariamente indisponíveis.
- Confirmar diferenças DEV/DEMO/homologação, variáveis de ambiente e secrets (somente inventário de nomes, nunca valores em logs/repositório).
- Revisar estratégia de persistência, backups/restauração **efetivamente ensaiados em ambiente isolado**, monitoração, logs, métricas e alertas.
- Verificar hashes das versões publicadas, consistência de build/runtime, rotas diretas do SPA, assets, caching, API URL, CORS e atualizações sem tela branca.
- Avaliar cenários de tráfego baixo/cold start e picos, limites de memória e conexões compatíveis com a infraestrutura escolhida.
- Testar limites de upload, download, exports e payloads grandes, sem introduzir módulo novo.
- A autenticação institucional, matriz de autorização e auditoria definitivas são **portões obrigatórios de produção** posteriores. A Etapa 12 deve identificar e registrar riscos residuais; jamais marcá-los como resolvidos por existirem testes de sessão DEV.

### 12.7 — Política de execução e evidência

**Camadas, sem sobrecarregar o desenvolvimento:** typecheck/lint e unitários focalizados durante ajustes; suites de regressão por módulo antes do aceite; E2E crítico antes do merge/release; suíte integral, carga e soak em janela reservada de homologação. Não executar carga a cada alteração de CSS nem criar workflows redundantes/ruidosos que provoquem spam; CI deve usar filtros, agrupamento e limites adequados.

**Evidência para cada cenário:** identificação, ambiente/commit, massa, ferramentas/comandos, passos, screenshots quando visual, logs higienizados, indicadores, esperado vs. observado, status, correção e reteste. Classificar achados: bloqueante/crítico/alto/médio/baixo, sem «suite verde» genérica.

**Portão de aceite 12:** riscos bloqueantes corrigidos ou explicitamente impedindo liberação; fluxos críticos e isolamento testados; metas de desempenho/carga previamente aprovadas e atingidas; dashboards/listagens sem consultas explosivas; mobile sem bloqueio funcional; migrações, startup e recuperação verificadas; documentação/testes versionados. QA de execução deve ter evidências, não apenas uma lista de testes.

## 4. Integração com a Etapa 13 e o portão de produção

A Etapa 12 produz diagnóstico e suíte reproduzível. A Etapa 13 usa os achados para **refactor incremental, revisão completa do sistema e regressão final**:

- Revisar arquitetura implementada versus a desenhada: domínios, Services, repositories, controllers, DTOs, UI, acesso a dados, indexes e deploy.
- Priorizar gargalos efetivamente medidos, segurança, manutenção, excesso de responsabilidades e duplicações.
- A cada mudança estrutural, reexecutar unitários, integração e E2E correspondente; ao fim, repetir toda a matriz crítica e os testes de carga comparáveis.
- Verificar que integrações entre módulos, snapshots, tenant, estoque e histórias não sofreram alteração.
- Entregar **dossiê técnico de homologação**: topologia, arquitetura, mapa de fluxos, hipóteses/achados, mudanças, métricas antes/depois, limites aceitos, pendências, evidências e decisão de go/no-go; registrar a revisão testada em ambas as branches/repos.
- Não confundir fechamento técnico das Etapas 12/13 com autorização institucional para produção; os portões de matriz de permissões, identidade/autorização e integração corporativa permanecem necessários.

## 5. Entregáveis versionados

1. Matriz de fluxos e perfis/Unidades, incluindo casos negativos.
2. Scripts de testes frontend/backend/E2E e carga; fixtures reprodutíveis.
3. Inventário de endpoints/queries críticas, N+1, paginação, cache, alocação de recursos e arquitetura de deploy.
4. Relatório de benchmarks, p50/p95/p99, taxa de erros, consumo de recursos, cenários e limitações (sem métricas inventadas).
5. Registro de incidentes/riscos, correções, regressões e pendências aceitas.
6. Checklist de implantação: migrations, configuração, backups, restauração, health, logs e artefatos.
7. Parecer final da Etapa 12 e, após a 13, novo parecer de regressão integral com referências exatas de commit.

**Execução futura.** Este documento é planejamento e não constitui autorização para testes de carga, alterações de infraestrutura, deploy ou merge.
