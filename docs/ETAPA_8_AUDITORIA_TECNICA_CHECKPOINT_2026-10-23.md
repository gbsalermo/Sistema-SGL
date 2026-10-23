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
