# Integração para homologação — Produtos + Modalidades de bolsa

**Estado:** alterações reunidas nas branches colaborativas `collab/integracao-produtos-bolsas-2026-11` de **Sistema-SGL (API)** e **SGL-FRONTEND (Web)**. **Não integradas à `release`, não implantadas em produção.**

## Escopo consolidado

### 1. Hotfix de Produtos — funcionalmente validado pelo responsável

Mantidas as alterações de `collab/hotfix-produtos-descricao-catalogo-2026-11`:
- Descrição de produtos com texto longo: coluna PostgreSQL `TEXT` e entidade Java.
- Novo catálogo administrativo por Unidade, inclusive produtos sem `EstoqueCentral`.
- Rota `GET /api/v1/produtos/catalogo`, protegida para administradores.
- Registro de Unidade no cadastro e recuperação de produtos históricos sem estoque quando só há uma Unidade institucional.
- Mensagens de integridade e validação de campos, sem expor SQL.
- **Não alterar disponibilidade física para pedidos, FEFO/FIFO, movimentações, lotes e recipientes.**

### 2. Modalidades de bolsa por Unidade — aguardando homologação do responsável

Integradas **somente** mudanças funcionais da branch `collab/modalidades-bolsa-unidade-2026-11`:
- `ModalidadeBolsa` (catálogo: `nome`, `tipoBase`, `ativo`, Unidade), endpoints GET/POST/PUT/DELETE lógico.
- `VinculoEstagio` recebe `modalidadeBolsa` opcional e **snapshot** `modalidadeBolsaNome`; vínculos históricos sem modalidade continuam válidos.
- Contratos opcionais `modalidadeBolsaId` no cadastro inicial, criação de vínculo e registro de nova bolsa.
- Compatibilidade preservada com `TipoBolsa` existente. As opções originais CNPq/CAPES/Institucional/Voluntário/Contratual continuam funcionando.
- No frontend, seletor de modalidades por tipo-base, cadastro de novas modalidades durante o preenchimento e diálogo para renomear/inativar/reativar.
- Mudanças no fluxo institucional são pontuais: se a fonte oficial alterar o tipo base do vínculo, a referência de modalidade local deixa de ser aplicada. O histórico da ocorrência anterior permanece registrado.
- **Massa DEMO restaurada após regressão:** `DemoDataInitializer` contém seis estagiários ativos com vínculos institucionais e participações abertas, além de dois estagiários encerrados para testar filtros. O inicializador é exclusivo de `@Profile("demo")`, não é executado em produção, e os dados permanecem fictícios. A restauração não inclui a proposta futura de histórico acadêmico.
- **Teste de prevenção:** `DemoDataInitializerIntegrationTest` confere 8 estagiários, 6 vínculos ativos, 6 participações e a consulta operacional de ativos.

## Versionamento Flyway

A `release` atual termina em V48. Nesta integração existem exatamente:
- **V49** — `V49__descricao_produto_texto_e_unidade_cadastro.sql` — produtos.
- **V50** — `V50__modalidades_bolsa_por_unidade.sql` — bolsas.

**Não executar/mesclar a V49 de bolsas da branch antiga.** Em um banco persistente que já tenha executado `V49__modalidades_bolsa_por_unidade.sql`, migrar/reconciliar cuidadosamente o histórico Flyway em vez de iniciar esta integração diretamente; versões Flyway executadas não podem ser simplesmente renomeadas sem plano de migração. A validação na DEMO/H2 não testa essas migrations, porque Flyway está desabilitado no perfil.

## Sequência para homologação local

1. Confirmar `git status` limpo em ambos os repositórios.
2. Executar `git fetch github --prune` e mudar para `collab/integracao-produtos-bolsas-2026-11` **nos dois projetos**.
3. Backend: `./mvnw clean verify` (no Windows: `mvnw.cmd clean verify`).
4. Frontend: `npm ci`, `npm run build`, `npm run test`.
5. Revalidar rapidamente o aceite de Produtos: descrição BDA longa e exibição de produto ativo sem estoque.
6. Validar novo cadastro de modalidade, vinculação a tipo-base compatível, edição de nome, inativação/reativação, prevenção de duplicidade dentro da Unidade.
7. Registrar nova ocorrência de bolsa escolhendo modalidade; conferir datas, encerramento da anterior, novo vínculo, atividades, orientador, curso e treinamento. O **nome histórico deve continuar o original após renomear/inativar a modalidade**.
8. Confirmar que tipo-base incompatível ou modalidade inativa/de outra Unidade é rejeitado pela API; fluxo sem modalidade segue funcionando.
9. Validar prorrogação da bolsa atual, que **não deve** registrar nova ocorrência nem perder a modalidade existente.
10. Reiniciar a DEMO/H2 e confirmar seis estagiários ativos com vínculo/participação e dois encerrados. Conferir também responsividade das telas do estagiário, inclusive no mobile.
11. Em DEV PostgreSQL com backup, confirmar aplicação de V49 + V50 em sequência e `spring.jpa.hibernate.ddl-auto=validate`.
12. Conferir que dados existentes não foram apagados e que catálogo, vínculos anteriores, pedidos e relatórios continuam íntegros.

**Limite da validação disponível nesta preparação:** os arquivos foram revisados e sincronizados no GitHub; **não foi executado Maven/Vitest nem aplicado Flyway em PostgreSQL** por este assistente. O aceite funcional de produtos foi comunicado pelo responsável. Não afirmar homologação de bolsas até receber os testes.

## Liberação posterior

Depois da homologação do responsável, abrir/usar MRs da integração para a `release` canônica do GitLab, **um por repositório**, e publicar tags coordenadas do backend e frontend. Fazer backup real de produção antes das migrations. Os PRs antigos de produtos (#37 backend e #59 frontend, no GitHub) não devem ser mesclados separadamente caso a integração consolidada seja aprovada.

## Proposta separada

Histórico acadêmico por pessoa, ainda apenas documental:
`collab/docs-modelagem-formacoes-academicas-2026-11` — `docs/PROPOSTA_HISTORICO_FORMACOES_ACADEMICAS.md`.
