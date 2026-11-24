# Documentação técnica — SGL Backend

**Revisão do índice:** 17/11/2026 · **Base de código:** `release` · **Estado:** organização documental concluída por inspeção estática; homologação completa ainda pendente.

> **Fonte de verdade:** GitLab institucional `release`. GitHub `release` é espelho colaborativo. `main` mantém histórico próprio e **não deve ser interpretada como a release atual**. Esta branch `collab/docs-release-estado-2026-11` contém **somente documentação** a ser revisada e integrada por MR. Não confundir existência de teste ou endpoint com homologação em produção.

Este arquivo é o **índice de leitura**. O conteúdo integral de sua versão anterior foi preservado em [`HISTORICO_INDICE_DOCUMENTACAO_ATE_2026-11-17.md`](HISTORICO_INDICE_DOCUMENTACAO_ATE_2026-11-17.md); instruções antigas sobre `main`, sessão DEV e etapas a iniciar permanecem lá **como histórico**, não como procedimentos correntes.

## Comece por aqui

| Ordem | Documento | Finalidade e autoridade |
|---|---|---|
| 1 | [`../CONTINUIDADE.md`](../CONTINUIDADE.md) | Checkpoint de trabalho e restrições para retomada |
| 2 | [`GOVERNANCA_DOCUMENTAL.md`](GOVERNANCA_DOCUMENTAL.md) | Precedência das fontes e como tratar material histórico |
| 3 | [`DOSSIE_PROJETO_SGL.md`](DOSSIE_PROJETO_SGL.md) | Arquitetura, stack, modelos, segurança, controllers/services/repositories e testes |
| 4 | [`MATRIZ_COERENCIA_FRONTEND_BACKEND_2026-11-12.md`](MATRIZ_COERENCIA_FRONTEND_BACKEND_2026-11-12.md) | Evidências comparadas diretamente no código backend/frontend, incluindo alertas C1–C21 |
| 5 | [`REGISTRO_PENDENCIAS_REVISAO_SGL_2026-11.md`](REGISTRO_PENDENCIAS_REVISAO_SGL_2026-11.md) | **Único backlog de correções** dos dois repositórios: SGL-REV-001 a SGL-REV-031 |
| 6 | [`PLANO_PRE_PRODUCAO.md`](PLANO_PRE_PRODUCAO.md) | Roadmap e decisões históricas de pré-produção; conferir notas de vigência |
| 7 | [`ETAPA_12_PLANO_QUALIDADE_ARQUITETURA_CARGA_E2E.md`](ETAPA_12_PLANO_QUALIDADE_ARQUITETURA_CARGA_E2E.md) | Arquitetura, segurança, testes, concorrência, carga e regressões **planejados** |
| 8 | [`PLANO_HOMOLOGACAO_NUVEM_MOBILE_2026-11-17.md`](PLANO_HOMOLOGACAO_NUVEM_MOBILE_2026-11-17.md) | Ensaio **não executado** de endpoints reais em staging e jornadas Android/iOS para estagiários |
| 9 | [`AUDITORIA_REFERENCIAS_E_SINCRONIZACAO_2026-11-17.md`](AUDITORIA_REFERENCIAS_E_SINCRONIZACAO_2026-11-17.md) | Conferência de caminhos Markdown, workflows e cuidados antes do MR |
| 10 | [`SINCRONIZACAO_GITLAB_GITHUB.md`](SINCRONIZACAO_GITLAB_GITHUB.md) | Fluxo de branches, direções de sincronização e validação de Git remotes |

**Inventário de origem:** [`AUDITORIA_DOCUMENTAL_BACKEND_2026-11-12.md`](AUDITORIA_DOCUMENTAL_BACKEND_2026-11-12.md) descreve a árvore `release` na abertura da auditoria (**51 Markdown naquele checkpoint**, antes das novas notas e documentos). Não tratar essa quantidade histórica como contagem da branch documental atual.

## Estado funcional comprovado por leitura do código

- **Autenticação:** login corporativo, JWT, usuário recarregado a cada requisição, perfil e estado de acesso no backend. Empregados novos podem entrar `ATIVO` por perfil sugerido; colaboradores/estagiários novos exigem regularização `PENDENTE_VINCULACAO`. Login DEV só com `dev/demo`.
- **Tenant:** Unidade derivada da identidade autenticada. O header `X-SGL-Unidade-Id` só serve ao `ADMINISTRADOR_GERAL` para seleção autorizada de outra Unidade; **não** é autenticação geral.
- **Operações:** Projeto → SCI → Atividade; participação e laboratório autorizado obrigatórios para Pedidos de Estagiário. Para empregados, Projeto pode ser opcional e solicitação a laboratório ativo da Unidade pode gerar sinalização de exceção.
- **Estoque e Soluções:** lotes/recipientes, FEFO/FIFO, fracionamento, devolução, baixa única e preparação. **Backend `release` contém as migrations V1–V48**; presença no Git não prova aplicação em cada banco.
- **Resíduos, Relatórios e impressão:** fluxos e endpoints presentes; rótulos e documento de entrada têm acesso no frontend, mas impressão física e equivalência das exportações ainda precisam de homologação.

**Atenção de segurança:** a matriz C2/C6/C9/C16 e o backlog identificam possíveis acessos de leitura excessivos dentro da mesma Unidade e cenários condicionais sem tenant. **Não declarar esses riscos corrigidos**; definir política, testar e só depois propor mudanças de backend.

## Guias especializados

| Assunto | Referências |
|---|---|
| Resíduos | [`MODULO_RESIDUOS.md`](MODULO_RESIDUOS.md); Etapa 10 para rótulos |
| Relatórios | [`RELATORIOS.md`](RELATORIOS.md), [`EXPORTACAO_RELATORIOS.md`](EXPORTACAO_RELATORIOS.md), [auditoria de 26/10](ETAPA_9_1_AUDITORIA_CONTRATOS_RELATORIOS_2026-10-26.md) |
| Estoque e Pedidos | [blueprint histórico da Etapa 7](ETAPA_7_MODELO_ESTOQUE.md), [contrato de contexto](CONTRATO_CONTEXTOS_PEDIDOS_ETAPA_8.md) e [auditoria de integração](ETAPA_7_7_AUDITORIA_INTEGRACAO_BACKEND_FRONTEND.md) |
| Estágios e identidade | [`DECISAO_CICLO_INSTITUCIONAL_ESTAGIO.md`](DECISAO_CICLO_INSTITUCIONAL_ESTAGIO.md), [`ETAPA_8_CONTRATO_VINCULOS_ACESSO.md`](ETAPA_8_CONTRATO_VINCULOS_ACESSO.md) |
| Impressão e Zebra | [`ETAPA_10_PREPARACAO_ROTULOS_IMPRESSAO_2026-10-27.md`](ETAPA_10_PREPARACAO_ROTULOS_IMPRESSAO_2026-10-27.md) |
| Testes | [`testes.md`](testes.md) e [`REQUISICOES_POSTMAN_LOTES.md`](REQUISICOES_POSTMAN_LOTES.md) **históricos**; usar controllers/DTOs da release antes de reutilizar exemplos |

Os documentos por etapa, `CONTINUIDADE_ETAPA_*`, exemplos JSON, Postman e roteiros antigos foram **preservados**. Adendos de vigência identificam decisões ultrapassadas; jamais reabrir funcionalidades já implementadas apenas porque uma seção histórica usa “pendente” ou “a implementar”.

## Execução e homologação

**DEMO local e autenticação (atualizado em 24/11/2026):** o [guia de demonstração](DEMO_APRESENTACAO_EMBRAPA.md) é a referência atual para iniciar com perfil `demo`, H2 e massa fictícia. A interface usa **Entrar como desenvolvedor** com login curto, sem senha; `GET /api/v1/auth/config` informa `devLoginAtivo: true`. O login normal é corporativo. Instruções antigas de e-mail e senha `123456` são históricas e não servem ao fluxo atual.

A execução local usa `./mvnw` **na raiz do repositório**, Java 17, configuração de ambiente adequada e banco de teste. Para entrada em produção e deploy, conferir `Dockerfile`, `docker-compose.yml`, profiles e permissões institucionais; **não usar secrets nem rede/volume de produção** para simular o sistema.

- **Testes existentes:** JUnit/Spring Boot, segurança, arquitetura ArchUnit, serviços e integração; **não foram executados nesta auditoria documental**.
- **Limite importante:** perfil `test` usa H2, Hibernate `create-drop` e Flyway desabilitado; `mvnw test` verde não demonstra migrations/locks em PostgreSQL.
- **Homologação pendente:** PostgreSQL/Flyway reais em ambiente isolado, autorização entre pessoas da mesma Unidade, métricas de carga, E2E frontend/backend e jornada mobile de estagiários.

## Regras de manutenção e integração

1. **Código backend funcional:** o mantenedor aplica alterações manualmente, salvo autorização explícita; revisão de documentos ocorre somente em branch `collab/*`.
2. **Frontend:** alterações funcionais exigem autorização específica e preservação do padrão visual, ajuda e vídeos.
3. **Integração:** GitHub `collab/*` → GitLab `collab/*` → MR revisado → GitLab `release`; nunca merge direto em `main`, push forçado ou promoção de código incidental.
4. **Antes do MR:** comparar SHAs GitLab/GitHub, conferir diff documental, revisar central de Ajuda e conferir os riscos abertos em [`REGISTRO_PENDENCIAS_REVISAO_SGL_2026-11.md`](REGISTRO_PENDENCIAS_REVISAO_SGL_2026-11.md). O relatório de integridade de links é de 17/11 e deve ser atualizado caso caminhos mudem.

**Estado do documento:** índice atual de documentação, não certificado de testes nem autorização de deploy.
