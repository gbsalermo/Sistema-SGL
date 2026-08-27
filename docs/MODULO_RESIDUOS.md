# Módulo experimental de resíduos laboratoriais

**Branch:** `feat/gestao-residuos`

Este módulo foi criado como experimento isolado para representar o fluxo real descrito para resíduos laboratoriais sem alterar a lógica de estoque de produtos.

## Regra central de domínio

```text
Produto != Resíduo
```

`Produto` continua sendo o catálogo de materiais e insumos do SGL.

`Resíduo` representa um material gerado pelo laboratório e encaminhado à gestão para conferência, rotulagem, armazenamento temporário e destinação final.

Um resíduo pode possuir um ou vários componentes. Cada componente pode:

- referenciar opcionalmente um `Produto` já existente no catálogo;
- existir apenas como nome livre, quando não houver produto correspondente cadastrado;
- ser marcado como componente principal;
- registrar concentração ou quantidade aproximada em texto livre.

A associação `ComponenteResiduo -> Produto` é apenas informativa e de rastreabilidade. **Ela não consulta, baixa, repõe ou altera `EstoqueCentral`, `Lote` ou `MovimentacaoEstoque`.**

Exemplos válidos:

```text
Resíduo A
└── Acetona

Resíduo B - Extração de DNA
├── Metanol
├── Acetonitrila
├── Ácido clorídrico
└── outro reagente sem cadastro no catálogo
```

## Riscos

O laboratório informa inicialmente:

```text
nivelRiscoInformado
riscosInformados[]
```

A gestão não sobrescreve silenciosamente essa informação. Após conferência, registra separadamente:

```text
nivelRiscoConfirmado
riscosConfirmados[]
```

Isso preserva o que foi declarado na origem e o que foi tecnicamente validado pelo responsável.

`TipoRisco` passou a aceitar múltiplas classificações úteis à rotulagem, como inflamável, tóxico, corrosivo, irritante, perigo à saúde, oxidante, explosivo, gás pressurizado e perigo ambiental.

## Fluxo implementado

```text
INFORMADO
   ↓ receber
EM_ANALISE
   ↓ analisar-liberar
LIBERADO_PARA_ARMAZENAMENTO
   ↓ armazenar
ARMAZENADO_TEMPORARIAMENTE
   ↓ despachar
DESPACHADO
```

### 1. Laboratório informa o resíduo

`POST /api/v1/residuos`

Informa:

- laboratório e usuário gerador;
- projeto opcional;
- descrição do resíduo;
- processo/procedência que o gerou;
- recipiente físico;
- quantidade e unidade;
- riscos inicialmente percebidos;
- composição do resíduo.

### 2. Gestão recebe

`PUT /api/v1/residuos/{id}/receber`

Registra que o recipiente chegou à gestão e entrou em conferência.

### 3. Gestão confere e libera

`PUT /api/v1/residuos/{id}/analisar-liberar`

A gestão:

- confirma ou corrige a classificação de risco;
- informa o local de armazenamento temporário;
- informa o destino final previsto;
- define data prevista de despacho;
- libera o resíduo para rotulagem.

Nesse ponto o sistema gera:

```text
codigoRastreio = SGL-RES-AAAA-NNNNNN
qrCodeConteudo = SGL-RESIDUO:<UUID público>
```

O backend retorna o conteúdo do QR. A renderização gráfica/impressão do QR fica para o frontend ou para um gerador de PDF posterior.

### 4. Rótulo

`GET /api/v1/residuos/{id}/rotulo`

O endpoint consolida:

- código de rastreio;
- conteúdo do QR;
- descrição;
- laboratório e gerador;
- processo de origem;
- recipiente e quantidade;
- riscos confirmados;
- componentes;
- local temporário;
- destino previsto e data prevista de despacho.

### 5. Armazenamento temporário

`PUT /api/v1/residuos/{id}/armazenar`

Confirma fisicamente a entrada do recipiente no local temporário e registra a data/hora.

### 6. Despacho

`PUT /api/v1/residuos/{id}/despachar`

Confirma a saída física do resíduo e registra o destino final efetivo e a data/hora do despacho.

## Histórico

Cada mudança operacional registra um evento em `historico_residuo`.

`GET /api/v1/residuos/{id}/historico`

O histórico preserva:

- usuário responsável pela ação;
- status resultante;
- ação;
- observação;
- data/hora.

## Decisões propositais deste experimento

1. Resíduo não participa do saldo de estoque.
2. Referenciar um produto em um componente não significa consumo daquele produto.
3. Produto não precisa existir no catálogo para que um componente seja informado.
4. Riscos informados pelo laboratório e confirmados pela gestão são mantidos separadamente.
5. O rótulo só fica disponível depois da conferência da gestão.
6. O QR identifica o resíduo por UUID público; a imagem do QR não é armazenada no banco.
7. Somente `GESTOR` e `ADMINISTRADOR` podem executar as transições de gestão nesta versão experimental, enquanto a autenticação real ainda não foi integrada.

## Migration

A estrutura foi adicionada em:

```text
V5__create_residuo_module.sql
```

As migrations anteriores permanecem intocadas.

## Estado

Este módulo ainda deve ser validado antes de qualquer merge para `main`.

Validações recomendadas:

```text
mvn clean test
subida do PostgreSQL com V1 -> V5
Swagger UI
fluxo completo no Postman
rótulo com resíduo de componente único
rótulo com mistura de vários componentes
componente vinculado a Produto sem alteração de estoque
componente livre sem Produto cadastrado
comparação riscos informados x riscos confirmados
histórico completo até DESPACHADO
```
