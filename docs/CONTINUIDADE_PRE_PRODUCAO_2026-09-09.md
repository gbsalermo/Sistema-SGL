# Checkpoint de continuidade — Pré-Produção SGL

**Data:** 09/09/2026  
**Etapa atual:** Etapa 1 — Padronização e refinamento visual global  
**Subetapa atual:** 1.4 — aplicação tela a tela

Este arquivo é um checkpoint operacional complementar ao plano canônico `docs/PLANO_PRE_PRODUCAO.md` para retomada imediata do trabalho.

## Estado atual

```text
1.1 — definição do padrão visual                    ✅ concluída
1.2 — fundação visual compartilhada                ✅ concluída
1.3 — componentes básicos compartilhados           ✅ concluída
1.4 — aplicação tela a tela                        🔧 em andamento
```

## Blocos aprovados e integrados no frontend

```text
Pedidos                       ✅
Dashboards                     ✅
Resíduos                       ✅
Estoque                        ✅
Movimentações                  ✅
```

## Estagiários

A aplicação visual de Estagiários foi preparada em 09/09/2026 e ficou aguardando validação.

Frontend:

```text
repo: gbsalermo/SGL-FRONTEND
branch: feat/etapa-1-4-estagiarios
PR: #36
rota: /estagiarios
```

Regra: **não mergear o PR #36 antes da validação visual e funcional do responsável do projeto**.

Primeiro passo na próxima retomada:

```text
validar /estagiarios
→ se aprovado, squash merge do PR #36
→ iniciar bloco de Relatórios
```

## Interfaces restantes após Estagiários

```text
/relatorios
/relatorios/residuos
/relatorios/pessoas-laboratorio
/administracao/cadastros
```

Os três caminhos de Relatórios devem preferencialmente ser tratados como um único bloco visual. Administração/Cadastros encerra as interfaces operacionais principais da 1.4.

Não contar como interfaces pendentes:

- rotas de solicitação que reutilizam Views de Pedidos já padronizadas;
- login, explicitamente fora do escopo desta padronização;
- 404, tela técnica;
- rótulo de Resíduo, cujo refinamento está na Etapa 3.2;
- rótulo de Produto, recomendado para a revisão conjunta de impressão/rótulos.

## Decisões visuais recentes

- superfícies operacionais normalmente brancas/neutras;
- Dashboard do Solicitante mantém diferenciação suave por domínio: Pedidos azul claro e Resíduos creme/amarelo claro;
- telas operacionais de Resíduos não usam creme estático; creme pode aparecer em hover quando útil;
- Movimentações usa cor estática muito suave por tipo para facilitar leitura de tabela densa:
  - entrada/devolução = verde;
  - saída = azul;
  - ajuste = amarelo;
  - descarte = vermelho;
- hover de Movimentações intensifica levemente a mesma semântica.

## Requisitos futuros já posicionados no plano

### Etapa 3.2

- refinamento do rótulo de Resíduo;
- compatibilidade de impressão com impressoras Zebra;
- avaliar uma **Ficha/Comprovante de Lote** imprimível com os dados do lote para acompanhamento/repasse de material;
- essa ficha é operacional/informativa e não deve ser tratada como nota fiscal oficial;
- decidir posteriormente entre impressão pelo navegador, PDF ou ambos.

### Etapa 7.1

Antes de alterar estruturalmente Pedidos, analisar o padrão real de pedido usado pelo cliente, comparar com o SGL e sintetizar um padrão-alvo aproveitando os pontos positivos dos dois e descartando redundâncias/limitações.

## Sequência de retomada

```text
validar Estagiários
→ Relatórios (3 rotas)
→ Administração / Cadastros
→ revisão final da 1.4
→ fechamento documental da Etapa 1
→ Etapa 2 — Dark Mode definitivo
```

A matriz de permissões continua posterior ao bloco atual de pré-produção e não deve ser tratada como próxima tarefa imediata.
