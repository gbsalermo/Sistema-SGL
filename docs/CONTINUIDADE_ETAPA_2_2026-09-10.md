# Continuidade SGL — 10/09/2026

## Estado atual

A Etapa 1 — Padronização e refinamento visual global — foi concluída e validada no frontend.

O fechamento incluiu:

- Pedidos;
- Dashboards;
- Resíduos;
- Estoque;
- Movimentações;
- Estagiários;
- Relatórios;
- Administração/Cadastros;
- ajuste final da marca SGL nas interfaces autenticadas;
- correção do recolhimento da sidebar da Gestão.

PR final da Etapa 1 no frontend:

```text
SGL-FRONTEND #40
merge: 7777a3ee8bb37baf721af0a662ee2b7b9c46be0a
```

## Etapa atual

Etapa 2 — Dark Mode definitivo.

A execução deve respeitar a ordem já definida no plano canônico:

```text
esboço
→ paleta de cores
→ regras de comportamento
→ tokens/componentes
→ aplicação nas telas autenticadas
→ revisão tela a tela
→ testes
```

## Decisões preservadas

- Dark Mode somente nas interfaces autenticadas;
- Login e 404 permanecem claros;
- Gestão e Solicitante compartilham a mesma linguagem visual;
- preferência escolhida pelo usuário deve persistir;
- a referência visual usa superfícies em tons de azul-marinho, sem cards brancos sobre fundo escuro;
- cores semânticas continuam existindo em versões adequadas ao tema escuro;
- telas de impressão/rótulos preservam fundo claro quando necessário.

## Auditoria inicial da Etapa 2

O frontend atual possui três camadas provisórias de Dark Mode (`dark-mode-runtime.css`, `dark-mode-coverage.css`, `dark-mode-consistency.css`), muitos seletores amplos com `!important`, cores repetidas e mais de uma fonte de verdade para o tema.

A Etapa 2 deve convergir para uma única arquitetura de tema, sincronizando preferência do usuário, Vuetify, raiz do DOM, tokens e componentes.

Documento operacional detalhado no frontend:

```text
docs/ETAPA_2_DARK_MODE.md
branch inicial: feat/etapa-2-dark-mode
```

## Próximo passo exato

Subetapa 2.1 — validar a paleta e a hierarquia visual do Dark Mode usando o Dashboard como piloto antes de espalhar o tema definitivo pelas demais telas.
