# Sincronização GitLab ↔ GitHub — SGL

**Atualizado em:** 22/09/2026  
**Estado:** infraestrutura ativa e validada  
**Abrange:** backend `sgl-api` e frontend `sgl-web`  
**Objetivo:** permitir que outra pessoa ou IA entenda exatamente qual repositório é canônico, onde trabalhar, como os pushes/pulls funcionam e como evitar sobrescrever trabalho do supervisor.

---

## 1. Regra central

O GitLab institucional da Embrapa é a fonte de verdade operacional.

```text
GitLab/main
= main canônica
= recebe trabalho do supervisor
= destino final dos Merge Requests
```

O GitHub é espelho de `main` e ponto de colaboração para branches `collab/*`.

```text
GitLab/main
    ↓ automático
GitHub/main

GitHub/collab/*
    ↓ automático
GitLab/collab/*
    ↓ MR
GitLab/main
```

Nunca tratar o GitHub `main` como fonte primária enquanto este modelo estiver ativo.

---

## 2. Repositórios

### Backend

```text
GitLab: https://git-cnpmf.nuvem.ti.embrapa.br/nti/sgl-api
GitHub: gbsalermo/Sistema-SGL
```

Remotes locais esperados:

```text
github  git@github.com:gbsalermo/Sistema-SGL.git
gitlab  https://git-cnpmf.nuvem.ti.embrapa.br/nti/sgl-api
```

### Frontend

```text
GitLab: https://git-cnpmf.nuvem.ti.embrapa.br/nti/sgl-web
GitHub: gbsalermo/SGL-FRONTEND
```

Remotes locais esperados:

```text
github  git@github.com:gbsalermo/SGL-FRONTEND.git
gitlab  https://git-cnpmf.nuvem.ti.embrapa.br/nti/sgl-web
```

A configuração antiga em que `origin` possuía dois destinos de push foi removida. Não recriar multi-push em um mesmo remote.

---

## 3. Responsabilidade de cada direção

### GitHub `collab/*` → GitLab `collab/*`

Branches colaborativas são criadas a partir do `gitlab/main` atualizado, publicadas primeiro no GitHub e sincronizadas automaticamente para o GitLab.

Uso:

```text
usuário/IA trabalha em GitHub/collab/*
→ GitHub Actions valida o nome da branch
→ consulta a branch correspondente no GitLab
→ se o GitLab não divergiu, faz push normal
→ cria/atualiza GitLab/collab/*
→ MR é aberto no GitLab contra main
```

Proteções:

- somente branches `collab/**`;
- não usa `--force`;
- se a branch homônima do GitLab contiver commits que não estejam no GitHub, o workflow falha;
- a falha é intencional para impedir sobrescrita de histórico;
- não editar manualmente a mesma `collab/*` nos dois remotes.

Workflow:

```text
Backend:
.github/workflows/check-gitlab-connectivity.yml
```

O nome do arquivo do backend é legado; o nome exibido no Actions é **Sync collab branches to GitLab**.

```text
Frontend:
.github/workflows/sync-collab-to-gitlab.yml
```

### GitLab `main` → GitHub `main`

O GitHub Actions executa a cada 15 minutos e também aceita disparo manual.

Uso:

```text
workflow no GitHub
→ busca GitLab/main via HTTPS
→ compara GitHub/main com GitLab/main
→ se já iguais, encerra com sucesso
→ se GitHub/main for ancestral, faz fast-forward
→ se houver divergência, falha sem sobrescrever nada
```

Workflow nos dois repositórios:

```text
.github/workflows/sync-gitlab-main-to-github.yml
```

Regra obrigatória:

```text
GitHub/main nunca deve receber implementação funcional direta.
```

Mudança destinada à `main` deve entrar via GitLab/MR e depois chegar ao GitHub pelo sincronizador.

---

## 4. Credencial usada pelos workflows

Nos dois repositórios GitHub existe um Repository Secret:

```text
GITLAB_PUSH_TOKEN
```

Ele contém um Personal Access Token do GitLab.

A instalação atual do GitLab não expunha `write_repository`; foi usado o escopo `api`, que permite a operação Git-over-HTTPS conforme as permissões da conta.

Regras:

- nunca registrar o valor do token em documentação, código, issue, commit ou chat;
- não imprimir o token em logs;
- manter o token apenas em GitHub Actions Secrets;
- se expirar ou for revogado, os workflows falharão até a rotação;
- o token não concede permissões superiores às da conta dona dele.

Foi tentada uma chave SSH dedicada, porém a porta 22 do GitLab retornou timeout no ambiente local. A solução SSH foi abandonada e não faz parte da arquitetura atual.

---

## 5. Fluxo local recomendado

Antes de qualquer trabalho:

```bash
git fetch gitlab --prune
git fetch github --prune
```

Para iniciar uma nova branch colaborativa:

```bash
git switch -c collab/<nome> gitlab/main
git push -u github collab/<nome>
```

Depois disso, o workflow replica a branch para o GitLab.

Se a IA criar commits diretamente no GitHub enquanto o usuário também possui a branch local:

```bash
git pull --rebase github collab/<nome>
```

Executar isso antes de continuar editando localmente.

Se o supervisor avançar `GitLab/main` e a branch local ainda não possuir commits próprios:

```bash
git fetch gitlab --prune
git merge --ff-only gitlab/main
git push github collab/<nome>
```

Se a branch já possuir commits próprios, não usar comandos destrutivos. Comparar os históricos e reconciliar explicitamente.

---

## 6. O que não fazer

```text
NÃO usar git push --force.
NÃO trabalhar diretamente em GitHub/main.
NÃO manter o mesmo branch sendo editado independentemente no GitHub e no GitLab.
NÃO restaurar o antigo origin com múltiplos push URLs.
NÃO sobrescrever GitLab/main para fazer o GitHub "ganhar".
NÃO assumir que uma branch antiga pode ser mergeada inteira sobre main atual.
```

O GitLab é canônico porque o supervisor trabalha nele e pode avançar `main` fora do ciclo da IA.

---

## 7. Reconciliação histórica feita em 22/09/2026

Os `main` do GitLab e GitHub possuíam históricos divergentes.

Foi feita uma reconciliação única, sem `--force`:

1. partiu-se do `GitLab/main`;
2. o antigo `GitHub/main` foi registrado como segundo ancestral por merge de histórico;
3. a árvore preservada foi a do GitLab;
4. o merge entrou no GitLab por MR;
5. depois o GitHub `main` foi avançado por fast-forward;
6. o sincronizador automático foi instalado e testado.

Após essa operação, backend e frontend chegaram a `0 0` em:

```bash
git rev-list --left-right --count gitlab/main...github/main
```

Snapshots no fechamento da configuração:

```text
Backend main GitHub observado:  a9747ac71d9f76ac7437f49224693077dcc838d9
Frontend main GitHub observado: e24a3444e28da25a733a42fc04288ce58c5754ad
```

Esses SHAs são apenas checkpoints históricos. A regra permanente é comparar os remotes, não depender desses valores.

---

## 8. Situação especial da Etapa 4

A antiga branch `feat/etapa-4-residuos` contém implementação funcional feita antes das correções mais recentes do supervisor.

Ela deve ser tratada como **fonte de referência**, não como branch pronta para merge.

Branches atuais de reconciliação:

```text
Backend:  collab/etapa-4-residuos-reconcile
Frontend: collab/etapa-4-residuos-reconcile
```

Regra:

```text
main atual do GitLab
+ correções do supervisor
+ port seletivo da implementação antiga
= nova Etapa 4 reconciliada
```

Não fazer merge integral da antiga `feat/etapa-4-residuos`.

No backend existe ainda conflito de numeração de migration:

```text
V16 = add_residuo_unidade_snapshot          já canônica no main
V17 = locais de armazenamento              deve ser renumerada na reconciliação
V18 = modelos de Resíduo                   deve ser renumerada na reconciliação
```

A migration V16 antiga da feature não deve substituir a V16 canônica do supervisor.

---

## 9. Fluxo de MR

Para qualquer mudança colaborativa:

```text
GitHub/collab/<branch>
→ sincronização automática
→ GitLab/collab/<branch>
→ Merge Request no GitLab
→ revisão
→ GitLab/main
→ sincronização automática
→ GitHub/main
```

O MR no GitLab continua sendo o gate de integração.

---

## 10. Testes dos workflows

Os dois sentidos foram testados manualmente em 22/09/2026.

Validações realizadas:

```text
GitHub collab → GitLab collab         ✅
bloqueio de branch não-collab         ✅
GitLab main → GitHub main             ✅
fast-forward sem force                ✅
falha fechada em caso de divergência  ✅ por lógica do workflow
```

O workflow de `collab/*` foi intencionalmente disparado em `main` durante um teste e falhou, comprovando que a proteção de nome da branch funciona.

---

## 11. Regra para outra IA

Antes de alterar código:

```text
1. ler CONTINUIDADE.md;
2. ler este arquivo;
3. confirmar gitlab/main e github/main;
4. executar fetch dos dois remotes;
5. verificar a branch collab atual;
6. nunca assumir que GitHub/main é canônico;
7. preservar correções do supervisor;
8. usar a branch antiga da Etapa 4 apenas como referência;
9. fazer commits pequenos;
10. deixar integração final para MR no GitLab.
```

Quando houver dúvida sobre divergência:

```bash
git rev-list --left-right --count gitlab/main...github/main
git merge-base gitlab/main github/main
```

Para uma branch:

```bash
git rev-list --left-right --count gitlab/<branch>...github/<branch>
```

Nunca resolver divergência com force push por padrão.

---

## 12. Sincronização documental

Documentação de infraestrutura e continuidade deve permanecer coerente nos dois repositórios.

Arquivos que devem ser atualizados em conjunto quando este fluxo mudar:

```text
Backend:
CONTINUIDADE.md
docs/DOSSIE_PROJETO_SGL.md
docs/README.md
docs/PLANO_PRE_PRODUCAO.md
docs/CONTINUIDADE_ETAPA_4_2026-09-17.md
docs/SINCRONIZACAO_GITLAB_GITHUB.md

Frontend:
CONTINUIDADE.md
docs/DOSSIE_PROJETO_SGL.md
docs/README.md
docs/SINCRONIZACAO_GITLAB_GITHUB.md
```

Se uma alteração de infraestrutura for feita em apenas um dos lados, a tarefa documental ainda não está concluída.
