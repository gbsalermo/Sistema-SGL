# Governança documental — SGL Backend

**Estado:** organização e resolução de redundância de autoridade em 12/11/2026. **Não** equivale a revisão de todos os métodos, endpoints ou testes. Base: `release` espelhada; trabalho em `collab/docs-release-estado-2026-11`.

## Precedência e responsabilidades

1. **Código executável e banco:** controllers, DTOs, services, repositories, models, segurança, testes e migrations da `release` prevalecem como evidência do comportamento implementado. Isso não substitui validação de requisitos e permissões.
2. **Dossiê técnico:** `DOSSIE_PROJETO_SGL.md` explica por domínio **onde mexer** e quais dependências conferir.
3. **Arquitetura:** `GUIA_ESTRUTURAL.md` define camadas e convenções; não deve reproduzir o mapa completo do dossiê.
4. **Fluxo de negócio:** `FLUXO_DO_SISTEMA.md` explica jornadas; regras de autorização e contratos detalhados pertencem aos serviços/DTOs e documentos especializados conferidos.
5. **API:** `ENDPOINTS_INTERNOS.md` é catálogo de orientação; controllers/DTOs/OpenAPI verificada são fonte dos endpoints reais. `JSON_EXEMPLOS.md`, `REQUISICOES_POSTMAN_LOTES.md` e `API_AUDITORIA_PRE_SWAGGER.md` são exemplos/evidências, não contratos independentes.
6. **Relatórios:** `RELATORIOS.md` descreve domínio; `EXPORTACAO_RELATORIOS.md` especializa formatos; `ETAPA_9_*.md` registra decisões históricas. Não duplicar filtros/DTOs sem verificar código.
7. **Estagiários e pedidos:** `DECISAO_CICLO_INSTITUCIONAL_ESTAGIO.md`, `DECISAO_CONTEXTO_OPERACIONAL_ESTAGIARIO_PEDIDOS.md`, `CONTRATO_CONTEXTOS_PEDIDOS_ETAPA_8.md` e `ETAPA_8_CONTRATO_VINCULOS_ACESSO.md` preservam decisões de datas distintas; divergência deve ser confrontada com `PedidoService`, `VinculoEstagioService`, elegibilidade e migrations V47/V48. **Nenhum desses arquivos isoladamente é contrato homologado da release.**
8. **Estoque:** `ETAPA_7_MODELO_ESTOQUE.md` e auditorias 7.5–7.7 documentam decisões de FEFO/FIFO, lotes, fracionamento e integração; o comportamento atual depende dos services e migrations da `release`.
9. **Resíduos:** `MODULO_RESIDUOS.md` é visão de domínio; mudanças posteriores, inclusive rótulos, devem ser verificadas nos services/DTOs.
10. **Git:** `SINCRONIZACAO_GITLAB_GITHUB.md` é o único documento de procedimento de branches; GitLab `release` canônica, GitHub `release` espelho, `main` independente, MRs manuais.
11. **Continuidade:** `../CONTINUIDADE.md` é o checkpoint atual; `CONTINUIDADE_ETAPA_*`, `VALIDACAO_ETAPA_*`, `PENDENCIAS_POS_PROTOTIPO.md` e `PLANO_PRE_PRODUCAO.md` são históricos/planos com data, não status de produção.
12. **Qualidade:** `ETAPA_12_PLANO_QUALIDADE_ARQUITETURA_CARGA_E2E.md` é **plano**, não prova de testes realizados; `docs/testes.md` e relatórios de CI devem fornecer evidência real.
13. **Índice:** `docs/README.md` deve somente apontar para fontes; revisão editorial final fica para depois da auditoria funcional.

## Matriz de redundâncias e decisão editorial

| Sobreposição | Fonte de autoridade | Tratamento |
|---|---|---|
| Checkpoints de setembro/outubro vs estado novembro | `../CONTINUIDADE.md` | Preservar históricos; marcar vigência e não repetir “etapa atual” |
| Camadas e mapa de arquivos | `GUIA_ESTRUTURAL.md` para arquitetura; `DOSSIE_PROJETO_SGL.md` para caminhos | Referências cruzadas, sem cópia integral |
| Endpoints, exemplos JSON e Postman | Controllers/DTOs reais; `ENDPOINTS_INTERNOS.md` como catálogo | Exemplos são ilustrações até verificação |
| Estágio, vínculos, autorização e contexto de pedidos | Implementação `release` e decisões ratificadas | **Conflitos semânticos pendentes**; não fundir regras sem testar perfis |
| Estoque, pedidos e fracionamento | Serviços de estoque/pedidos e migrations | Manter auditorias como evidências, sem duplicar regra vigente |
| Relatórios e exportações | Services/DTOs; `RELATORIOS.md` e `EXPORTACAO_RELATORIOS.md` com escopos distintos | Etapa 9 é histórico |
| Planejamento de qualidade e testes | Plano separado de evidências executadas | Não converter intenção em homologação |
| Sincronização Git antiga `main` vs atual `release` | `SINCRONIZACAO_GITLAB_GITHUB.md`, atualização novembro | Trecho antigo é histórico, não instrução |
| Rótulos etapa 10 em frontend e backend | Código e contrato do módulo; docs datados como decisões | Checar gatilhos, impressão e campos antes de consolidar |

## Regra para mudanças futuras

Documentar cada alteração no **arquivo proprietário**, ligar a decisão à implementação/teste, manter referências nos demais. Não apagar registros históricos, não alterar backend funcional nesta tarefa e não aprovar MR documental antes de validar contratos e referências cruzadas.

**Resultado:** organização de responsabilidade documental concluída; revisão semântica, segurança, testes e homologação ainda pendentes.
