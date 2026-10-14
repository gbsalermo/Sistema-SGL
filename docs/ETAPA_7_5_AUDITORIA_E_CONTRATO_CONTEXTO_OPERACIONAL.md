# Etapa 7.5 — Auditoria e contrato do contexto operacional

**Data:** 14/10/2026  
**Branch:** `collab/etapa-7-unidades-solucoes-contexto`  
**Estado:** 7.5 iniciada — auditoria técnica executada; contrato proposto a consolidar antes de implementação. A Etapa 7.4 foi encerrada por aceite funcional, sem merge.

## 1. Escopo (sequência aprovada)

A 7.5 estabiliza a origem e a integridade do contexto `Participação do Estagiário → Atividade → SCI → Projeto → Laboratório → Unidade` e a seleção coerente de Projeto/Laboratório para os demais perfis. **A alteração estrutural dos Pedidos convencionais e de Soluções para consumir obrigatoriamente o novo contexto fica na Etapa 8**, respeitando o roadmap. A 7.5 também prepara a visualização de estagiários participantes no hub de Projetos, aperfeiçoamento decidido depois da Etapa 6.

## 2. Inventário verificado

| Área | Implementado | Lacuna 7.5 |
|---|---|---|
| `VinculoEstagioAtividade` | FK para vínculo e Atividade, início/fim e histórico | Falta uma consulta de contexto operacional diretamente por usuário e uma visão por Atividade |
| `VinculoEstagioAtividadeResponseDTO` | Resolve Atividade/SCI/Projeto/Laboratório pelo grafo de domínio; expõe `ativa`, períodos e culturas | `ativa` significa somente `dataFimParticipacao == null`, não garante que o estágio e a hierarquia estejam operacionais hoje |
| `VinculoEstagioAtividadeService` | Cadastra, edita, lista por vínculo e lista participações abertas com tenant; valida hierarquia na criação/edição | Falta contrato read-only que agregue múltiplos vínculos para um solicitante e recalcule elegibilidade para Pedido |
| `EstagiarioService` e interface Estagiários | Os retornos da gestão já exibem todas as participações e contexto Projeto/Laboratório | Falta exposição objetiva no hub Projeto → SCI → Atividade |
| `PedidoRequestDTO` | Recebe `usuarioId`, `laboratorioId`, `projetoId`; `tipo=SOLUCAO` usa a mesma raiz | Não possui `vinculoEstagioAtividadeId` nem contrato de derivação |
| `PedidoService.criar` / `validarConsistenciaPedido` | Checa tenant, vínculo Projeto/Laboratório e exige mesmo laboratório do usuário para não gestores | Para Estagiário com atividades em múltiplos laboratórios, a igualdade a `Usuario.laboratorio` bloqueia o contexto correto |
| Novo Pedido / Nova Solução (Vue) | O Gestor escolhe laboratório; perfil comum usa laboratório da sessão e lista projetos desse laboratório | Para Estagiário, laboratório legado não representa sua participação; para os demais, projeto ainda é escolhido após laboratório e não o determina |
| `ProjetosGestaoView` | Hub hierárquico Projeto → SCI → Atividade e responsável pela Atividade | Ainda não lista os estagiários associados a cada Atividade |

**Referências verificadas diretamente na branch**: `src/main/java/com/sgl/{model/Pedido.java,model/VinculoEstagioAtividade.java,model/Estagiario.java,dto/request/PedidoRequestDTO.java,dto/response/VinculoEstagioAtividadeResponseDTO.java,service/PedidoService.java,service/VinculoEstagioAtividadeService.java,controller/VinculoEstagioAtividadeController.java,repository/VinculoEstagioAtividadeRepository.java}`; frontend `src/modules/{pedidos/views/solicitante/NovoPedidoView.vue,solucoes/views/NovaSolucaoView.vue,estagiarios/views/EstagiariosGestaoView.vue,projetos/views/ProjetosGestaoView.vue}`.

## 3. Contrato proposto por perfil

### Estagiário (perfil ESTAGIARIO)

1. A única referência selecionável é `participacaoId = VinculoEstagioAtividade.publicId`; não enviar Projeto/Laboratório como escolhas arbitrárias.
2. O serviço resolve de forma autoritativa: Participação → Atividade → SCI → Projeto → Laboratório → Unidade. Tenant deve coincidir com Unidade do usuário e do projeto/laboratório.
3. Elegibilidade exige: usuário ativo; vínculo vigente/não finalizado; participação não encerrada; início de participação já ocorrido; início/fim do estágio e período da Atividade/SCI/Projeto compatíveis com a operação; entidades operacionais. Uma participação “aberta” por `dataFimParticipacao == null` pode ser **não elegível** por inatividade ou prazo.
4. Uma participação elegível: pode ser pré-selecionada. Mais de uma: usuário deve escolher qual **Atividade**, mostrando em conjunto Projeto e Laboratório derivados. Nenhuma: bloquear solicitação e explicar a ausência de contexto, sem redirecionar silenciosamente ao laboratório da sessão.
5. A alteração futura do vínculo não pode reescrever o contexto de Pedidos antigos. Persistir referência e campos/snapshot necessários ao histórico na etapa de aplicação do Pedido.

### Técnico / Analista / Pesquisador

1. Quando selecionar Projeto, o Projeto é a fonte do Laboratório, e o backend valida o encadeamento e a Unidade. Não permitir escolher laboratório incompatível separadamente.
2. Quando não houver Projeto (Pedido de Produtos), preservar `Usuario.laboratorio` como contexto institucional-base, se ativo e na mesma Unidade. Para Solução, Projeto continua obrigatório no contrato vigente.
3. O contrato deve explicitar quais Projetos são elegíveis no escopo do usuário; manter isolamento de Unidade. Não assumir que a simples existência de Projeto em uma Unidade equivale a direito de acesso por perfil.

### Gestor / Administrador

Podem escolher laboratório operacional ativo da própria Unidade para uma solicitação em nome próprio, conforme comportamento já validado na 7.4. Havendo Projeto, este deve pertencer ao laboratório informado. Não transferir automaticamente suas permissões para outro tenant.

## 4. Contratos de leitura sugeridos (não implementados)

### `GET /api/v1/contextos-operacionais/usuarios/{usuarioId}`

Serviço de consulta com validação de tenant, titularidade/perfil **antes de produção**, devolvendo array de `ContextoOperacionalResponse`:
- `origem: PARTICIPACAO_ESTAGIO | PROJETO | LABORATORIO_BASE`;
- `participacaoId?`, `atividadeId?`, `atividadeNome?`, `atividadeCodigoSeg?`, `sciId?`, `sciNome?`;
- `projetoId?`, `projetoNome?`, `laboratorioId`, `laboratorioNome`, `unidadeId`;
- `elegivel`, `motivoInelegibilidade?`, `dataInicioVigencia?`, `dataFimVigencia?`;
- opcional `vinculoEstagioId?` para facilitar consulta/diagnóstico.
Não expor atividades de outros usuários no endpoint pessoal.

### `GET /api/v1/atividades/{atividadeId}/participacoes`

Consulta de participantes ativos/históricos **para interface de Gestão**, filtrando Unidade/tenant, com `usuarioId`, `nome`, `participacaoId`, início/fim, `vinculoEstagioId`, e status. Considerar endpoint batch por Projeto para evitar N+1 ao abrir hierarquia com muitas Atividades. A rota é proposta, não existente.

## 5. Fases da 7.5 (ordem recomendada)

| Bloco | Entrega | Critério |
|---|---|---|
| 7.5.1 | Contrato de elegibilidade e apresentação do contexto; revisar filtros de tenant e datas | Mesma participação resolve sempre mesmo Projeto/Laboratório/Unidade; sem combinações livres |
| 7.5.2 | Consultas read-only e testes (usuário e por Atividade/Projeto) | 0, 1 e múltiplas participações; encerramento e tenant cruzado; perfis e períodos |
| 7.5.3 | Hub de Projetos: estagiários por Atividade, atual/histórico | Responsável existente mantido; participantes derivados do vínculo, sem nova associação artificial |
| 7.5.4 | Preparo do frontend para consumir novo contexto e documentar contrato da Etapa 8 | Componentes/serviços compartilháveis, sem quebrar `Novo Pedido` e `Nova Solução` na 7.5 |

**Fora da 7.5:** migration e efetiva associação `Pedido → VinculoEstagioAtividade` (Etapa 8), relatórios consolidados (Etapa 9), ampliação de Últimas movimentações para eventos administrativos (refinamento futuro), segurança/autorização definitiva (pré-produção), margem de erro (extra futuro).

## 6. Testes exigidos

- Estagiário com 0 participações e vínculo institucional: sem contexto operacional elegível, sem liberar pedido por `Usuario.laboratorio`.
- Estagiário com 1 participação elegível: seleção automática e Projeto/Laboratório derivados.
- Estagiário com várias participações, inclusive Projetos e Laboratórios distintos da mesma Unidade: escolha obrigatória e derivações consistentes.
- Participação encerrada, início futuro, vínculo finalizado, usuário inativo, hierarquia inativa/encerrada: não elegível para operação nova; histórico continua consultável.
- Tentativa cross-tenant por usuário/vínculo/atividade: não listar nem resolver contexto alheio.
- Técnico/Pesquisador/Analista: Projeto determina Laboratório; sem Projeto usa contexto-base somente quando permitido.
- Gestor/Admin: laboratório ativo da própria Unidade e Projeto correspondente, mesmo sem laboratório pessoal ativo.
- Hub de Projeto: participantes atuais/históricos da Atividade, sem duplicação por várias participações; responsivo Light/Dark.
- Regressão: seis cenários principais de Soluções, Pedidos convencionais, distribuição de indicadores da 7.4, contexto dos Estagiários da Etapa 6.

## 7. Risco de segurança e versionamento

`X-SGL-Unidade-Id` é temporariamente controlado pelo cliente em DEV e `SecurityConfig` usa permissividade transitória. Testes de recorte por tenant e navegação no frontend **não** provam autenticação/autorização real. O contrato de contexto deve prever que identidade e Unidade virão da sessão autenticada na versão de produção.

**Não executar migrations nem alterar o Pedido existente apenas com base nesta auditoria.** Preservar os fluxos 7.4 homologados. Sem merge; iniciar implementação técnica 7.5.1/7.5.2 depois de fixar a semântica de elegibilidade e o contrato de leitura.


---

## 8. 7.5.1 — Política de elegibilidade implementada (14/10/2026)

**Código entregue nesta branch:** `src/main/java/com/sgl/service/ElegibilidadeContextoOperacional.java`. Classe final/pura (sem consulta a banco, sem Spring e sem efeito colateral), método:

```java
Resultado resultado = ElegibilidadeContextoOperacional.avaliar(
    participacao, dataReferenciaServidor, unidadeSolicitanteValidada);
```

`Resultado` é `record(boolean elegivel, Motivo motivo)`, com código de motivo estável para clientes/serviços. A política **não modifica** `VinculoEstagioAtividade.ativa`, status institucional, períodos, associação ou Pedido. `ativa` do DTO atual continua representando apenas participação não encerrada; `elegivel` é um conceito adicional para **criar novas solicitações**. A data deve ser obtida pelo backend, nunca escolhida pelo cliente para contornar vigência.

### Ordem de verificações do contrato

| Checagem | Motivo do bloqueio |
|---|---|
| Grafo, referências/IDs obrigatórios e datas de início do vínculo/participação/SCI/Atividade | `CONTEXTO_INCOMPLETO` |
| Perfil do usuário = `ESTAGIARIO` | `PERFIL_INCOMPATIVEL` |
| Usuário ativo | `USUARIO_INATIVO` |
| Vínculo não `FINALIZADO`, sem data fim efetiva, situação institucional preenchida | `VINCULO_FINALIZADO` |
| Hoje dentro de início e fim previsto do vínculo (quando conhecido) | `VINCULO_FORA_DA_VIGENCIA` |
| Participação sem fim efetivo | `PARTICIPACAO_ENCERRADA` |
| Hoje a partir do início da participação | `PARTICIPACAO_FORA_DA_VIGENCIA` |
| Unidade de usuário = Unidade do Laboratório derivado = tenant informado e não nulos | `UNIDADE_DIVERGENTE` |
| `ativo=true` em Laboratório, Projeto, SCI e Atividade | `HIERARQUIA_INATIVA` |
| Status `ATIVO` e situação de execução diferente de `EXECUCAO_CANCELADA` nos três níveis Projeto/SCI/Atividade | `HIERARQUIA_NAO_OPERACIONAL` |
| Data do servidor dentro dos períodos informados nos três níveis (fim nulo = sem fim cadastrado; Projeto pode ter início nulo) | `HIERARQUIA_FORA_DA_VIGENCIA` |

Datas-limite são inclusivas. `SituacaoExecucaoProjeto.EM_ANDAMENTO_ATRASADO` **não** bloqueia por si só, mas período encerrado bloqueia. `SituacaoEstagio.PRORROGADO` permanece válido quando dentro do novo prazo. Não confundir prazo previsto com situação institucional finalizada. Não usar `Usuario.laboratorio` do Estagiário na derivação.

**O comportamento de 7.5.1 exige confirmação de domínio caso o cliente institucional permita atividade atrasada fora do período sem prorrogação; nesse caso rever explicitamente a política antes de ligar ao fluxo de Pedidos.** A aprovação dessa regra não altera históricos passados.

### Testes preparados, ainda sem log Maven

`src/test/java/com/sgl/service/ElegibilidadeContextoOperacionalTest.java` cobre sucesso, prorrogação, múltiplas participações em laboratórios distintos da mesma Unidade, usuário inativo, perfil incompatível, fim institucional, início futuro/prazo expirado, participação encerrada/futura, tenant e Unidade divergentes, cada nível hierárquico inativo, status e cancelamento de execução, períodos da hierarquia, limites inclusivos e dados incompletos.

**Estado:** 7.5.1 implementada no código, revisão estática realizada; `mvn test` e inicialização **não executados aqui**. Não existe endpoint novo nesta fase. **7.5.2 é a próxima frente:** criar consulta read-only por usuário e por Atividade com filtros reais, DTO próprio (`elegivel`/`motivo`), cache/carga adequada, testes de acesso/tenant e ausência de N+1; 7.5.3 adapta o hub frontend. **Sem migration, sem alteração de Pedido/Solução, sem merge.**


---

## 9. 7.5.2 — Consultas e APIs de contexto operacional publicadas (14/10/2026)

**Estado:** implementação backend publicada na branch; **JUnit da 7.5.2, teste do boot e teste HTTP ainda pendentes de execução pelo responsável**.

### Endpoints read-only (NOVOS)

| Método | Rota | Resultado |
|---|---|---|
| GET | `/api/v1/contextos-operacionais/usuarios/{usuarioId}` | Todas as participações do Estagiário indicado (elegíveis e inelegíveis, incluindo histórico), ordenadas por início mais recente |
| GET | `/api/v1/contextos-operacionais/atividades/{atividadeId}/participacoes` | Participações atuais e históricas vinculadas à Atividade, com elegibilidade calculada |
| GET | `/api/v1/contextos-operacionais/projetos/{projetoId}/participacoes` | Participações de todas as Atividades do Projeto, em **consulta única de lote** para o hub Projetos |

Todos exigem Unidade corrente e cruzam Unidade de origem (Estagiário) e Unidade do destino (Projeto/Laboratório) diretamente no repositório. Se o usuário, Atividade ou Projeto não pertence à Unidade, a operação falha antes da consulta de participantes. **Estagiário com zero participações válidas ou sem histórico** retorna lista vazia, não `Usuario.laboratorio`.

### DTO de resposta

`ContextoOperacionalResponseDTO` expõe: `origem = PARTICIPACAO_ESTAGIO`, `participacaoId`, `vinculoEstagioId`, `usuarioId`, `usuarioNome`, `atividadeId`, `atividadeNome`, `atividadeCodigoSeg`, `sciId`, `sciNome`, `projetoId`, `projetoNome`, `laboratorioId`, `laboratorioNome`, `unidadeId`, `dataInicioParticipacao`, `dataFimParticipacao`, `aberta`, `elegivel`, `motivoInelegibilidade`. **`aberta` não significa `elegivel`**. `motivoInelegibilidade` é null se elegível, senão é o enum estável da política.

### Arquivos alterados/adicionados

- `repository/VinculoEstagioAtividadeRepository.java`: 3 JPQL queries filtradas por usuário/destino/tenant + `@EntityGraph` para leitura da hierarquia sem consultas por participação.
- `dto/response/ContextoOperacionalResponseDTO.java`: DTO serializável/read-only de contexto.
- `service/ContextoOperacionalConsultaService.java`: valida recurso no tenant, aplica `ElegibilidadeContextoOperacional.avaliar(..., LocalDate.now(), unidadeId)` em uma data de referência obtida no servidor.
- `controller/ContextoOperacionalController.java`: exposição em REST.
- `test/service/ContextoOperacionalConsultaServiceTest.java`: sucesso do Estagiário, participante encerrado/histórico, lote por Projeto, 0 participações, perfil não Estagiário, cross-tenant, ausência de tenant e contexto recusado.

**Comandos de homologação local pelo responsável:**
```bash
mvn -Dtest=ContextoOperacionalConsultaServiceTest,ElegibilidadeContextoOperacionalTest test
mvn test
```
Executar boot para validação de inicialização de Spring Data JPA (`@Query` / `@EntityGraph`), além de smoke GET via Swagger/Postman. Não marcar como verde sem log local.

**Limite obrigatório de segurança:** como a identidade/autorização definitiva ainda não existe, a URL com `usuarioId` permite consultas de outros usuários da mesma Unidade no estado DEV, e a Unidade vem do header controlado pelo cliente. O filtro de tenant evita divergências nas queries **mas não substitui autorização real**. Este contrato precisa de controle de titularidade para consulta pessoal e perfil Gestor/Admin para listar participantes em produção. Não ligar estas APIs públicas a um ambiente de produção sem essa etapa de segurança. Nunca tratar o `usuarioId` da URL como identidade autenticada.

**Não houve alteração de migrations, fluxos de criação de Pedido/Solução ou movimentações de estoque.** 7.5.3 (frontend do hub de Projetos) inicia após validação local dos testes e boot da 7.5.2. Sem merge.


## 10. Validação da 7.5.2 e início do hub frontend — 14/10/2026

Após o pedido de executar os testes 7.5.2 e subir o backend, o responsável confirmou **"subiu verde"**. Estado da 7.5.2: **validada localmente pelo responsável**; a evidência é o relato do responsável, não logs Maven ou HTTP anexados. O backend permanece sem alterações funcionais adicionais nesta transição.

O frontend iniciou 7.5.3 usando a consulta de Projeto em lote, exibindo participantes atuais/históricos por Atividade, mantendo nome do responsável. Nenhuma relação artificial criada, nenhuma migration; frontend aguarda `npm run build` e validação visual. Aplicação estrutural ao Pedido permanece exclusivamente na Etapa 8.
