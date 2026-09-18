# Continuidade SGL — Etapa 4

**Atualizado em:** 18/09/2026  
**Etapa anterior:** Etapa 3 — Refinamentos do fluxo atual de Resíduos ✅ concluída e validada  
**Etapa atual:** Etapa 4 — Expansão operacional de Resíduos 🔧  
**Subetapa atual:** 4.2-B — V17 + entidades + repositories  
**Próxima implementação:** 4.2-B — implementação manual pelo responsável com referência do assistente  
**Branch:** `feat/etapa-4-residuos`

## 1. Antes de continuar

Ler nesta ordem:

```text
CONTINUIDADE.md
docs/PLANO_PRE_PRODUCAO.md
este arquivo
docs/MODULO_RESIDUOS.md
docs/DOSSIE_PROJETO_SGL.md
docs/FLUXO_DO_SISTEMA.md
```

Também revisar no frontend:

```text
gbsalermo/SGL-FRONTEND/CONTINUIDADE.md
```

A Etapa 3 já foi integrada à `main` nos dois repositórios e a branch `feat/etapa-4-residuos` já foi criada a partir da `main` atualizada.

---

## 2. Regra de trabalho

O responsável do projeto quer aprender e implementar o backend manualmente.

Portanto:

```text
backend funcional
→ IA analisa
→ explica a modelagem
→ fornece passos e código de referência
→ usuário implementa manualmente
→ IA revisa o resultado
```

Não aplicar diretamente código funcional de backend sem autorização explícita.

Frontend/documentação podem ser alterados diretamente quando o usuário autorizar, sempre explicando antes o impacto.

Executar em passos pequenos, com commits lógicos e sem antecipar etapas futuras.

---

## 3. Estado atual do domínio de Resíduos

Fluxo preservado ao final da Etapa 3:

```text
INFORMADO
→ EM_ANALISE
→ LIBERADO_PARA_ARMAZENAMENTO
→ ARMAZENADO_TEMPORARIAMENTE
→ DESPACHADO
```

Código SGL:

```text
SGL-RES-AAAA-NNNNNN
```

A identificação existe desde a criação. Código e QR técnico existem desde o registro inicial.

Regra de prévia/impressão:

```text
INFORMADO / EM_ANALISE
→ prévia disponível
→ impressão bloqueada

LIBERADO_PARA_ARMAZENAMENTO ou posterior
→ impressão permitida
```

O QR técnico pertence à identificação/contrato. O template físico atual do frontend pode não renderizá-lo; o padrão final e Zebra permanecem na Etapa 10.

Produto continua diferente de Resíduo:

```text
Produto = catálogo/estoque
Residuo = ocorrência operacional real
```

Componentes podem referenciar Produtos apenas para rastreabilidade/sugestões. Isso não movimenta estoque automaticamente.

---

## 4. Dados consolidados na Etapa 3

O Resíduo já possui:

- descrição;
- `processoOrigem`, exibido como Procedência / uso do Resíduo;
- estado físico;
- tratamento realizado + descrição;
- recipiente;
- quantidade/unidade;
- componentes;
- risco informado e risco confirmado;
- classes informadas e classes confirmadas;
- segurança/EPI informada e confirmada;
- observações do Solicitante e da Gestão;
- Gestor recebedor inicial;
- histórico de todas as transições;
- código SGL;
- QR técnico;
- armazenamento temporário textual;
- destino previsto/confirmado.

Migrations relevantes já aplicadas:

```text
V11 — módulo de Resíduos
V12 — backfill Código SGL
V13 — estado físico, tratamento e responsabilidade inicial
V14 — Classes de Resíduo
V15 — segurança/EPI
```

Migrations aplicadas são imutáveis. A V16 foi aplicada na 4.1-A; qualquer nova alteração de schema deve usar V17 ou superior.

---

## 5. Snapshot — regra arquitetural

Dados históricos de uma ocorrência real não devem depender de cadastros mutáveis.

Exemplos já implementados:

```text
ClasseResiduo = catálogo atual/editável
ResiduoClasse = snapshot histórico da classificação

Produto = recomendações atuais de segurança
Residuo = segurança efetivamente informada/confirmada naquela ocorrência
```

A Etapa 4.1 seguirá a mesma regra para local de armazenamento.

---

# 6. Escopo da Etapa 4

```text
4.1 Locais de armazenamento cadastráveis          ✅ concluída
→ 4.2 Modelos de Resíduos pré-cadastrados          🔧 atual
→ 4.3 Uso de modelo ou preenchimento manual        ⏳
→ 4.4 Correções administrativas do ciclo           ⏳
```

A 4.1 foi concluída. Durante a 4.2, não antecipar a 4.3/4.4 antes de fechar o contrato de `ModeloResiduo`.

---

# 7. Etapa 4.1 — decisão arquitetural aprovada

Objetivo: substituir a dependência exclusiva de texto livre por locais reutilizáveis, sem perder flexibilidade nem histórico.

Modelagem aprovada:

```text
LocalArmazenamentoResiduo
= catálogo atual/editável por Unidade

Residuo.localArmazenamentoResiduo
= referência opcional ao catálogo

Residuo.complementoLocalArmazenamento
= complemento opcional da ocorrência

Residuo.localArmazenamentoTemporario
= snapshot textual histórico completo
```

Exemplos:

```text
Catálogo:
Almoxarifado Químico

Catálogo + complemento:
Almoxarifado Químico - Prateleira B2

Manual:
Área externa provisória junto ao abrigo técnico
```

Regras fechadas:

1. `LocalArmazenamentoResiduo` pertence obrigatoriamente a uma `Unidade`.
2. O catálogo é mutável e possui `ativo`.
3. Local inativo não deve aparecer em novas seleções.
4. Inativação não pode quebrar Resíduos antigos.
5. Alterar o nome do catálogo futuramente não modifica o snapshot histórico do Resíduo.
6. `localArmazenamentoTemporario` continua sendo o texto histórico completo e continua atendendo rótulo/relatório.
7. O vínculo estruturado com o catálogo será opcional para manter compatibilidade com caminho manual e resíduos legados.
8. O complemento é opcional e pertence à ocorrência, não ao catálogo.
9. Modo catálogo: `localArmazenamentoId` preenchido, complemento opcional, texto manual ausente.
10. Modo manual: `localArmazenamentoId` ausente, complemento ausente e texto manual obrigatório.
11. Payload ambíguo com catálogo e texto manual simultaneamente deve ser rejeitado.
12. Lookup do catálogo deve validar UUID + Unidade do Resíduo e `ativo=true` para novas seleções.
13. A análise/liberação define o local planejado.
14. A confirmação física pode manter ou corrigir o local.
15. Correção física deve permanecer rastreável no histórico.
16. Não fazer refactor amplo de `Residuo` durante a 4.1.
17. Não alterar rótulo/relatório inicialmente: ambos podem continuar usando `localArmazenamentoTemporario`.
18. Validar comprimento do snapshot final para respeitar o limite existente do campo.

---

# 8. Plano de implementação da 4.1

## 4.1-A — Fundação do catálogo no backend ✅

Implementado nesta subetapa:

```text
V16__create_residue_storage_locations.sql
LocalArmazenamentoResiduo.java
LocalArmazenamentoResiduoRepository.java
```

Estrutura aplicada pela V16:

```text
nova tabela locais_armazenamento_residuo
→ id
→ public_id
→ unidade_id
→ nome
→ ativo

residuos
→ local_armazenamento_residuo_id nullable
→ complemento_local_armazenamento nullable
```

Critérios de saída cumpridos: Flyway aplicou V16, `ddl-auto=validate` passou e a aplicação subiu normalmente. O escopo adicional foi tratado nas subetapas seguintes.

## 4.1-B — CRUD + tenant ✅

Implementado request/response/service/controller e validado:

- criar;
- listar;
- listar ativos;
- editar;
- inativar;
- duplicidade por Unidade;
- acesso fora da Unidade.

## 4.1-C — Integração com análise/liberação ✅

Referência estruturada e complemento foram adicionados ao `Residuo`, o DTO de análise foi adaptado e o modo catálogo x manual foi validado com casos válidos e inválidos.

## 4.1-D — Confirmação física/correção ✅

A confirmação de armazenamento foi adaptada para manter ou corrigir o local planejado e registrar a mudança no histórico.

## 4.1-E — Revisão backend ✅

Revisão concluída em 18/09/2026. O contrato backend da 4.1 foi fechado após validação do catálogo, tenant, integração com análise/liberação, confirmação física/correção, snapshot histórico e histórico de mudança. Também foi adicionado teste automatizado de domínio para proteger as regras centrais de armazenamento.

## 4.1-F — Frontend Administração/Cadastros ✅

Catálogo integrado à central administrativa com listagem, busca, criação, edição e ativação/inativação.

## 4.1-G — Frontend Gestão ✅

Na análise e confirmação física, permitir:

```text
local cadastrado
+ complemento
ou
local manual
```

## 4.1-H — Regressão integrada ✅

Concluída em 18/09/2026. Foram validados o catálogo administrativo, seleção de local cadastrado/manual na análise, complemento, confirmação física, correção do local e histórico planejado x confirmado. As validações de regra do backend já haviam sido cobertas por Postman e teste de domínio. A auditoria tipográfica global do frontend foi registrada para a Etapa 13 e não bloqueia o fechamento funcional da 4.1.

---

# 9. Etapas 4.2–4.4 — sequência atual

## 4.2 — ModeloResiduo

Contrato canônico da modelagem: `ETAPA_4_2_MODELO_RESIDUO.md`.

```text
4.2-A contrato/modelagem                    ✅ aprovado
4.2-B V17 + entidades + repositories        ⏳
4.2-C CRUD + tenant + validações            ⏳
4.2-D testes/revisão backend                ⏳
4.2-E Administração/Cadastros frontend      ⏳
4.2-F validação e fechamento                ⏳
```

Modelo pode sugerir descrição, origem/uso, composição, Produtos, classes, riscos, EPI, recipiente e tratamento padrão. Alterar modelo não pode alterar Resíduos históricos. A 4.2 não cria vínculo com `Residuo`; isso pertence à 4.3.

## 4.3 — Uso pelo Solicitante

Na tela Informar Resíduo, permitir escolha entre modelo pré-cadastrado e preenchimento manual. O modelo preenche sugestões; a ocorrência permanece independente.

## 4.4 — Correções administrativas

Avaliar para Administrador:

```text
Cancelar Resíduo
→ justificativa obrigatória
→ preservar registro/histórico

Retornar para análise/liberação
→ justificativa obrigatória
→ preservar eventos anteriores
→ exigir nova validação
→ reavaliar permissão de impressão
```

Antes de implementar, fechar status permitidos, irreversibilidade de `DESPACHADO`, eventual `CANCELADO`, comportamento dos dados confirmados, novo evento de liberação, efeito no rótulo e permissões.

Não confundir com delete lógico; decisão geral continua na Etapa 11.

---

## 10. Fora do escopo da Etapa 4

Não antecipar:

- Projetos/Atividades → Etapa 5;
- evolução institucional de Estagiários → Etapa 6;
- Relatórios consolidados → Etapa 7;
- normalização g/mL/unidades e Soluções → Etapa 8;
- Soluções em Pedidos → Etapa 9;
- Zebra/template final/infraestrutura física → Etapa 10;
- Manual e decisão geral de delete lógico → Etapa 11;
- testes automatizados frontend → Etapa 12;
- refactor final de classes grandes → Etapa 13.

---

## 11. Observação sobre tamanho das classes

`Residuo.java` cresceu significativamente durante a Etapa 3.

Não realizar refactor estrutural grande agora apenas para reduzir linhas. A Etapa 13 já foi reservada para revisar `Residuo`, Services, DTOs, Controllers, métodos longos e organização de packages, preservando contratos e comportamento.

---

## 12. Próximo passo real

```text
4.2 — ModeloResiduo
```

A 4.1 está concluída e validada. A 4.2-A foi aprovada. O foco atual é a 4.2-B; a 4.2-C seguirá o mesmo padrão de implementação manual pelo responsável. Não antecipar a 4.2-D antes da revisão dessas duas subetapas.
