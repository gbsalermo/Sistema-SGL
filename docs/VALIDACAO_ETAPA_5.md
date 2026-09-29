# Validação e Fechamento — Etapa 5

**Etapa:** Projetos e Atividades  
**Fechamento:** 29/09/2026  
**Branch:** `collab/etapa-5-projetos-atividades`  
**Estado:** ✅ concluída e validada

## Escopo encerrado

```text
5.0 Portão de confirmação                         ✅
5.1 Projeto base                                  ✅
5.2 SCI                                           ✅
5.3 Atividades + prorrogações                     ✅
5.4 Código SEG                                    ✅
5.5 Interface e integração                        ✅
```

## Backend validado

- Projeto → SCI → Atividade estabilizado;
- regras temporais e de pertencimento hierárquico;
- V19–V24 aplicadas para expansão de Projeto, SCI, Atividade, prorrogações, unicidade SEG e histórico de correção SEG;
- prorrogações explícitas, justificadas e auditáveis;
- Código SEG com formato hierárquico e unicidade global;
- imutabilidade do Código SEG após definição no CRUD comum;
- correção administrativa auditável;
- massa DEV idempotente;
- correção do initializer para manter a carga DEV transacional;
- suíte JUnit completa confirmada verde durante o fechamento.

## Frontend validado

- rota operacional `/projetos`;
- Projeto como eixo principal de navegação;
- filtro por Laboratório, busca e status;
- detalhes do Projeto;
- SCI e Atividades agrupados hierarquicamente;
- cadastro/edição de SCI;
- cadastro/edição de Atividade;
- prorrogação de Projeto/SCI/Atividade;
- correção administrativa de Código SEG;
- histórico de prorrogações e correções;
- cadastro administrativo de Projeto alinhado ao domínio atual;
- dark mode;
- tipografia baseada nos tokens canônicos;
- identificação visual SCI em azul e Atividade em verde;
- refinamentos de hierarquia visual validados.

## Código SEG — decisão final

```text
Projeto   XX.XX.XX.XXX.XX.00
SCI       XX.XX.XX.XXX.XX.SS
Atividade XX.XX.XX.XXX.XX.SS.AAA
```

- Projeto é o único nível que pode existir temporariamente sem SEG;
- primeira definição do SEG de Projeto ocorre em Administração > Cadastros > Projetos;
- sem SEG no Projeto não se cria SCI;
- SCI exige SEG;
- Atividade exige SCI com SEG;
- o frontend sugere o próximo sufixo provável para novo SCI/Atividade;
- a sugestão é editável antes do primeiro salvamento e não transforma sequência em regra obrigatória;
- códigos institucionais avulsos permanecem permitidos se válidos e únicos;
- após persistido, o SEG só muda pelo fluxo administrativo de correção.

## Validação da 5.5

A validação visual/integrada foi confirmada em execução pelo responsável do projeto em 29/09/2026.

Foram refinados durante a validação:

- escala tipográfica;
- filtro por Laboratório;
- remoção de ação redundante de abrir hierarquia;
- hierarquia visual SCI/Atividade;
- diferenciação azul/verde;
- sugestão assistida do Código SEG.

Não se registra uma suíte automatizada frontend como executada nesta etapa. A suíte JUnit do backend foi confirmada verde.

## Decisões já encaminhadas

### Etapa 6

Próximo foco: Estagiários e vínculos, usando Atividade → SCI → Projeto como vínculo estrutural.

### Etapa 7

- adicionar relatório consolidado de Projetos;
- sintetizar Movimentações e Resumo operacional em uma única opção de interface;
- manter modos internos Resumo e Detalhamento;
- preservar inicialmente endpoints atuais para compatibilidade.

## Regra de retomada

Após o merge da branch da Etapa 5:

1. garantir sincronização GitLab/GitHub conforme `docs/SINCRONIZACAO_GITLAB_GITHUB.md`;
2. criar branch própria da Etapa 6 a partir da `main` canônica sincronizada;
3. não reabrir o escopo funcional da Etapa 5 salvo correção de regressão;
4. usar `CONTINUIDADE.md` e `docs/PLANO_PRE_PRODUCAO.md` como checkpoints principais.
