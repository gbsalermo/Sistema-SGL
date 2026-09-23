# Continuidade SGL — Etapa 4

**Criado originalmente em:** 17/09/2026  
**Atualizado em:** 22/09/2026  
**Etapa anterior:** Etapa 3 — Refinamentos do fluxo atual de Resíduos ✅ concluída e validada  
**Etapa atual:** Etapa 4 — Expansão operacional de Resíduos 🔧 reconciliação + revalidação
**Bloco atual:** 4.1 — Locais de armazenamento 🔧 backend reconciliado e validado; frontend portado e pendente de validação integrada  
**Branch histórica da implementação:** `feat/etapa-4-residuos`  
**Branch atual de trabalho:** `collab/etapa-4-residuos-reconcile`  
**Fonte canônica de `main`:** GitLab institucional  
**Documento de infraestrutura:** `docs/SINCRONIZACAO_GITLAB_GITHUB.md`

Este arquivo substitui a interpretação anterior de “Etapa 4 ainda não iniciada”. A Etapa 4 chegou a ser implementada integralmente em uma branch antiga, porém essa branch foi construída antes das correções mais recentes do supervisor no GitLab. Portanto, a implementação histórica será portada seletivamente para a base atual e só depois revalidada.

---

## 1. Estado real em 22/09/2026

A implementação histórica cobre:

```text
4.1 Locais de armazenamento cadastráveis          🔧 backend reconciliado/validado; frontend portado e aguardando validação integrada
4.2 Modelos de Resíduos pré-cadastrados            ✅ implementado na branch antiga
4.3 Uso de modelo ou preenchimento manual          ✅ implementado na branch antiga
4.4 Correções administrativas do ciclo             ✅ implementado na branch antiga
Validação integrada na base corrigida              ⏳ pendente após reconciliação
```

A antiga `feat/etapa-4-residuos` **não deve ser mergeada integralmente**.

Motivos:

- backend antigo ainda usava a estrutura `backend/sgl-backend/...`;
- `main` atual do GitLab avançou com correções do supervisor;
- houve alterações de segurança/tenant/testes que não podem ser sobrescritas;
- a numeração Flyway antiga conflita com a nova V16 canônica;
- `ResiduoService` sofreu mudanças relevantes em ambos os históricos.

Regra da reconciliação:

```text
gitlab/main atual
+ correções do supervisor
+ port seletivo da Etapa 4 antiga
= nova Etapa 4 reconciliada
```

---

## 2. Infraestrutura Git obrigatória

Antes de trabalhar nesta etapa, ler:

`docs/SINCRONIZACAO_GITLAB_GITHUB.md`

Resumo:

```text
GitLab/main
= fonte canônica

GitHub/collab/*
→ sincroniza automaticamente
→ GitLab/collab/*
→ MR
→ GitLab/main

GitLab/main
→ sincroniza automaticamente
→ GitHub/main
```

Não usar `--force`. Não editar `GitHub/main` diretamente.

Se a IA criar commits na branch GitHub enquanto o usuário trabalha localmente:

```bash
git pull --rebase github collab/etapa-4-residuos-reconcile
```

Antes de portar qualquer bloco, atualizar a branch com a base canônica quando for fast-forward possível:

```bash
git fetch gitlab --prune
git merge --ff-only gitlab/main
git push github collab/etapa-4-residuos-reconcile
```

---

## 3. Regra de trabalho

O responsável do projeto implementa manualmente o backend funcional.

Fluxo:

```text
IA analisa implementação antiga + main atual
→ explica o que deve ser preservado
→ adapta modelagem/arquivos/migrations
→ fornece passos e código de referência
→ usuário implementa manualmente
→ IA revisa
→ testes
→ próximo bloco
```

Frontend e documentação podem ser alterados diretamente quando autorizado.

Não portar todos os arquivos de uma vez.

---

## 4. Ordem de reconciliação

```text
4.1 Locais de armazenamento
→ validar compilação/testes
→ 4.2 ModeloResiduo
→ validar compilação/testes
→ 4.3 uso pelo Solicitante + frontend
→ validar integração
→ 4.4 correções administrativas
→ validar integração
→ bateria completa da Etapa 4
```

A antiga branch serve como referência de comportamento e código, não como fonte de verdade estrutural.

---

## 5. Flyway — decisão obrigatória

No `main` atual existe:

```text
V16__add_residuo_unidade_snapshot.sql
```

Essa migration veio das correções do supervisor e é canônica.

Na implementação antiga da Etapa 4 existiam:

```text
V16__create_residue_storage_locations.sql
V17__create_residue_models.sql
```

Na reconciliação devem virar:

```text
V16 = add_residuo_unidade_snapshot        ✅ preservar
V17 = create_residue_storage_locations    ⏳ portar
V18 = create_residue_models               ⏳ portar
```

Migration já aplicada é imutável. Nunca substituir a V16 do supervisor.

---

## 6. Etapa 4.1 — Locais de armazenamento

Objetivo preservado:

```text
local cadastrado
+ complemento livre
+ possibilidade manual quando permitido
+ snapshot textual no Resíduo
```

Arquitetura histórica de referência:

```text
LocalArmazenamentoResiduo
= catálogo mutável por Unidade

Residuo.localArmazenamentoResiduo
= referência opcional ao catálogo

Residuo.complementoLocalArmazenamento
= complemento da ocorrência

Residuo.localArmazenamentoTemporario
= snapshot textual histórico
```

A Gestão pode selecionar local cadastrado ou informar manualmente. Na confirmação física pode manter ou corrigir o local.

Ao portar:

> **Regra de segurança da retomada:** não copiar o `ResiduoService` antigo inteiro. A implementação histórica da 4.1 foi feita antes das correções fail-closed/cross-tenant do supervisor. Devem ser portados somente os trechos funcionais de local de armazenamento para o service atual.

- adaptar paths para a estrutura atual `src/...`;
- usar V17;
- manter isolamento de tenant da base corrigida;
- não reintroduzir lógica fail-open;
- revisar `Residuo`, DTOs, Controller e Service contra o `main` atual;
- preservar testes novos do supervisor.

---

## 7. Etapa 4.2 — ModeloResiduo

Regra principal:

```text
ModeloResiduo = definição reutilizável/editável
Residuo       = ocorrência real independente
```

A implementação histórica continha:

- `ModeloResiduo`;
- `ComponenteModeloResiduo`;
- repositories;
- DTOs;
- Service;
- Controller;
- CRUD por Unidade;
- nome único por Unidade;
- classes ativas da mesma Unidade;
- Produto opcional;
- tratamento padrão;
- Segurança/EPI;
- inativação lógica;
- tela administrativa no frontend;
- testes backend.

Não existe relação viva `Residuo -> ModeloResiduo` que altere ocorrência histórica.

Migration reconciliada: **V18**.

---

## 8. Etapa 4.3 — Uso pelo Solicitante

Na criação do Resíduo:

```text
usar modelo pré-cadastrado
ou
preencher manualmente
```

Modelo sugere/preenche dados reutilizáveis. Permanecem específicos da ocorrência:

- quantidade;
- usuário;
- laboratório;
- projeto;
- observação do gerador;
- demais dados que dependam do evento real.

O payload final continua sendo o contrato normal de criação de `Residuo`, preservando snapshot e independência histórica.

Frontend deve ser reconciliado somente depois do contrato backend do bloco estar estável.

---

## 9. Etapa 4.4 — Correções administrativas

Comportamento histórico planejado/implementado:

```text
CANCELAR
RETORNAR_ETAPA
```

Perfil:

```text
ADMINISTRADOR
```

Regras históricas:

- justificativa obrigatória;
- trilha no histórico;
- `CANCELADO` como estado terminal para retorno;
- `DESPACHADO` não cancela diretamente;
- retorno administrativo ocorre uma etapa por vez;
- nova execução da etapa cria novo evento e não apaga eventos anteriores.

Essas regras devem ser conferidas novamente contra as correções atuais de autorização/tenant antes da homologação.

---

## 10. Validação

Roteiro:

`docs/VALIDACAO_ETAPA_4.md`

Importante:

A bateria não deve ser executada como validação final enquanto os quatro blocos ainda não tiverem sido portados para a branch reconciliada.

Validação final esperada:

```text
backend test/build                          ⏳
frontend build/type-check                  ⏳
4.1 locais                                 ⏳
4.2 CRUD de modelos                        ⏳
4.3 modelo + manual                        ⏳
4.4 cancelar/retornar + histórico          ⏳
tenant entre Unidades                      ⏳
relatórios/cancelados                      ⏳
regressão do fluxo normal de Resíduos      ⏳
```

---

## 11. Achados do supervisor

As correções de segurança/tenant/testes feitas pelo supervisor na `main` têm precedência sobre a implementação antiga da Etapa 4.

Não desfazer durante o port:

- isolamento de tenant fail-closed;
- correções cross-tenant;
- snapshots de Unidade;
- testes adicionados pelo supervisor;
- alterações atuais de repositories/services;
- demais correções já integradas à `main`.

Quando houver conflito entre a feature antiga e a `main`, adaptar a funcionalidade da Etapa 4 ao código novo — nunca o contrário.

---

## 12. Próximo passo

Antes de continuar código:

```text
1. confirmar branches collab atualizadas com gitlab/main;
2. revisar 4.1 antiga contra main atual;
3. renumerar migration de locais para V17;
4. portar 4.1 manualmente no backend;
5. executar testes;
6. somente então avançar para 4.2.
```

A validação da Etapa 4 será retomada depois que a implementação antiga tiver sido integralmente reconciliada com a base atual.
