# Etapa 4.2 — ModeloResiduo

**Criado em:** 18/09/2026  
**Status:** 4.2-A — contrato/modelagem aprovado ✅  
**Branch:** feat/etapa-4-residuos  
**Próxima subetapa:** 4.2-B — V17 + entidades + repositories

---

## 1. Objetivo

Criar um catálogo reutilizável de **Modelos de Resíduo** por Unidade para representar resíduos recorrentes.

Regra central:

    ModeloResiduo
    = definição reutilizável/editável
    = catálogo da Unidade
    = pode mudar ao longo do tempo

    Residuo
    = ocorrência real
    = dados efetivamente informados naquele momento
    = histórico independente do modelo

A 4.2 cria e administra os modelos. O uso do modelo na tela **Informar Resíduo** pertence à 4.3.

---

## 2. Invariante histórica

Alterar, renomear ou inativar um modelo nunca pode alterar um Resíduo já criado.

Quando a 4.3 usar o modelo:

    ModeloResiduo
    → preenche sugestões no formulário
    → usuário revisa os dados
    → CriarResiduoRequestDTO
    → Residuo independente
    → snapshots atuais do domínio são gravados

Portanto, o Residuo não deve depender do ModeloResiduo para reconstruir seu histórico.

Na 4.2:

- não adicionar FK de Residuo para ModeloResiduo;
- não alterar CriarResiduoRequestDTO;
- não alterar Residuo;
- não alterar a tela Informar Resíduo.

---

## 3. Entidade principal proposta

    ModeloResiduo
    ├── id
    ├── publicId
    ├── unidade
    ├── nome
    ├── descricao
    ├── processoOrigem
    ├── estadoFisico
    ├── tratamentoRealizado
    ├── descricaoTratamento
    ├── recipiente
    ├── unidadeMedida
    ├── nivelRisco
    ├── riscos[]
    ├── classes[]
    ├── medidasSeguranca[]
    ├── observacaoSeguranca
    ├── componentes[]
    └── ativo

### 3.1 Campos

| Campo | Regra |
|---|---|
| publicId | UUID público imutável |
| unidade | obrigatório; define o tenant do modelo |
| nome | obrigatório; até 150 caracteres; único por Unidade de forma case-insensitive no service |
| descricao | descrição sugerida do Resíduo; obrigatória |
| processoOrigem | procedência/uso sugerido; obrigatório |
| estadoFisico | obrigatório |
| tratamentoRealizado | obrigatório |
| descricaoTratamento | obrigatória somente quando tratamento = true |
| recipiente | recipiente padrão sugerido; obrigatório |
| unidadeMedida | unidade padrão da ocorrência; obrigatória |
| nivelRisco | nível de risco sugerido; obrigatório |
| riscos | riscos sugeridos; conjunto obrigatório, seguindo as regras atuais do Resíduo |
| classes | pelo menos uma Classe de Resíduo ativa da mesma Unidade |
| medidasSeguranca | conjunto obrigatório; pode ser vazio quando não houver medida específica |
| observacaoSeguranca | opcional; obrigatória quando houver OUTRO |
| componentes | pelo menos um componente |
| ativo | controle lógico de disponibilidade; padrão true |

---

## 4. Componente do modelo

O componente reutilizável não deve reutilizar a entidade ComponenteResiduo, porque esta pertence à ocorrência real.

Criar entidade própria:

    ComponenteModeloResiduo
    ├── id
    ├── publicId
    ├── modeloResiduo
    ├── produto            opcional
    ├── nomeComponente
    ├── principal
    ├── concentracaoOuQuantidade
    └── observacao

Regras:

- produto é opcional;
- quando houver Produto, ele deve estar disponível para a Unidade do modelo;
- nomeComponente pode ser livre quando não houver Produto;
- deve existir produto ou nomeComponente;
- principal mantém a regra atual do Resíduo e não será obrigatório na 4.2;
- vínculo com Produto serve para composição/sugestão e nunca movimenta estoque.

---

## 5. Classes, Produtos e segurança

### Classes

ModeloResiduo referencia o catálogo atual de ClasseResiduo.

Na 4.2 não há snapshot dentro do modelo:

    ModeloResiduo
    → ClasseResiduo atual

    Residuo criado futuramente
    → ResiduoClasse
    → snapshot histórico

Na criação/edição do modelo, as classes devem:

- existir;
- estar ativas;
- pertencer à mesma Unidade do modelo.

### Produtos

Produtos referenciados em componentes devem estar disponíveis para a Unidade.

Inativar ou alterar Produto posteriormente não reescreve o modelo automaticamente.

### Segurança

O modelo mantém recomendações de segurança próprias:

    medidasSeguranca[]
    observacaoSeguranca

Ao usar o modelo na 4.3, essas informações serão copiadas para os campos informados da ocorrência e então passam a fazer parte do snapshot do Residuo.

---

## 6. Dados que não pertencem ao modelo

| Campo | Motivo |
|---|---|
| quantidade | varia a cada geração |
| usuarioGerador | pertence à ocorrência |
| laboratorio | contexto real do usuário/ocorrência |
| projeto | depende da geração específica |
| observacaoGerador | observação daquele evento |
| local de armazenamento | definido pela Gestão durante análise |
| complemento do local | pertence à ocorrência |
| destino final | decisão operacional da Gestão |
| gestor recebedor | pertence ao ciclo real |
| status/datas | ciclo operacional real |
| código SGL / QR | identidade do Resíduo real |
| risco confirmado | resultado da análise da Gestão |
| classes confirmadas | resultado da análise |
| segurança confirmada | resultado da análise |

### Quantidade x unidade

O modelo guarda unidadeMedida, mas não guarda quantidade.

Exemplo:

    Modelo:
    Resíduo de extração de DNA
    unidadeMedida = ML

    Ocorrência 1:
    250 ML

    Ocorrência 2:
    600 ML

---

## 7. Regras de tenant e ciclo de vida

    ModeloResiduo pertence a exatamente uma Unidade

Regras:

1. criação exige Unidade válida;
2. tenant ativo só acessa modelos da própria Unidade;
3. transferência de modelo para outra Unidade é proibida;
4. nome duplicado na mesma Unidade é rejeitado case-insensitive;
5. mesmo nome pode existir em Unidades diferentes;
6. DELETE será inativação lógica;
7. modelo inativo permanece consultável para administração, mas não será elegível para novos usos;
8. classes e Produtos usados na criação/edição devem ser válidos para a Unidade;
9. nenhuma operação da 4.2 movimenta estoque.

---

## 8. Dependências inativadas depois

Não haverá cascata automática quando uma Classe ou Produto usado pelo modelo for inativado.

Exemplo:

    Modelo ativo
    → usa Classe A

    Administrador inativa Classe A
    → modelo não é apagado
    → catálogo continua íntegro
    → uso futuro deve revalidar dependências na 4.3
    → administrador pode editar o modelo para corrigir

Na 4.2, criação e edição sempre exigem dependências ativas.

---

## 9. Estrutura planejada da V17

A migration ainda não é criada na 4.2-A. A modelagem aprovada servirá de base para a 4.2-B.

Estrutura prevista:

    modelos_residuo
    → id
    → public_id
    → unidade_id
    → nome
    → descricao
    → processo_origem
    → estado_fisico
    → tratamento_realizado
    → descricao_tratamento
    → recipiente
    → unidade_medida
    → nivel_risco
    → observacao_seguranca
    → ativo

    modelo_residuo_riscos
    → modelo_residuo_id
    → risco

    modelo_residuo_medidas_seguranca
    → modelo_residuo_id
    → medida

    modelo_residuo_classes
    → modelo_residuo_id
    → classe_residuo_id

    componentes_modelo_residuo
    → id
    → public_id
    → modelo_residuo_id
    → produto_id nullable
    → nome_componente
    → principal
    → concentracao_ou_quantidade
    → observacao

Índices/constraints mínimos previstos:

- UUID público único;
- nome único por (unidade_id, nome) no banco;
- validação case-insensitive adicional no service;
- índice por unidade_id;
- FKs para Unidade, ClasseResiduo e Produto.

A V17 será imutável depois de aplicada.

---

## 10. Limites da 4.2

A 4.2 entrega:

    cadastro de ModeloResiduo
    CRUD + tenant
    componentes
    classes
    riscos
    segurança
    ativação/inativação
    Administração/Cadastros

A 4.2 não entrega:

    selecionar modelo ao informar Resíduo
    preencher automaticamente formulário do Solicitante
    gravar modeloId no Residuo
    converter modelo em ocorrência dentro do backend

Esses pontos pertencem à **4.3 — uso de modelo ou preenchimento manual**.

---

## 11. Subetapas

    4.2-A contrato/modelagem                    ✅ aprovado
    4.2-B V17 + entidades + repositories        ⏳
    4.2-C CRUD + tenant + validações            ⏳
    4.2-D testes/revisão backend                ⏳
    4.2-E Administração/Cadastros frontend      ⏳
    4.2-F validação e fechamento                ⏳

Critério de fechamento da 4.2-A — atendido em 18/09/2026:

- campos do modelo aprovados;
- campos explicitamente excluídos aprovados;
- regra de snapshot aprovada;
- tenant/ciclo de vida aprovados;
- separação entre 4.2 e 4.3 aprovada;
- estrutura conceitual da V17 aprovada.

---

## 12. Próximo passo após aprovação

Modelagem aprovada. Próxima subetapa:

    4.2-B
    → V17__create_residue_models.sql
    → ModeloResiduo.java
    → ComponenteModeloResiduo.java
    → ModeloResiduoRepository.java

Sem Service, Controller, DTOs, alterações em Residuo ou frontend nesta subetapa.
