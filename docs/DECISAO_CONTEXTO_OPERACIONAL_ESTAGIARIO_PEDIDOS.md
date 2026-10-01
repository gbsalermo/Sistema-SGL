# Decisão estrutural — Contexto operacional de Estagiários em Pedidos

**Data:** 01/10/2026  
**Status:** decisão arquitetural aprovada; implementação futura nas Etapas 8–9  
**Branch de registro:** `collab/etapa-6-estagiarios-vinculos`

## 1. Problema identificado

O fluxo atual de Pedido ainda recebe do frontend `usuarioId`, `laboratorioId` e `projetoId`, e parte da validação usa `Usuario.laboratorio` como contexto operacional.

Esse desenho não representa corretamente o domínio estabilizado na Etapa 6, porque um Estagiário pode possuir múltiplas participações simultâneas em Atividades e essas Atividades podem pertencer a Projetos e Laboratórios diferentes, desde que permaneçam na mesma Unidade.

Portanto, não existe necessariamente um único "Laboratório do Estagiário".

## 2. Fonte institucional de verdade

Para Estagiários, o contexto operacional deve ser derivado da cadeia:

```text
Usuario
└── Estagiario
    └── VinculoEstagio
        └── VinculoEstagioAtividade
            └── Atividade
                └── SCI
                    └── Projeto
                        └── Laboratorio
                            └── Unidade
```

Culturas ficam associadas à participação `VinculoEstagioAtividade`, não ao Usuario nem globalmente ao Estagiário.

`Usuario.laboratorio` permanece temporariamente como campo legado/contexto institucional de compatibilidade, mas deixa de ser fonte de verdade operacional para Estagiários.

## 3. Regra para a interface de Estagiários

A interface não deve apresentar um único laboratório como atributo definitivo do Estagiário.

A visão alvo passa a apresentar as participações do vínculo, por exemplo:

```text
Atividade A
→ Projeto A
→ Laboratório A

Atividade B
→ Projeto B
→ Laboratório B
```

Se houver múltiplas participações abertas, múltiplos Laboratórios podem aparecer naturalmente.

Essa adaptação visual pertence ao bloco 6.5 — frontend integrado.

## 4. Regra futura para Pedido de Estagiário

Para um Estagiário operacionalmente ativo, o Pedido deve ser contextualizado pela participação em Atividade.

Fluxo alvo:

```text
Estagiário
→ escolhe uma de suas participações/Atividades abertas
→ backend resolve Atividade
→ deriva SCI
→ deriva Projeto
→ deriva Laboratório
→ confirma Unidade/tenant
→ cria Pedido
```

Se existir apenas uma participação aberta, a interface pode selecioná-la automaticamente. Se existirem várias, o Estagiário escolhe a participação correspondente ao Pedido.

O frontend não deve permitir combinações livres e independentes de Atividade, Projeto e Laboratório para Estagiários.

## 5. Relação futura de Pedido

A evolução estrutural prevista é:

```text
Pedido
├── Usuario solicitante
├── VinculoEstagioAtividade     ← contexto operacional do Estagiário
├── Projeto                     ← derivado pelo backend
├── Laboratorio                 ← derivado pelo backend
└── Itens
```

A FK de participação deve preservar o contexto histórico do Pedido.

Os campos diretos de Projeto/Laboratório podem ser mantidos por compatibilidade, consulta e rastreabilidade, mas para Pedido de Estagiário seus valores devem ser derivados e validados pelo backend, não aceitos como fonte independente de verdade enviada pelo cliente.

Para solicitantes que não sejam Estagiários, o contrato operacional específico permanece a ser fechado junto da refatoração de Pedidos; a participação de estágio não será exigida nesses perfis.

## 6. Contexto de Pedido para usuários não Estagiários

A participação de estágio é obrigatória somente para o perfil `ESTAGIARIO`. Para os demais solicitantes comuns — atualmente `TECNICO`, `ANALISTA` e `PESQUISADOR` — o contexto segue duas possibilidades.

### Usuário comum com Projeto

Quando o Pedido estiver associado a Projeto:

```text
TECNICO / ANALISTA / PESQUISADOR
→ escolhe Projeto
→ backend deriva Laboratorio
→ backend confirma Unidade/tenant
→ cria Pedido
```

O frontend não deve enviar Projeto e Laboratório como escolhas independentes. O Projeto é a fonte do contexto operacional e o Laboratório é consequência da hierarquia.

### Usuário comum sem Projeto

Pedidos operacionais gerais continuam válidos quando não houver Projeto aplicável:

```text
TECNICO / ANALISTA / PESQUISADOR
→ Pedido sem Projeto
→ usa Usuario.laboratorio como contexto institucional/base
→ confirma Unidade/tenant
→ cria Pedido
```

Nesse caso, `Usuario.laboratorio` continua útil e não deve ser removido do domínio apenas por causa da evolução dos Estagiários.

### Ordem de resolução do contexto

```text
1. Solicitante é ESTAGIARIO
   → participação aberta obrigatória
   → deriva Atividade/SCI/Projeto/Laboratório

2. Solicitante não é ESTAGIARIO e informou Projeto
   → deriva Laboratório do Projeto

3. Solicitante não é ESTAGIARIO e Pedido não possui Projeto
   → usa Usuario.laboratorio
```

Combinações inconsistentes devem ser rejeitadas pelo backend. Exemplo: um Projeto do Laboratório A não pode ser acompanhado de um Laboratório B fornecido livremente pelo cliente.

Assim, `Usuario.laboratorio` deixa de ser a fonte universal de contexto, mas continua sendo contexto institucional/base para operações sem Projeto de usuários não Estagiários.

## 6. Integridade e sincronização entre Solicitante e Gestão

Solicitante e Gestão são duas visões do mesmo registro de domínio.

```text
Frontend Solicitante
        \
         → Backend/Banco ← fonte da verdade
        /
Frontend Gestão
```

Não existe sincronização direta entre as duas interfaces.

Alterações institucionais em vínculo, curso, formação, Atividades ou Cultura são lidas novamente da API. Dados históricos do Pedido permanecem ligados à participação usada na criação.

## 8. Distribuição no roadmap

### Etapa 6.5 — frontend integrado

- remover a noção visual de um único "Laboratório do Estagiário";
- mostrar Atividade/Projeto/Laboratório por participação;
- preparar a experiência para múltiplos contextos operacionais.

### Etapa 8 — Unidades, Soluções e contexto operacional

- fechar o contrato de contexto operacional usado por Pedidos;
- deixar explícito que Unidade/tenant continua vindo da identidade/sessão, enquanto Laboratório operacional do Estagiário vem da participação;
- tratar `laboratorioId/laboratorioNome` da sessão DEV como compatibilidade, não como autoridade operacional do Estagiário;
- estabilizar o contrato antes da evolução de Pedido.

### Etapa 9 — Pedidos + Soluções

- adicionar associação Pedido → `VinculoEstagioAtividade` para Pedidos de Estagiários;
- alterar o payload para selecionar a participação, em vez de confiar em Laboratório/Projeto enviados livremente;
- derivar Projeto/Laboratório/Unidade no backend;
- preservar histórico;
- integrar o mesmo contexto às telas de Solicitante e Gestão;
- manter FIFO/FEFO, locks, aprovação, cancelamento/devolução e integração com Soluções.

## 9. Regra final

```text
Laboratório não é o núcleo do Estagiário.

Atividade/participação é o contexto operacional.
Projeto e Laboratório são derivados dessa participação.
Unidade continua sendo o limite institucional/tenant.
```
