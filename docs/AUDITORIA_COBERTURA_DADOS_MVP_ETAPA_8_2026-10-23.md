# SGL — Auditoria de documentação e cobertura de entrada de dados do MVP
**Checkpoint:** 23/10/2026 · **Branches:** `collab/etapa-8-contextos-pedidos` (backend e frontend) · **Situação:** inspeção de código e documentação; não constitui teste E2E adicional nem autorização de merge.

## Escopo e fontes examinadas

- Backend: `CONTINUIDADE.md`, `docs/PLANO_PRE_PRODUCAO.md`, `docs/FLUXO_DO_SISTEMA.md`, `docs/GUIA_ESTRUTURAL.md`, `docs/CONTRATO_CONTEXTOS_PEDIDOS_ETAPA_8.md`, `docs/ETAPA_8_CONTRATO_VINCULOS_ACESSO.md`, `docs/ETAPA_8_AUDITORIA_TECNICA_CHECKPOINT_2026-10-23.md`, `docs/ETAPA_8_IMPLEMENTACAO_VINCULOS_DEV_2026-10-21.md`, `docs/MODULO_RESIDUOS.md`, `docs/RELATORIOS.md`, `docs/ETAPA_12_PLANO_QUALIDADE_ARQUITETURA_CARGA_E2E.md` e decisões históricas.
- Frontend: `CONTINUIDADE.md`, `docs/INVENTARIO_TELAS.md`, `docs/ETAPA_CADASTROS_ADMIN.md`, `docs/ESTRUTURA_FRONTEND.md`, `docs/ROADMAP_INTERFACE_GESTAO.md`, `docs/FLUXOS_NAVEGACAO.md`, padrões visuais e docs da Etapa 8.
- Código consultado: DTOs `UsuarioRequestDTO`, `LaboratorioRequestDTO`, `ProjetoRequestDTO`, `SciRequestDTO`, `AtividadeRequestDTO`, `EstagiarioRequestDTO`, `CriarResiduoRequestDTO`, `PedidoRequestDTO`; Controllers respectivos; componentes `CadastrosAdminView.vue`, `ProjetosGestaoView.vue`, `EstagiariosGestaoView.vue`, `VinculosUsuariosCadastro.vue`, `VinculosOperacionaisGestaoView.vue` e formulários de Pedidos, Soluções e Resíduos.
- Procedimento: confrontar cada dado requerido com **fonte de origem**, rota de criação/edição, payload e papel que possui acesso. Arquivo de migration/seed **não** é considerado substituto permanente de entrada pela UI.

## Inventário: dados operacionais e caminho para mantê-los

| Recurso/dado | Contrato / origem | Entrada/gestão no MVP | Diagnóstico |
|---|---|---|---|
| Unidade | Institucional; `UsuarioRequestDTO.unidadeId`, `LaboratorioRequestDTO.unidadeId` | Referência nas telas; seed no DEV | **Dependência institucional deliberada**: sem CRUD normal de Unidade; se uma instalação sem dados institucionais não dispuser de Unidades, não inicia operação por UI |
| Usuário (nome, e-mail, Unidade, perfil) | Futuro login/provisionamento institucional; dados de teste via seed/SQL; `UsuarioController` tem API de criação | Cadastros lista existentes e edita perfil; Vínculos altera base/associações; **não há formulário comum de criação de usuário** | **Lacuna de inicialização de ambiente isolado**, não bug de campo oculto no fluxo atual: sem provisionamento DEV ou integração institucional, Gestor não consegue cadastrar uma pessoa nova pela UI |
| Laboratório | Unidade e nome obrigatórios; responsável opcional | Administração > Cadastros: criar/editar; responsáveis elegíveis listados | Entrada disponível, mas atribuição de responsável depende de usuário preexistente |
| Projeto | Laboratório e nome obrigatórios; datas, responsável textual e Código SEG conforme regra | Administração > Cadastros: criar/editar/ativar; projetos associados a laboratório | Entrada disponível |
| SCI | Projeto, Código SEG, nome e início obrigatórios | Gestão de Projetos > SCI > criar/editar | Entrada disponível |
| Atividade | SCI, Código SEG, nome e início obrigatórios | Gestão de Projetos > Atividade > criar/editar | Entrada disponível |
| Produto/catálogo | Nome, código, unidade, risco e campos condicionais | Administração > Cadastros: criar/editar; campos de risco, fiscalizações e segurança | Entrada disponível; validação condicional existe |
| Estoque físico | Unidade, produto, lote, recipiente, apresentação, fracionável e saldo | Gestão > Estoque: entrada/lote; saldos derivados das operações | Entrada disponível; não cadastrar saldo fictício manualmente fora do caso de uso |
| Modelo de Solução e Resíduo | Catálogos operacionais | Administração > Cadastros > Resíduos e Soluções | Entrada disponível |
| Classe de resíduo e local-base | Cadastro por Unidade | Administração > Cadastros: criar/editar | Entrada disponível; seeds DEV são conveniência, não fonte exclusiva |
| Resíduo operacional | Descrição, processo, estado físico, tratamento, recipiente, quantidade, risco, componente, classe e segurança obrigatórios | Solicitante > Informar resíduo; classificação/armazenagem pela Gestão | Formulário de operação disponível; os valores que não são simples texto têm seleção/cadastro em telas próprias quando previsto |
| Laboratório-base, Projeto, SCI e Atividade do usuário | Gestão local dos vínculos; herança ascendente no backend | Gestão/Admin > Vínculos operacionais | Entrada disponível para usuário **já provisionado** |
| Estagiário (novo registro/titular) | `EstagiarioRequestDTO` exige `usuarioId`, Laboratório, datas, bolsa, orientador, Atividade, Formação; outras dependências condicionais | Drawer de Estagiários administra registros, vínculos e participações **existentes**; Curso e Cultura podem ser criados no contexto | **Dependência de provisionamento/seed**: não foi localizado formulário de cadastro inicial completo de Estagiário na View examinada. Fluxo inicial não é autossuficiente numa instância vazia |
| Curso de Estagiário | Catálogo local | Drawer de Estagiários permite criar Curso | Entrada disponível |
| Cultura de participação | Catálogo local | Drawer de Estagiários permite criar Cultura | Entrada disponível |
| Orientador | Usuário elegível por Unidade/projeto | Drawer de Estagiários seleciona usuário existente | Entrada disponível **após existir o usuário** |
| Referência de bolsa, vigência e observações | Dados de vínculo, com integração institucional futura por campo | Drawer de Estagiários (editar/prorrogar/bolsa/observações) | Entrada disponível para registros existentes; preservar precedência institucional futura |

### Lacunas e riscos que requerem decisão, por prioridade

**L1 — Bloqueio de bootstrap, dependente do ambiente (confirmado):** no cenário *sem massa DEV e sem identidade institucional*, a UI não cria Unidade, Usuário ou novo Estagiário completo. Isso decorre do contrato deliberado `docs/ETAPA_CADASTROS_ADMIN.md` e do `CadastrosAdminView.vue`, não de uma falha encontrada na criação de Pedido. **Não criar cadastro paralelo de identidade sem decisão do responsável.** Definir uma rotina controlada e documentada de provisionamento de homologação, ou marcar o bloqueio como requisito de entrega da integração corporativa.

**L2 — Dependência de pessoas preexistentes (confirmado):** o responsável de Laboratório e orientador de Estagiário só podem ser escolhidos dentre Usuários existentes. A UI não gera automaticamente Usuário faltante. Garantir que dados institucionais cheguem antes de exigir essas referências; orientar a Gestão quando a lista estiver vazia. Responsável de Projeto atualmente é texto livre e não exige criação de conta.

**L3 — Necessidade de smoke test de instalação nova (pendente de execução):** validar uma base **isolada e sintética**, sem inicializadores DEV, com sequência Unidade/Usuário provisionados → Laboratório → Projeto → SCI → Atividade → vínculos → catálogo/estoque → Pedido/Solução/Resíduo. Testes manuais anteriores em base pré-populada não demonstram esse cenário. **Não executar `flyway clean` ou apagar base atual.**

**L4 — Acesso por papel (decisão a explicitar):** Cadastros completos/usuários-permissões são administrativos; Gestor possui interface de Vínculos e gestão operacional, mas não deve assumir que tem autonomia de cadastrar todos os catálogos. Se a política exigir que Gestor mantenha algum cadastro-base sem Administrador, definir especificamente a delegação antes de alterar guards/rotas.

**L5 — Cobertura automática UI (pendente):** o frontend possui `vue-tsc` e `vite build`, mas não consta harness de Vitest/E2E na configuração revisada. Build verde não prova preenchimento de todos os formulários sob listas vazias, validação de obrigatórios ou ausência de recarga integral. Priorizar testes de contrato/formulário na Etapa 12, com caso de referência ausente.

### Separação entre diagnóstico, decisão e execução

- **Comprovado por código:** existem telas de CRUD de Laboratório, Projeto, SCI, Atividade, Produto, Classe/Local, Cursos/Culturas; não foi localizado CRUD manual de Usuário/Unidade nem criação inicial completa de Estagiário; existem seeds DEV.
- **Homologado pelo responsável anteriormente:** Vínculos, Pedidos, Soluções, Resíduos; Flyway V44; frontend build; ArchUnit 5/5.
- **Não comprovado nesta inspeção:** suporte de uma instalação sem seed; disponibilidade de referências para todas as Unidades, desempenho real, permissão definitiva institucional, teste E2E de formulário novo, sincronização automática já finalizada no GitLab.
- **Fora do escopo atual por decisão:** implementação de autenticação/SSO; testes destrutivos e carga extensa.
- **Política de documentação:** preservar checkpoints datados como históricos, mas apontar este documento e o adendo vigente do contrato da Etapa 8 como fontes atuais. Não reescrever decisões históricas para fingir que já estavam concluídas.

## Fechamento controlado

A sincronização **documental de conteúdo** deve ser feita em ambas as branches GitHub; a sincronização **GitHub → GitLab** deve ser conferida por comparação de hashes/Actions, nunca presumida. Sem merge, sem force push e sem modificar `main`.
