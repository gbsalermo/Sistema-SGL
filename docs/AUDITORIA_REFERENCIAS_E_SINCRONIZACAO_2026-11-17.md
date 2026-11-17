# Auditoria de referências e sincronização — SGL

**Realizada em:** 17/11/2026. **Estado:** auditoria de integridade Markdown e evidências de GitHub Actions; **não** é merge nem homologação de ambientes.  
**Repositórios:** `gbsalermo/Sistema-SGL` e `gbsalermo/SGL-FRONTEND`.  
**Base:** branches GitHub `collab/docs-release-estado-2026-11`, antes da criação deste relatório. GitLab `release` continua fonte canônica de produção.

## 1. Integridade dos links Markdown locais

**Verificação:** leitura de cada Markdown incluído no lote e extração de caminhos em **links e imagens Markdown** (ignorando exemplos que não representam referências reais); normalização de caminhos relativos (`.`, `..`, `#fragmento`, query) e confronto do **arquivo-alvo** com o Git tree da branch colaborativa.

| Repositório | Markdown presentes no lote | Links Markdown relativos resolvidos | Arquivos-alvo ausentes |
|---|---:|---:|---:|
| Backend | **56** | **108** | **0** |
| Frontend, incluindo 21 textos da Ajuda | **90** | **18** | **0** |
| **Total** | **146** | **126** | **0** |

**Escopo exato:** 57 arquivos Markdown estavam na árvore do backend e 91 no frontend antes da criação do relatório; a diferença de um por repositório corresponde a `docs/HISTORICO_DOSSIE_*_ATE_2026-11-12.md`, cópia integral arquivada que não fez parte da varredura. Os README de raiz, `docs/README.md` e os artigos publicados da Ajuda **foram lidos, mas nenhum foi editado nessa etapa de medição**.

**Não validado nesta checagem:** fragmentos `#ancora` no documento-alvo, respostas HTTP dos URLs externos, rotas dinâmicas Vue, URLs em texto simples/blocos de código e imagens HTML fora da amostra pontual descrita abaixo. Regex de Markdown não equivale a parser completo. Assim, **zero caminhos ausentes não significa zero erros de navegação, mídia ou execução**.

**Verificação complementar dos recursos HTML dos READMEs:** `README.md` do backend tem `<img src="docs/LOGO.png">`, e o arquivo existe no Git. O README do frontend usa imagem externa `https://raw.githubusercontent.com/gbsalermo/Sistema-SGL/main/docs/LOGO.png`; o blob `docs/LOGO.png` é **idêntico** no `main` e no `release` do backend no momento da inspeção (SHA `cac30c2c5143ef4c0fe968476a81dcf8d0f26709`). Isso verifica a origem versionada, **não o comportamento do navegador/CDN ao baixar a imagem**.

## 2. Estado verificável das branches e dos workflows

**Antes de novos commits deste relatório:**

| Repositório | Comparação GitHub `release...collab/docs-release-estado-2026-11` | HEAD auditado | GitHub Actions `collab/* → GitLab` |
|---|---|---|---|
| Backend | **100 à frente, 0 atrás** | `c8999ba99b135b0e0ef018660781f94e479c6903` | último sucesso encontrado para **esse SHA** em 17/11, run [44155410150](https://github.com/gbsalermo/Sistema-SGL/actions/runs/44155410150) |
| Frontend | **71 à frente, 0 atrás** | `b9e218da55bbeb70bba29ea6c3104589224137aa` | último sucesso encontrado para **esse SHA** em 17/11, run [44155424823](https://github.com/gbsalermo/SGL-FRONTEND/actions/runs/44155424823) |

Os commits acima e os runs são **snapshot anteriores ao relatório**; novas edições na branch devem ganhar nova confirmação de sincronização.

**Arquivos de workflow verificados no GitHub `main` (que hospeda agendamentos/dispatch):**
- Backend: `.github/workflows/check-gitlab-connectivity.yml` tem nome de UI *Sync collab branches to GitLab*, com gatilho push `collab/**`, recusa divergência e push sem `--force`.
- Frontend: `.github/workflows/sync-collab-to-gitlab.yml`, mesma política geral `collab/**`.
- Ambos: `.github/workflows/sync-gitlab-release-to-github.yml` e `sync-gitlab-main-to-github.yml` existem no **GitHub main**, embora o arquivo de espelhamento `release` **não esteja na árvore da própria `release`**. Isso **é esperado**, pois workflow com gatilho `schedule` é hospedado na branch padrão e faz fetch explícito da branch GitLab; não criar pendência falsa por sua ausência na `release`.
- Runs bem-sucedidos de espelhamento `release` observados em 17/11: backend [run 44141832152](https://github.com/gbsalermo/Sistema-SGL/actions/runs/44141832152); frontend [run 44163380471](https://github.com/gbsalermo/SGL-FRONTEND/actions/runs/44163380471). **Sucesso do workflow não substitui comparação direta, posterior, dos SHAs GitLab/GitHub**.

**Limitação:** a verificação não fez fetch direto do servidor GitLab institucional, nem confirmou MR, aprovação, deploy, proteção de branches, ambiente produtivo, status de mídia ou migração. A ausência de arquivos funcionais alterados foi verificada via comparação GitHub `release...collab`; somente `docs/*` e `CONTINUIDADE.md` tinham mudanças antes deste relatório.

## 3. Condições para conclusão e merge documental

1. **Após os últimos commits**, confirmar execução de sincronização `collab/*` com o novo SHA em ambos os repos; verificar na própria branch GitLab que o SHA recebido coincide.
2. Executar no clone autorizado:
   ```bash
   git fetch github --prune
   git fetch gitlab --prune
   git rev-list --left-right --count gitlab/release...github/release
   git rev-list --left-right --count gitlab/collab/docs-release-estado-2026-11...github/collab/docs-release-estado-2026-11
   git diff --name-status gitlab/release...github/collab/docs-release-estado-2026-11
   ```
   **Esperado:** `0 0` na sincronização de release e na branch collab; apenas Markdown/documentação no diff. Não fazer `--force`, nem merge direto na `main`/GitHub `release`.
3. Conferir `README.md` e `docs/README.md` dos dois repos: os índices antigos ainda possuem descrições de Etapa 6 “próxima”, Etapa 9 “aguardando merge”, rótulos apenas planejados e contexto DEV como fonte de autorização. **Essas seções são históricas e precisam ser reconciliadas no fechamento**, com referências para o dossiê e `CONTINUIDADE.md`. O último código conhecido da `release` já contém as implementações referidas; testes de produção não foram feitos.
4. Preservar `src/modules/ajuda/`, `public/ajuda/`, vídeos, posters, PDFs e scripts sem modificação; revisar o diff de media e Ajuda. `SGL-REV-006/031` seguem como problemas **editoriais futuros**, não motivo para alterar material publicado agora.
5. Submeter MRs documentais separados no GitLab para backend/frontend e verificar conflitos/linhas divergentes antes de aceitar. Preferir integração que preserve o conteúdo histórico; qualquer squash/rebase deve ser decidido no fluxo de MR, **não reescrever o histórico remoto por força**.
6. **Validação funcional futura distinta:** executar [`PLANO_HOMOLOGACAO_NUVEM_MOBILE_2026-11-17.md`](PLANO_HOMOLOGACAO_NUVEM_MOBILE_2026-11-17.md) com staging isolado e dispositivos Android/iOS. Não afirmar QA concluído porque o documento existe.

## 4. Próximas tarefas de qualidade documental

- Revisar navegabilidade de **âncoras internas** e URLs públicas críticas em rodada específica, com validação de destino HTTP quando houver acesso autorizado; não confundir com links relativos já conferidos.
- Ao encerrar o conjunto documental, referenciar este relatório nos inventários e READMEs; manter o [registro único de problemas](REGISTRO_PENDENCIAS_REVISAO_SGL_2026-11.md) separado da auditoria. Não abrir um item de correção funcional somente porque um arquivo de etapa histórica menciona uma pendência que já foi resolvida.
- Repetir a auditoria de caminhos após mover/renomear qualquer documento. Nesta rodada **não houve movimentos de arquivos históricos**.
