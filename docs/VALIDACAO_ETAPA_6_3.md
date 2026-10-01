# Validação da Etapa 6.3 — Dados acadêmicos, Curso, Cultura e segurança

**Data:** 01/10/2026  
**Branch:** `collab/etapa-6-estagiarios-vinculos`  
**Status:** concluída e validada ✅

## Escopo validado

A Etapa 6.3 consolidou os dados acadêmicos e temáticos do vínculo de estágio sem duplicar informações no `Estagiario`.

### 6.3.1 — Formação, Curso e treinamento de segurança

Implementado e validado:

- `FormacaoEstagiario` como enum controlado;
- opção `OUTRO` com `formacaoOutro` obrigatório;
- catálogo `Curso` por Unidade;
- Curso com inativação lógica e preservação histórica;
- `VinculoEstagio.formacao`, `formacaoOutro` e `curso`;
- `treinamentoSegurancaConcluido` no vínculo;
- novo vínculo sempre nasce com treinamento `false`;
- conclusão do treinamento ocorre por operação explícita;
- vínculo finalizado não pode receber nova conclusão de treinamento;
- isolamento por tenant mantido.

Migration utilizada: `V28__add_academic_data_and_courses_to_internship_links.sql`.

### 6.3.2 — Cultura por participação em Atividade

Implementado e validado:

- catálogo `Cultura` por Unidade;
- Cultura com inativação lógica;
- associação explícita `VinculoEstagioAtividadeCultura`;
- cardinalidade N:N entre participação e Cultura;
- atualização idempotente do conjunto de Culturas de uma participação;
- Cultura inativa não pode ser adicionada como nova associação;
- Cultura inativa já associada pode permanecer visível historicamente;
- criação de nova participação aceita `culturaIds`;
- criação do primeiro vínculo aceita Culturas na participação inicial;
- primeiro cadastro de Estagiário aceita Culturas na participação inicial;
- respostas diretas e aninhadas retornam Culturas;
- cadeia validada:
  `EstagiarioResponseDTO → VinculoEstagioResponseDTO → participacoesAtividade → culturas`;
- o primeiro vínculo reutiliza o mesmo fluxo de `VinculoEstagioAtividadeService`, evitando regras duplicadas.

Migration utilizada: `V29__create_cultures_and_internship_activity_links.sql`.

## Regras de domínio consolidadas

```text
VinculoEstagio
├── formação
├── curso
├── treinamento de segurança
└── participações em Atividades
    └── Culturas
```

Formação e Curso continuam separados:

```text
Formação = enum institucional + OUTRO
Curso    = catálogo por Unidade
```

Cultura pertence ao contexto da participação, não globalmente ao Estagiário.

## Validação executada

Em 01/10/2026 o responsável pelo projeto confirmou:

```text
compilação backend     ✅
suíte completa         ✅ verde
erros de compilação    0
falhas reportadas      0
```

Os testes existentes foram ajustados às novas dependências e foram adicionados testes específicos para catálogo de Cultura e associação Cultura ↔ participação.

## Próximo bloco

```text
6.4 — período, situação, prorrogações e finalização
```

A próxima migration livre esperada é `V30`, caso o 6.4 exija alteração de schema.
