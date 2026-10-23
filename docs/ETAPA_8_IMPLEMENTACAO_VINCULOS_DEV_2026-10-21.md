# Etapa 8 — Implementação DEV dos vínculos operacionais (21/10/2026)

**Branch:** `collab/etapa-8-contextos-pedidos`  
**Estado:** código enviado ao GitHub; **não homologado**. Não houve merge nem sincronização GitLab. Login institucional não foi implementado conforme decisão do responsável.

## Escopo entregue em código
- V42: `vinculos_operacionais_usuario`, com FK para Usuário, Laboratório e opcionalmente Projeto/SCI/Atividade, datas, histórico de encerramento e observação.
- `VinculoOperacionalUsuarioService` e controller DEV/DEMO: listar, criar, encerrar, definir Laboratório-base. Estagiários continuam usando `VinculoEstagioAtividade`.
- `PedidoService`: Técnico, Analista e Pesquisador podem operar em laboratório-base ou vínculo adicional vigente. Projeto de laboratório autorizado sem vínculo direto mantém pedido com sinalização `projetoSemVinculoDireto` (V43). Estoque, aprovação, FEFO/FIFO inalterados.
- `ResiduoService`: aceita laboratório adicional autorizado para funcionários efetivos; estagiários continuam vinculados à atividade elegível.
- Gestão: sinalização de projeto não vinculado em Pedidos/Soluções com confirmação explícita antes da aprovação.
- Administração > Cadastros: configuração do laboratório-base e vínculos hierárquicos com SCI/Atividade derivados. Gestor usa rota específica sem acesso à alteração global de perfis; Administrador também mantém aba Permissões.
- Consulta DEV de pedidos dos orientandos para Analista/Pesquisador, apenas leitura e limitada a laboratórios autorizados.
- Login DEV mantido como antes. Criação automática no primeiro acesso, SSO e autorização institucional são atividades futuras, proibido expor endpoints DEV em produção.
- Testes unitários adicionados para vínculo-base, tenant, vigência, duplicidade de escopo? **Não foi adicionada cobertura específica de duplicidade**, encerramento e perfil; testes existentes ajustados para nova dependência.

## Riscos e pendências honestas
1. Nenhuma compilação Maven ou build Vue foi executada nesta sessão; ambiente local de execução e rede Git não estão disponíveis.
2. Migrações V42/V43 **não aplicadas**. Executar Flyway sobre cópia DEV do PostgreSQL; examinar versão e esquema antes.
3. Ainda não há identidade autenticada no backend. Endpoints de edição recém-criados são limitados por profile DEV/DEMO, mas DEV **não é ambiente seguro** contra usuário malicioso. Não expor externamente.
4. Dados institucionais de primeiro login continuam simulados com usuários cadastrados previamente.
5. Consulta de supervisão atual usa o orientador registrado no vínculo de estágio e o laboratório autorizado no momento da consulta; não cria histórico separado de atribuições de supervisor.
6. Relatórios antigos de pessoas por laboratório podem continuar refletindo somente o laboratório-base. Devem ser testados e adaptados antes da homologação completa.
7. Não executados testes de arquitetura, carga, fluxos, Light/Dark, mobile, troca de tenant e regressão de estoque.

## Aceite DEV recomendado
1. Rodar `mvn clean test` e compilação do backend. Validar V42/V43 com profile `dev`, conferir erro de inicialização.
2. Rodar `npm run build` e testes frontend existentes.
3. Em Cadastros: selecionar um Analista de teste; atribuir laboratório-base; criar vínculo em outro laboratório; encerrar e confirmar revogação imediata.
4. Associar usuário a Projeto/SCI/Atividade e confirmar laboratório/projeto derivados, rejeição de referências de outra Unidade e de hierarquias incompatíveis.
5. Entrar no DEV como Analista: Pedido de Materiais no laboratório-base e adicional; pedido sem Projeto; com Projeto sem vínculo direto; verificar aviso e flag no backend.
6. Na Gestão: validar alerta e confirmação de ciência antes de aprovar Pedido e Solução sem vínculo direto.
7. Verificar que laboratórios sem autorização são rejeitados pelo backend ainda que o payload seja forjado.
8. Estagiário: repetir 0/1/N participações em Pedidos, Soluções e Resíduos, garantindo ausência de regressão.
9. Orientador Analista/Pesquisador: conferir somente pedidos de seus orientandos que pertençam a laboratório autorizado.
10. Validar Resíduos e relatórios, FEFO/FIFO, baixa física, isolamento tenant, ausência de acesso administrativo para perfis comuns e compatibilidade DEV/DEMO.

## Fluxo futuro
SSO institucional autentica identidade estável -> SGL localiza ou provisiona usuário pendente -> Gestor/Admin autorizado concede Laboratório-base e vínculos -> permissões operacionais são aplicadas pelo servidor. **Não implementar esse fluxo de autenticação nesta Etapa 8.**


## Estado posterior validado — 23/10/2026 (adendo)
O texto acima documenta a situação da data original, **não a situação final da Etapa 8**. Os blocos 1–4 (Vínculos, Pedidos, Soluções e Resíduos) foram homologados manualmente no DEV. Migrations V40–V44 validadas em PostgreSQL; build frontend e ArchUnit (5/5) aprovados. Projeto é opcional nos fluxos atuais de Pedidos, Soluções e Resíduos para perfis elegíveis. O escopo de identidade institucional permanece separado. Conferir `docs/AUDITORIA_COBERTURA_DADOS_MVP_ETAPA_8_2026-10-23.md` antes do fechamento: unidades/usuários/novos estagiários dependem de provisionamento/seed. Não há merge autorizado, nem sincronização GitLab comprovada.
