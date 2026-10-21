# Etapa 8 — Contrato enxuto de vínculos operacionais e primeiro acesso

**Status:** decisão de produto registrada, implementação de vínculos administrativos condicionada à autenticação/autorização real. Não substituir a sessão DEV por auto-cadastro não autenticado.

## Identidade e autorização
- Ambiente institucional é fonte de identidade quando integração autenticada estiver disponível; não pressupor que fornece laboratório, orientador, projeto ou atividade.
- No primeiro acesso **autenticado**, localizar usuário por identificador institucional estável (não apenas nome/e-mail). Se ausente, provisionar conta em estado PENDENTE_VINCULACAO; nunca conceder privilégios operacionais na criação.
- Existente: validar identidade e situação, carregar autorizações vigentes; conta sem vínculo permanece sem permissão operacional.
- Gestor/Admin autorizado da mesma Unidade aprova vínculos e laboratório-base. Registar ator, data, início/fim, justificativa e auditoria.
- O backend de hoje usa permitAll e login DEV sem verificação da senha. Nenhum endpoint de concessão de acesso pode confiar só em perfil/session/header de navegador. Requer autenticação real e checagem de permissões server-side ANTES de expor escrita.

## Relações
- Preservar Usuario.laboratorio como laboratório-base, inicialmente nulo para primeiro provisionamento.
- Vínculos adicionais tenant-scoped: Usuário ↔ Laboratório/Projeto/SCI/Atividade; cada vínculo tem escopo, vigência e concedente. Contextos ascendentes são derivados; não criar automaticamente vínculos formais em todos os níveis.
- Para estagiários, preservar VinculoEstagioAtividade (sem duplicar). Cadastro de orientador é operação específica do estágio.
- Responsável por laboratório já é Usuario; Projeto e Atividade têm responsável textual, cuja eventual migração a referência de usuário exige compatibilidade histórica.

## Pedidos, soluções e resíduos
- Estagiário: participação elegível obrigatória e contexto derivado no backend.
- Técnico/Analista/Pesquisador: laboratórios explicitamente autorizados, inclusive base. Pedido geral permitido.
- Projeto de laboratório autorizado sem vínculo específico: pedido permitido com sinalização persistida (sem vínculo direto) e alerta visível na análise; não conceder acesso indiscriminado a dados de todos projetos.
- Projeto em laboratório sem autorização: bloquear no backend.
- Solução continua exigindo projeto. Aprovação e baixa seguem políticas próprias de Gestão.
- Snapshot imutável do contexto e da condição de vínculo no momento do pedido.
- Gestor e Administrador: autorização de vínculo e consulta somente nos escopos autorizados; orientação não concede aprovação de estoque.
- Supervisor/orientador: leitura de pedidos próprios orientandos conforme escopo e vínculo vigente, sem permissão implícita de aprovar.

## UI e regressão
- Gestão > Administração > Cadastros: laboratório-base, vínculos adicionais, pendentes do primeiro acesso, histórico.
- Laboratórios/Projetos/Atividades reutilizam entidades existentes.
- Testar identidade não autenticada, falsificação de perfil, alteração de tenant, autoelevação, concedente não autorizado, vínculo revogado, múltiplos laboratórios, pedido sem vínculo específico, estágio, histórico e regressão de estoque.
- Nunca fazer merge sem build, Flyway e suíte + testes manuais.

## Decisão externa necessária
Definir fornecedor real de autenticação institucional (OIDC/SAML/LDAP/API confiável) ou implementar autenticação local segura com controle de provisionamento. Não inferir essa integração pelo login DEV.
