# Validação — Etapa 6.2 — Vínculos Estagiário ↔ Atividade

**Data de validação:** 01/10/2026  
**Branch:** `collab/etapa-6-estagiarios-vinculos`  
**Status:** ✅ concluído e validado

## Escopo fechado

O 6.2 consolidou a relação histórica entre o vínculo institucional de estágio e as Atividades da Etapa 5.

```text
Usuario
└── Estagiario
    └── VinculoEstagio 1..N
        └── VinculoEstagioAtividade 1..N
            └── Atividade
                └── SCI
                    └── Projeto
                        └── Laboratorio
                            └── Unidade
```

## Implementação

- V27 criou `vinculo_estagio_atividade`;
- associação possui UUID público, vínculo, Atividade, início/fim de participação e observação;
- histórico é preservado; reentrada futura na mesma Atividade não é proibida pelo schema;
- duas participações simultaneamente abertas para o mesmo vínculo/Atividade são bloqueadas no Service;
- um vínculo pode possuir várias Atividades simultâneas;
- encerrar uma participação não encerra o vínculo institucional;
- a última participação aberta não pode ser encerrada isoladamente enquanto o vínculo estiver em andamento;
- novo vínculo para Estagiário existente nasce com primeira Atividade na mesma transação;
- primeiro vínculo criado por `POST /estagiarios` também exige Atividade inicial;
- **evolução posterior do 6.4:** essa exigência permanece correta para o fluxo local/DEV validado no 6.2, mas não é regra absoluta para vínculos recebidos do ambiente institucional; vínculos sincronizados podem existir sem Atividade até receberem contexto operacional.
- dois vínculos não finalizados simultâneos para o mesmo Estagiário são bloqueados;
- Projeto/SCI/Laboratório não são duplicados no vínculo; são derivados da Atividade.

## Estado operacional

```text
Estagiário ativo operacionalmente
=
Usuario ativo
+ VinculoEstagio não FINALIZADO
+ >= 1 VinculoEstagioAtividade aberta
```

A listagem de ativos e o campo `ativo` do response seguem essa regra.

Desativar tecnicamente Projeto/SCI/Atividade não encerra silenciosamente o estágio histórico.

## Endpoints

```text
POST /api/v1/vinculos-estagio/{vinculoId}/atividades
GET  /api/v1/vinculos-estagio/{vinculoId}/atividades
GET  /api/v1/vinculos-estagio/{vinculoId}/atividades/ativas
PUT  /api/v1/vinculos-estagio/participacoes/{participacaoId}/encerrar
POST /api/v1/vinculos-estagio/estagiarios/{estagiarioId}
```

## Testes adicionados/ajustados

- `EstagiarioServiceTest`;
- `EstagiarioControllerTest`;
- `EstagiarioRepositoryTest`;
- `VinculoEstagioAtividadeServiceTest`;
- `VinculoEstagioServiceTest`;
- `VinculoEstagioAtividadeControllerTest`;
- `VinculoEstagioControllerTest`;
- `VinculoEstagioAtividadeRepositoryTest`.

Cobertura principal:

- primeira Atividade obrigatória;
- criação de novo vínculo histórico;
- vínculo simultâneo bloqueado;
- tenant;
- orientador;
- Atividade operacional;
- períodos;
- participação duplicada;
- histórico;
- múltiplas participações;
- encerramento;
- proteção da última participação;
- estado operacional ativo;
- contratos HTTP;
- queries JPA encadeadas validadas com H2.

## Resultado da validação

Em 01/10/2026 o responsável do projeto executou:

- suíte completa de testes do backend;
- compilação completa do sistema.

Resultado informado:

```text
erros: 0
falhas: 0
compilação: sucesso
```

O bloco 6.2 está oficialmente encerrado.

## Próxima retomada

```text
6.3 — dados acadêmicos, tipo de vínculo, Cultura
      e treinamento de segurança
```

Não antecipar encerramento/prorrogações do vínculo; esse fluxo pertence ao 6.4.
