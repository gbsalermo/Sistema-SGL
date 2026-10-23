# Etapa 8 — Auditoria técnica, checkpoint 1 (23/10/2026)

**Branch auditada:** `collab/etapa-8-contextos-pedidos` (GitHub colaborativo).  
**Estado:** inspeção estática inicial do backend e reforço de testes; **sem execução local de Maven, banco, Flyway, carga ou merge**. Não equivale a homologação técnica final.

## Escopo efetivamente inspecionado
- Contratos: `docs/ETAPA_8_INICIO_E_REGRAS.md`, `docs/CONTRATO_CONTEXTOS_PEDIDOS_ETAPA_8.md`, `docs/ETAPA_8_CONTRATO_VINCULOS_ACESSO.md`, `docs/ETAPA_8_AJUSTES_VALIDACAO_2026-10-21.md` e plano transversal da Etapa 12.
- Serviços: `PedidoService`, `ResiduoService` e `VinculoOperacionalUsuarioService`; entidade de vínculo, entidade e DTO de Resíduo.
- Testes: `PedidoContextoOperacionalServiceTest`, `PedidoSolucaoServiceTest`, `ResiduoServiceTest`, `VinculoOperacionalUsuarioServiceTest`.
- Fluxos funcionais de vínculos, Pedidos, Soluções e Resíduos validados manualmente pelo responsável na Etapa 8, conforme conversa de homologação. Isso **não comprova** todas as variantes, integração real ou concorrência.

## Achados e classificação

1. **Pendente obrigatório antes de produção — identidade/autorizações reais.** O contrato documenta `permitAll`, sessão DEV e header de Unidade controlado pelo cliente. As regras de negócio de laboratório/tenant existentes não substituem autenticação, titularidade nem autorização server-side para operações administrativas. **Decisão expressa da Etapa 8: não implementar o provedor de identidade nesta fase**; manter exclusivamente DEV e não expor externamente.
2. **Divergência documental — Solução sem Projeto.** Os documentos iniciais de 20–21/10 ainda afirmam que Solução exige Projeto. A decisão e implementação posterior passaram a permitir `projetoId = null`, validadas manualmente. Documentação histórica deve ser preservada como checkpoint antigo, mas atualizar o contrato/roadmap vigente antes do merge.
3. **Lacuna de regressão corrigida neste checkpoint — testes do contrato novo.** Foram adicionados cenários unitários para Solução sem Projeto e para persistência do indicador `projetoSemVinculoDireto` no Resíduo com/sem vínculo de Projeto. **Não executados**, sujeitos a validação Maven.
4. **Ponto a aprofundar — invariantes de Projeto/SCI/Atividade e mensagens do frontend.** O backend reconhece vínculo específico herdado em `pertenceAoProjeto()`. Revalidar se todos os avisos de criação no frontend consideram os mesmos escopos e a vigência, evitando falso positivo.
5. **Ponto a aprofundar — integração/concorrência/performance.** Conferir migration V40–V44 em banco novo e atualizado, índices de consultas por vínculos, transações, FEFO/FIFO, locks de lotes, baixa única, soluções preparadas/não preparadas e cancelamento. Sem medições, não declarar ausência de N+1 nem capacidade suportada.
6. **Dados históricos.** Indicadores de Projeto sem vínculo direto são snapshots de criação para Pedidos e Resíduos novos. A migration V44 aplica `FALSE` aos Resíduos anteriores; não inferir autorização histórica de registros legados.

## Alterações deste checkpoint
- `src/test/java/com/sgl/service/PedidoSolucaoServiceTest.java`: criação de Solução sem Projeto.
- `src/test/java/com/sgl/service/ResiduoServiceTest.java`: persistência do indicador com vínculo de Projeto ausente/presente.
- Nenhuma regra de produção alterada durante esta rodada de auditoria.

## Portões seguintes (ainda pendentes)
1. Executar `mvn test` e build backend na mesma branch; corrigir regressões identificadas.
2. Validar Flyway V40–V44 e testes de integração com banco DEV isolado.
3. Auditar sincronização dos formulários frontend com a hierarquia Projeto/SCI/Atividade, inclusive avisos falsos.
4. Revisar autorização tenant/perfil e validação de alterações/revogação durante operação, distinguindo escopo DEV e pré-produção.
5. Fazer regressão estoque/soluções/resíduos + revisão visual e testes de arquitetura/carga em ambiente isolado.
6. Atualizar roadmap vigente e documentação, confirmar GitLab/GitHub e solicitar aceite antes de merge.


## Checkpoint 5.4 — análise estática de consultas e startup DEV (23/10/2026)

**Evidência cruzada com o log de inicialização enviado pelo responsável:**
- Spring Boot e Hibernate iniciados, 44 migrations validadas. O log também exibiu consultas repetidas de recipientes de lote, Projetos, SCI e Atividades.
- `src/main/java/com/sgl/test/DataInitializer.java` é `@Profile("dev")`, implementa `CommandLineRunner` e, quando já existe massa, executa `validarMassaEstoqueEtapa7Existente()`, `garantirCadastrosResiduos()`, `garantirDadosEtapa5()` e `garantirDadosEtapa6()`.
- Em `validarMassaEstoqueEtapa7Existente()`, há `loteRepository.findAll()` e um `anyMatch` que executa `recipienteEstoqueRepository.findByLoteIdOrderBySequencialAsc(lote.getId())` **por lote até encontrar um sem recipiente**, mesmo quando só necessita testar existência. Este é um padrão N+1 confirmado em rotina de validação DEV, não uma falha operacional comprovada da API.
- As consultas de hierarquia também aparecem em verificações de massa DEV, mas seu peso em requisições reais **não foi medido**.

**Decisão neste checkpoint:** não alterar automaticamente esse inicializador nem o motor de Estoque durante a homologação. A inicialização já passou; uma otimização exigiria teste de regressão preservando a detecção de massa incompatível. Registrar uma melhoria futura para usar consulta de existência/agrupamento no repositório em vez de carregar todos os recipientes por lote.

**Limites da conclusão:** análise estática + log de inicialização NÃO provam latência, número de queries por requisição, uso de CPU, N+1 nas telas ou carga máxima. A medição de endpoints com massa representativa continua na Etapa 12.

**Próximo portão da Etapa 8:** 5.5, revisão de build, testes automatizados disponíveis do frontend e lacunas de arquitetura/fluxos, mantendo MVP e autenticação fora do escopo atual.


## Checkpoint 5.5 — primeira integração ArchUnit

- Adicionada dependência **somente de testes** `com.tngtech.archunit:archunit-junit5:1.5.1` ao `pom.xml`.
- Criada suíte `src/test/java/com/sgl/architecture/SglArchitectureTest.java` com cinco regras básicas: entidades, DTOs, repositórios e serviços não dependem de Controllers; DTOs não dependem de Repositories.
- Não foram introduzidas regras de impedimento de acesso Controller→Repository nem proibição abrangente de ciclos sem inventariar possíveis dependências legadas primeiro.
- A adição **não altera classes de produção nem a modelagem de Estoque**.
- **Pendente:** executar `./mvnw -Dtest=SglArchitectureTest test` (ou `mvnw.cmd -Dtest=SglArchitectureTest test` no Windows) e registrar eventuais violações reais. Não declarar verde sem resultado.
- Os testes arquiteturais completos, métricas e fluxo E2E do sistema inteiro continuam no programa transversal da Etapa 12.


## Checkpoint 5.5 validado — resultado apresentado pelo responsável (23/10/2026)

- Executado `SglArchitectureTest` em JUnit 5: **5/5 testes aprovados, 0 falhas, 0 erros**, comprovado por captura do executor.
- A falha Maven anterior, ocorrida durante a compilação de classes principais, não apareceu nesta execução; não atribuir a causa retrospectivamente sem evidência do comando/ambiente que a corrigiu.
- Build TypeScript/Vite do frontend foi informado como validado após correções de `VinculosUsuariosCadastro.vue` e `SolucaoInput.projetoId`.
- Inicialização DEV validada com PostgreSQL e **44 migrations Flyway** já aplicadas.
- Teste 5.3: revisão estática das suítes de estoque e Soluções concluída; testes de carga, consulta por endpoint, concorrência e E2E representativos **não executados nesta auditoria**, mantidos na Etapa 12.

## Checkpoint 5.6 — preparação do fechamento

**Validações funcionais em DEV informadas pelo responsável:** Blocos 1–4 (Vínculos, Pedidos, Soluções, Resíduos) concluídos. A compilação Java/ArchUnit e frontend passou nos cenários reportados. Não se deve confundir este aceite funcional com a avaliação de produção.

**Contrato operacional consolidado:** o Projeto é opcional para Pedidos gerais, Soluções e Resíduos conforme decisões posteriores da Etapa 8; laboratório-base ou vínculos adicionais vigentes definem autorização para perfis comuns; vínculo específico de Projeto, SCI ou Atividade pode gerar contexto de Projeto, inclusive herdado; Projeto do laboratório autorizado sem vínculo específico gera aviso histórico para análise/recebimento, não um impedimento automático.

**Pendências anteriores a qualquer merge:**
1. Conferir `git status` e `git log` nos clones locais para evitar diferenças não enviadas.
2. Confirmar sincronização da branch colaborativa no GitHub e GitLab **sem forçar atualização da main institucional**. Não há evidência de sincronização nesta auditoria.
3. Executar, se ainda não houver resultado recente no HEAD final, `./mvnw test` e `npm run build` nas revisões que irão para MR; ArchUnit isolado aprovado não é substituto da suíte inteira.
4. Revisão humana do diff e autorização explícita do responsável para merge. Nenhum merge realizado.
5. Registrar como próximos trabalhos as medições de desempenho, carga e E2E completos da Etapa 12, além de eventual revisão separada de identidade institucional fora do MVP atual.
