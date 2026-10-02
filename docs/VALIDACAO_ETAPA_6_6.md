# Validação — Etapa 6.6

Data de preparação: 02/10/2026

Status: **VALIDADO — Etapa 6.6 concluída em 02/10/2026**.

## Objetivo

Fechar a Etapa 6 — Estagiários e Vínculos com uma regressão automatizada concentrada nas regras adicionadas/refinadas nos blocos 6.1–6.5, sem antecipar a Etapa 12 de testes automatizados de frontend.

## Cobertura automatizada preparada

### Vínculo e Bolsa

`VinculoEstagioServiceTest`

Cobertura final inclui:

- criação local com primeira Atividade;
- criação institucional sem Atividade inicial;
- idempotência da criação institucional;
- obrigatoriedade de término previsto;
- edição local do vínculo;
- bloqueio de troca de tipo de Bolsa pelo `Editar vínculo`;
- prorrogação da Bolsa atual;
- histórico auditável da prorrogação;
- bloqueio de prorrogação sem aumento da data final;
- nova Bolsa local na data de corte;
- encerramento da ocorrência anterior;
- criação de novo `VinculoEstagio`;
- movimentação de participação iniciada no ponto de corte;
- divisão de participação que atravessa a troca de Bolsa;
- preservação de Cultura na continuidade da participação;
- bloqueio de nova Bolsa local com início futuro.

### Participações em Atividades

`VinculoEstagioAtividadeServiceTest`

Cobertura final inclui:

- associação válida;
- bloqueio de participação ativa duplicada;
- vínculo finalizado;
- período do vínculo;
- histórico;
- encerramento;
- regra da última participação ativa;
- Culturas;
- correção administrativa de participação ativa;
- bloqueio de edição de participação encerrada;
- uso da data final do Projeto quando a Atividade não possui `dataFim`;
- exposição de `atividadeDataInicio`, `atividadeDataFim` e `projetoDataFim`.

### Projeto / Atividade

`AtividadeServiceTest`

Além da regressão da Etapa 5, foi acrescentada cobertura explícita para:

- impedir `Atividade.dataFim > Projeto.dataFim`, inclusive quando o SCI não possui data final própria.

### Curso

Novos testes:

- `CursoServiceTest`;
- `CursoControllerTest`.

Cobertura:

- criação no catálogo da Unidade;
- normalização de nome;
- duplicidade por Unidade;
- isolamento de tenant;
- listagem de ativos;
- inativação lógica sem exclusão histórica;
- contrato HTTP usado pela opção `Outro / adicionar novo curso`.

### Observações e treinamento de segurança

Novos testes:

- `ObservacaoVinculoEstagioServiceTest`;
- `ObservacaoVinculoEstagioControllerTest`.

Cobertura:

- observação operacional auditável;
- conclusão do treinamento;
- reversão do treinamento;
- evento `TREINAMENTO_CONCLUIDO`;
- evento `TREINAMENTO_REVERTIDO`;
- bloqueio quando não há mudança de estado;
- autorização somente para GESTOR/ADMINISTRADOR;
- tenant obrigatório;
- contratos HTTP de observações e treinamento.

### Controllers de vínculo e participação

`VinculoEstagioControllerTest` passou a cobrir:

- `PUT /api/v1/vinculos-estagio/{vinculoId}/prorrogar-bolsa`;
- `POST /api/v1/vinculos-estagio/{vinculoId}/nova-bolsa`;
- validação de payload dos dois fluxos.

`VinculoEstagioAtividadeControllerTest` passou a cobrir:

- `PUT /api/v1/vinculos-estagio/participacoes/{participacaoId}`.

## Bateria de execução

### 1. Backend — regressão focada da Etapa 6

No repositório backend:

```bash
./mvnw -Dtest=VinculoEstagioServiceTest,VinculoEstagioAtividadeServiceTest,AtividadeServiceTest,CursoServiceTest,ObservacaoVinculoEstagioServiceTest,VinculoEstagioControllerTest,VinculoEstagioAtividadeControllerTest,CursoControllerTest,ObservacaoVinculoEstagioControllerTest test
```

Critério:

```text
Tests run: ...,
Failures: 0,
Errors: 0,
Skipped: 0
BUILD SUCCESS
```

### 2. Backend — suíte completa

```bash
./mvnw clean test
```

Critério obrigatório para encerramento:

```text
Failures: 0
Errors: 0
BUILD SUCCESS
```

### 3. Frontend — validação automática disponível nesta etapa

O frontend ainda não possui framework de testes unitários/componentes; essa expansão permanece planejada para a Etapa 12. Para o fechamento da Etapa 6 executar:

```bash
npm run type-check
npm run build
```

Critérios:

- `vue-tsc --noEmit` sem erros;
- build Vite concluído;
- nenhum erro de TypeScript introduzido pelo drawer, fluxo de Bolsa, Curso ou participação.

## Critério final para marcar a Etapa 6 como concluída

A Etapa 6 só deve ser marcada como encerrada quando:

1. regressão focada da Etapa 6 estiver verde;
2. suíte backend completa estiver verde;
3. frontend `type-check` estiver verde;
4. frontend `build` estiver verde;
5. polimento manual já validado permanecer sem regressão.

## Resultado final

Validação local confirmada em 02/10/2026: **100% verde**.

- regressão automatizada da Etapa 6 validada;
- correção dos mocks de `HistoricoSincronizacaoVinculoEstagioRepository` em `EstagiarioServiceTest` validada;
- suíte automatizada final sem falhas informadas;
- polimento funcional/visual aprovado;
- Etapa 6 autorizada para fechamento e merge.

**Etapa 6 — Estagiários e Vínculos: CONCLUÍDA E VALIDADA.**
