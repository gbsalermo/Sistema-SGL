# Handoff da 7.5 para a Etapa 8 — Contexto operacional de Pedidos e Soluções

**Atualizado em:** 15/10/2026  
**Branch:** `collab/etapa-7-unidades-solucoes-contexto`  
**Estado:** contrato preparado; **NÃO implementado no fluxo de criação de Pedidos**.

## Objetivo e separação de responsabilidade

A Etapa 7.5 implementou as bases de leitura do contexto do Estagiário: política pura `ElegibilidadeContextoOperacional`, consultas tenant-scoped e participantes por Atividade/Projeto (hub). A Etapa 8 aplicará essas bases **a novas solicitações de Produtos e Soluções**, sem transformar Solução em Produto artificial.

`VinculoEstagioAtividade.publicId` é a referência operacional escolhida pelo Estagiário. O encadeamento, **resolvido exclusivamente pelo backend**, é:

```text
Participação → Atividade → SCI → Projeto → Laboratório → Unidade
```

**Importante:** uma participação *aberta* não é necessariamente *elegível*. A API retorna ambos; a criação do Pedido deverá recalcular a elegibilidade no servidor.

## Contrato de escrita proposto para a Etapa 8

### Estagiário

- Nova propriedade canônica do request de Pedido: **`participacaoId`** (UUID `VinculoEstagioAtividade.publicId`), obrigatória para perfil `ESTAGIARIO`.
- A consulta `GET /api/v1/contextos-operacionais/usuarios/{usuarioId}` devolve todas as participações do Estagiário (atuais, futuras e históricas); o cliente deve oferecer apenas as **elegíveis** para novas solicitações.
- Zero contextos elegíveis: bloquear o envio, explicar o motivo; **não** usar `Usuario.laboratorio` como fallback.
- Um contexto elegível: permitir pré-seleção.
- Vários contextos elegíveis: exigir escolha explícita da **participação/Atividade**. Mesmo duas participações no mesmo Projeto são seleções distintas.
- `projetoId`, `laboratorioId`, `unidadeId` e `atividadeId` exibidos na UI são **derivados/informativos**, jamais autoridade para o servidor. O backend deve rejeitar conflito se receber campos legados divergentes; idealmente o cliente não os envia para Estagiário quando o novo contrato estiver ativo.
- Ao criar, verificar: participação pertence ao próprio usuário, status e vigência de Usuário/Vínculo/Participação/Projeto/SCI/Atividade/Laboratório, tenant e elegibilidade **na data da operação**. Não confiar em um `elegivel` obtido anteriormente pelo navegador.
- Preservar histórico: persistir a referência da participação e o snapshot do contexto de origem necessário (IDs/nome ou relação com Projeto/Atividade e Laboratório da data da solicitação). Hoje `VinculoEstagioAtividadeService.atualizar` permite corrigir a Atividade de uma participação aberta; Pedido antigo **não pode mudar de Projeto/Laboratório/Atividade retroativamente** se isso ocorrer.
- Migração Flyway para FK de participação em `pedido` (nome/coluna a confirmar na implementação); nula para Pedidos legados e perfis sem participação. Não exigir backfill especulativo.

### Técnico, Analista, Pesquisador

- **Quando há Projeto:** selecioná-lo determina o Laboratório; Unidade segue do Projeto, validada contra usuário/tenant.
- **Pedido de Produtos sem Projeto:** preservar laboratório-base do Usuário quando permitido pelas regras institucionais, se ativo na mesma Unidade.
- **Solução:** continua exigindo Projeto segundo fluxo já homologado. Não introduzir seleção contraditória Projeto/Laboratório.
- A política de quais Projetos podem ser selecionados por perfil precisa ser validada com o responsável institucional antes de ampliar a visibilidade atual.

### Gestor e Administrador

- Continuam podendo atuar sobre Laboratórios da própria Unidade, no escopo permitido pela gestão.
- Projeto, quando presente, precisa pertencer ao Laboratório correspondente. Nunca permitir Unidade diferente.

## Contrato de leitura já presente (Etapa 7.5.2)

| API | Finalidade | Estado |
|---|---|---|
| `GET /api/v1/contextos-operacionais/usuarios/{usuarioId}` | Contextos do Estagiário | Já implementada, validação JUnit/boot reportada verde |
| `GET /api/v1/contextos-operacionais/atividades/{atividadeId}/participacoes` | Participantes atuais e históricos da Atividade | Já implementada |
| `GET /api/v1/contextos-operacionais/projetos/{projetoId}/participacoes` | Consulta em lote para hub Projeto | Já implementada e visualizada na 7.5.3 |

Essas APIs em DEV validam Unidade de origem e destino, mas **não autenticam o usuário titular**: o header `X-SGL-Unidade-Id` é temporariamente fornecido pelo cliente e `SecurityConfig` é permissiva. Antes da exposição externa/produção, exigir identidade autenticada no servidor e verificar:
- endpoint pessoal: titular ou Gestor/Admin autorizado;
- listagens por Atividade/Projeto: papéis de Gestão permitidos;
- isolamento entre Unidades validado por testes de API, não somente pela UI.

## Preparação frontend feita na 7.5.4

- `src/modules/operacao/types/contextoOperacional.ts`: tipo central de DTO e funções puras `prepararSelecaoContextoEstagiario(contextos, unidadeId)` e `resolverContextoEstagiario(opcoes, participacaoId, unidadeId)`.
- `src/modules/operacao/services/contextoOperacionalService.ts`: serviço de leitura de contextos por usuário, por Atividade e por Projeto. **Ainda não chamado pelos formulários**.
- `src/modules/projetos/services/projetosService.ts`: mantém o método existente para o hub, delegando ao serviço comum. `src/modules/projetos/types/projetos.ts` reexporta o tipo compartilhado.
- A camada de seleção da UI filtra `elegivel=true`, exige IDs completos e Unidade coerente; para 0 retorna `SEM_CONTEXTO_ELEGIVEL`, para 1 `PRE_SELECIONAVEL`, para N `ESCOLHA_OBRIGATORIA`. É somente conveniência de interface, **não autorização**.
- Nenhum payload, controle ou submit de `NovoPedidoView.vue` ou `NovaSolucaoView.vue` foi modificado na 7.5.4.

## Sequência de implementação recomendada na Etapa 8

1. Revisar contrato de elegibilidade e política institucional de Projeto/Laboratório por perfil.
2. Implementar validação autoritativa da participação do Estagiário no domínio do Pedido; resolver e persistir contexto/snapshot, migrar banco preservando Pedidos legados.
3. Testar 0/1/N participações, início futuro, encerradas, expirada, hierarquia inativa, mudança posterior da Atividade, Projeto incompatível, cross-tenant e solicitante falso. Aprovação, baixa, preparo, rejeição, cancelamento e entrega não podem alterar essa escolha histórica.
4. Adaptar **os dois** formulários frontend para compartilhar a escolha de contexto; selecionar Participação/Atividade antes de exibir Projeto/Laboratório derivados. Um Projeto escolhido por outro perfil determina Laboratório.
5. Rodar testes automatizados e regressão de Pedidos de Produtos + Soluções, com perfis e IB/IQ; assegurar não duplicar baixa física ou alterar snapshots de Soluções.

## Exclusões e pendências

- **7.6/7.7:** massa DEV/DEMO, builds finais no HEAD, regressão, responsividade, Light/Dark e QA de perfis/isolamento. O relato de testes verdes anteriores não valida automaticamente mudanças futuras.
- **Segurança de pré-produção:** autenticação/autorização real, auditoria, matriz de permissões e fontes autoritativas da identidade/Unidade.
- **Nenhum merge** foi realizado ou está autorizado pelo presente documento.


## Atualização de contrato vigente — fechamento funcional da Etapa 8 (23/10/2026)

**Este adendo substitui, para o MVP atual, as afirmações históricas acima de que Soluções sempre exigem Projeto.** O documento original representa o handoff da Etapa 7.5, não a implementação final.

- **Pedidos de Produtos, Soluções e Resíduos:** Projeto é opcional para os perfis habilitados, condicionado à autorização do laboratório de origem.
- **Técnico, Analista e Pesquisador:** podem utilizar laboratório-base ou vínculo operacional adicional vigente. Vínculos específicos de Projeto, SCI ou Atividade autorizam o laboratório correspondente; vínculos em SCI/Atividade também derivam o Projeto. Não confundir a herança de contexto com concessão automática de novos vínculos formais.
- **Projeto selecionado, sem vínculo específico do usuário:** quando pertence a laboratório autorizado, o registro é admitido e mantém indicador/snapshot de ausência de vínculo de Projeto, exibido para confirmação da Gestão no fluxo próprio. Resíduos seguem a confirmação no recebimento.
- **Estagiário:** permanece dependente de participação elegível validada no backend; origem histórica é preservada.
- **Banco:** migrations V40–V44 da Etapa 8; histórico de vínculos e indicadores não devem ser recalculados retroativamente com base nas permissões atuais.
- **Validação:** blocos funcionais 1–4 reportados aprovados em DEV; Flyway validou 44 migrations; suíte ArchUnit 5/5 verde; frontend compilou após correções de tipagem.
- **Fora deste aceite:** autenticação institucional e testes globais de carga/arquitetura/E2E da Etapa 12. Não há autorização de merge implícita.
