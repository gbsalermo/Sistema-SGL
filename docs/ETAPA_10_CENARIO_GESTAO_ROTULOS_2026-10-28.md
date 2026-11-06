# Etapa 10 — Cenário integrado para validação de rótulos

**Estado:** cenário de teste publicado na branch `collab/etapa-10-rotulos-impressao`; inicialização, build e validação funcional local ainda dependem da execução pelo mantenedor.

## Objetivo

Simular um mês de gestão laboratorial, usando o cenário DEMO já existente e complementando os casos que exercitam o rótulo de Resíduos e a consulta de Produtos. A massa demonstra a relação entre Unidade, laboratórios, usuários, Projetos/SCI/Atividades, estoque físico, lotes, movimentações, Pedidos de Produtos, Pedidos de Soluções e Resíduos em vários estados.

**Ambiente isolado, não produção:** somente o profile `demo` aciona `DemoDataInitializer`; os dados ficam no H2 em memória, descartados ao desligar o backend. Não injeta nada no PostgreSQL do profile `dev`; não é migration e não altera regras operacionais. Os dados são fictícios, não constituem recomendações de segurança química.

## Iniciar

No diretório **raiz do backend** (`Sistema-SGL`, onde está o `pom.xml`):

```bash
git switch collab/etapa-10-rotulos-impressao
git pull --ff-only github collab/etapa-10-rotulos-impressao
./mvnw clean verify
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

No PowerShell, o executável é `./mvnw.cmd`. Iniciar o frontend normalmente, apontando para a API local. **Não iniciar com os profiles `demo,dev` juntos:** o `demo` é um ambiente substituto e isolado do DEV. A carga não estará disponível em uma sessão que continua usando o backend `dev`.

Usuários fictícios para a interface (senha de demo: `123456`):
- Gestor: `ricardo.almeida@demo.sgl.local`
- Administradora: `mariana.souza@demo.sgl.local`
- Solicitante: `paulo.nascimento@demo.sgl.local`
- Estagiária: `ana.barbosa@demo.sgl.local`

**Datas:** relativas ao momento do boot, para manter um cenário recente. Código de rótulo é gerado pelo banco; filtre pelo **nome/descrição**, e não por um ID inventado neste roteiro.

## Fluxo integrado preexistente

- **Unidade:** CNPMF — Embrapa Mandioca e Fruticultura.
- **Sete laboratórios/áreas:** Biologia Molecular, Fitopatologia, Entomologia, Solos e Nutrição, Virologia, Ecofisiologia e Central de Soluções.
- **Catálogo e lotes:** itens por unidade, kit/caixa/frasco/garrafa, itens sob controle, lote vencido, lote perto do vencimento e estoque crítico.
- **Projetos:** projetos por laboratório com SCI/Atividades nos dados de demonstração.
- **Pedidos:** pendentes (inclusive urgentes), aprovados, entregues, rejeitados e cancelados.
- **Movimentações:** entradas, saídas por pedido, ajustes, devoluções e descartes, com histórico de um mês.
- **Resíduos:** Informado, Em análise, Liberado para armazenamento, Armazenado temporariamente e Despachado, com histórico de operação.

## Adições para a validação de rótulos

### Pedidos de Soluções

1. **Solução de limpeza laboratorial — preparo sob demanda**: pedido **ENTREGUE**, fora do catálogo, referente à movimentação de hipoclorito já existente na Central de Soluções (não cria outra saída).
2. **Tampão analítico demonstrativo**: modelo opcional de solução registrado no catálogo de soluções, com componentes associados a produtos existentes; pedido **PENDENTE** da Virologia.
3. **Tampão analítico demonstrativo**: pedido **EM_PREPARACAO** da Biologia Molecular. Não cria saída de estoque antes da entrega.

Esses registros são do domínio de **Pedidos de Soluções**; **não existe ainda rótulo específico de Soluções na Etapa 10**. Servem para testar o contexto da gestão enquanto esse modelo de rótulo é planejado.

### Resíduos com condições variadas para a etiqueta

| Localizar na lista pelo texto | Situação | O que avaliar no rótulo |
|---|---|---|
| Mistura de tampões e solventes da extração de DNA vegetal | INFORMADO | Data recente, classificação preliminar, dois riscos, medidas informadas, observação de segurança, bloqueio de impressão |
| Solução com formaldeído utilizada na fixação | EM_ANALISE | Texto de composição, riscos e responsável, impressão ainda bloqueada |
| Meio de cultura utilizado com crescimento fúngico inativado | LIBERADO_PARA_ARMAZENAMENTO | Riscos e medidas confirmadas, data de liberação, habilitação da impressão |
| Mistura de soluções de lavagem e substrato após ELISA | ARMAZENADO_TEMPORARIAMENTE | Múltiplos componentes e contexto de armazenamento |
| Mistura de metanol e etanol | ARMAZENADO_TEMPORARIAMENTE | Várias substâncias, mais de uma medida, nome/observação extensos |
| Soluções vencidas e consumíveis líquidos de PCR | DESPACHADO | Rastreabilidade histórica de etiqueta de resíduo despachado |
| Resíduo sólido de materiais de apoio a análises de rotina | LIBERADO_PARA_ARMAZENAMENTO | **Estado SÓLIDO**, nenhum pictograma registrado, nome e destino longos, impressão habilitada |
| Substrato cromogênico vencido separado para descarte | INFORMADO | Etiqueta curta e campos opcionais ausentes |

Os resíduos DEMO originais foram enriquecidos com `EstadoFisicoResiduo.LIQUIDO` e medidas registradas em conformidade com os campos existentes. Para registros confirmados, as medidas e observações confirmadas são armazenadas separadamente; para registros informados/em análise, permanecem preliminares. O novo resíduo sólido usa uma apresentação distinta. A **classificação e segurança da massa são puramente ilustrativas**.

## Checklist de validação

1. **Gestão:** entrar como gestor, conferir Dashboard, Pedidos e Soluções, Estoque e Lotes, Movimentações, Resíduos e relatórios com os mesmos laboratórios/produtos.
2. **Rótulo preliminar:** abrir um resíduo INFORMADO e outro EM_ANALISE. Verificar data de informação, estado físico, riscos, medidas, observações e botão Imprimir bloqueado.
3. **Rótulo liberado:** abrir LIBERADO, ARMAZENADO e DESPACHADO. Verificar riscos/medidas confirmados e autorização de impressão.
4. **Casos de layout:** testar nomes curtos e longos, 1/2+ pictogramas, nenhum pictograma, campos nulos, composição longa, observação extensa e unidade física.
5. **Tamanhos:** testar prévia e impressão de 90, 135, 180 e 190 mm, verificando que todas as informações e logos caibam no papel escolhido. A etiqueta pode **crescer em altura**; o tamanho máximo da mídia deve ser considerado.
6. **Produtos:** abrir os rótulos atuais de um produto comum, um produto fiscalizado e um kit, comparando com seus cadastros; rótulo de Produto não deve alegar um lote/recipiente específico sem contexto de lote.
7. **Regressão:** `./mvnw clean verify` e `npm run build`; verificar ausência de erros de inicialização no profile `demo`.

## Limites importantes

- Esta é uma **massa de demonstração por snapshots**: as movimentações do cenário não substituem testes completos de FEFO/FIFO/concorrência ou auditoria física detalhada por recipiente.
- Pedidos EM_PREPARACAO não consomem estoque pela fixture. O pedido entregue de Solução reaproveita a movimentação já registrada.
- Dados isolados do profile `demo` são recriados a cada inicialização e não aparecem no `dev` por padrão.
- Nenhum merge ou alteração de dados da produção foi realizado. Após validação, registrar resultados e sincronizar o planejamento da Etapa 10.
