# Ajuste visual e de fluxo — vínculos de usuários (21/10/2026)

## Motivo
Homologação 1.1–1.4 validada, porém a interface de vínculos foi considerada inadequada: item de navegação em Solicitações, campos crus sem hierarquia visual, difícil identificar usuário/status, ausência de histórico claro e encerramento sem confirmação. Erro 400 era exibido como mensagem HTTP genérica. Imagens anexadas pelo responsável foram usadas como referência da divergência visual. O teste 1.5 foi interrompido para ajustes.

## Implementação na branch `collab/etapa-8-contextos-pedidos`
- Sidebar Gestão: **Vínculos de usuários** movido para **Administração** (inclusive Gestor). `Cadastros > Permissões` mantém edição de perfis e contém atalho para o gerenciamento dedicado, sem duplicar formulário.
- `VinculosUsuariosCadastro.vue` refeito seguindo o layout de Estagiários: cards de resumo; lista/tabela com busca, perfil, laboratório-base, vínculo e situação; drawer por usuário com identidade, laboratório-base, laboratórios autorizados, vínculos vigentes e histórico.
- Status **Pendente de vinculação** se usuário ativo não tiver laboratório-base nem vínculo operacional vigente. Este estado **não** significa necessariamente primeiro login; o sistema ainda usa usuários DEV e não registra data de primeiro acesso.
- Modal de criação com campos hierárquicos progressivos e descrição; modal de confirmação antes de encerrar, identificando claramente o vínculo; cards encerrados sem ação de novo encerramento.
- Modal de edição **somente** para observação e data de término prevista, preservando identidade/escopo/histórico. Troca de escopo requer encerrar e criar novo vínculo.
- Novas mensagens interpretam `AxiosError.response.data.message` quando disponível e identificam conflito de duplicidade de contexto sem mostrar apenas `Request failed with status code 400`.
- CSS responsivo dedicado `src/styles/etapa-8-usuarios.css`, layout similar a Estagiários, com drawers e formulários ajustados para mobile.
- Backend DEV/DEMO: `GET /api/v1/vinculos-operacionais/resumo` agrega por unidade em uma consulta, evitando uma chamada por usuário; `PUT /{id}` permite editar observação e previsão de fim, sem permitir alterações destrutivas de contexto.

## Pendências para validar
- Acesso da Gestão e Admin pela sidebar (e link Cadastros).
- Estado pendente versus vinculado conforme laboratório-base e vínculo adicional.
- Busca, perfil, responsividade desktop/mobile, abertura/fechamento drawer, leitura de histórico.
- Duplicidade de vínculo aberto gera feedback legível, sem criar cópias; erros reais devem retornar mensagem do backend.
- Encerrar gera confirmação e mantém histórico sem botão ativo após revogação.
- Alterar previsão de término/observação e conferir integridade de contexto.
- Backend Maven e frontend build/testes **não executados nesta sessão**, alterações **não homologadas**.
- Login DEV ainda não representa autenticação/autorização institucional. Não implantar endpoints DEV em produção.
