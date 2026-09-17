# Continuidade SGL — Etapa 4

**Criado em:** 17/09/2026  
**Etapa anterior:** Etapa 3 — Refinamentos do fluxo atual de Resíduos ✅ concluída e validada  
**Próxima etapa:** Etapa 4 — Expansão operacional de Resíduos

## 1. Antes de iniciar

Ler nesta ordem:

```text
CONTINUIDADE.md
docs/PLANO_PRE_PRODUCAO.md
docs/CONTINUIDADE_ETAPA_3_2026-09-11.md
docs/MODULO_RESIDUOS.md
este arquivo
```

Também revisar no frontend:

```text
gbsalermo/SGL-FRONTEND/CONTINUIDADE.md
```

Confirmar que a Etapa 3 foi integrada à `main` nos dois repositórios antes de abrir a branch da Etapa 4.

Branch sugerida:

```text
feat/etapa-4-residuos
```

Criar a branch a partir da `main` atualizada. Não continuar a Etapa 4 sobre uma branch antiga da Etapa 3.

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

A identificação existe desde a criação.

A prévia do rótulo pode ser aberta em `INFORMADO` e `EM_ANALISE`, mas a impressão só é permitida a partir de `LIBERADO_PARA_ARMAZENAMENTO`.

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
- armazenamento temporário;
- destino previsto/confirmado.

Migrations relevantes:

```text
V11 — módulo de Resíduos
V12 — backfill Código SGL
V13 — estado físico, tratamento e responsabilidade inicial
V14 — Classes de Resíduo
V15 — segurança/EPI
```

Migrations aplicadas são imutáveis. Novas alterações devem usar V16+.

---

## 5. Snapshot — regra arquitetural importante

Dados históricos de uma ocorrência real não devem depender de cadastros mutáveis.

Exemplos já implementados:

```text
ClasseResiduo = catálogo atual/editável
ResiduoClasse = snapshot histórico da classificação

Produto = recomendações atuais de segurança
Residuo = segurança efetivamente informada/confirmada naquela ocorrência
```

Alterar Classe, Produto ou futuramente ModeloResiduo não pode modificar retroativamente Resíduos existentes.

Preservar essa regra durante toda a Etapa 4.

---

# 6. Escopo da Etapa 4

## 4.1 — Locais de armazenamento cadastráveis

Objetivo: substituir dependência exclusiva de texto livre por locais reutilizáveis, sem perder flexibilidade.

Uso esperado:

```text
local cadastrado
+ complemento livre
```

Exemplo:

```text
Almoxarifado Químico
+ Prateleira B2
```

Também deve ser possível informar local manualmente quando necessário.

Regra:

```text
etapa que exige armazenamento
→ precisa terminar com um local válido
```

Antes de implementar, definir:

- entidade/catálogo por Unidade;
- ativação/inativação;
- como preservar histórico se o local for renomeado depois;
- se será necessário snapshot de nome do local no Resíduo;
- como combinar local cadastrado + complemento livre.

Não antecipar layout definitivo antes da modelagem.

---

## 4.2 — Modelos de Resíduos pré-cadastrados pela Gestão

Criar `ModeloResiduo` ou estrutura equivalente para padrões recorrentes.

Modelo é definição reutilizável; não é ocorrência.

```text
ModeloResiduo
= padrão reutilizável

Residuo
= ocorrência real
```

Um modelo poderá sugerir/preencher, conforme a modelagem final:

- nome/descrição;
- procedência/uso padrão;
- composição padrão;
- Produtos/componentes relacionados;
- Classes de Resíduo;
- riscos conhecidos;
- segurança/EPI;
- recipiente/acondicionamento;
- tratamento padrão quando fizer sentido como sugestão;
- outros dados reutilizáveis aprovados.

Regras obrigatórias:

- modelo não movimenta estoque;
- alteração futura do modelo não altera Resíduos históricos;
- Solicitante ainda cria uma ocorrência real;
- classificação/segurança continuam sujeitas à conferência da Gestão;
- dados específicos da ocorrência não devem ficar presos ao modelo.

---

## 4.3 — Uso pelo Solicitante

Na tela Informar Resíduo, permitir escolha clara entre:

```text
usar modelo pré-cadastrado
ou
preencher manualmente
```

Selecionar modelo deve preencher sugestões iniciais. O usuário deve poder completar/ajustar o que pertence à ocorrência real, respeitando as regras definidas.

Não transformar o modelo em referência viva para o histórico.

---

## 4.4 — Correções administrativas do ciclo de vida

Necessidade levantada na validação final da Etapa 3.

Objetivo: permitir correções operacionais sem apagar a trilha histórica.

Avaliar para perfil `ADMINISTRADOR`:

```text
Cancelar Resíduo
→ justificativa obrigatória
→ preserva registro
→ preserva histórico
→ registra ator/data/motivo

Retornar para análise/liberação
→ justificativa obrigatória
→ preserva eventos anteriores
→ exige nova validação antes de liberar novamente
→ impressão deve voltar a ser bloqueada quando aplicável
```

Antes de implementar, fechar explicitamente:

1. de quais status pode retornar;
2. se `DESPACHADO` é irreversível no fluxo comum;
3. se será criado status `CANCELADO`;
4. quais dados confirmados permanecem visíveis após retorno;
5. se uma nova liberação cria novo evento sem apagar a anterior;
6. efeitos sobre rótulo e permissão de impressão;
7. permissões exatas da ação administrativa.

Não confundir com delete lógico.

A avaliação geral de delete lógico permanece na **Etapa 11**.

---

## 7. Fora do escopo da Etapa 4

Não antecipar:

- Projetos/Atividades → Etapa 5;
- evolução institucional de Estagiários → Etapa 6;
- Relatórios consolidados → Etapa 7;
- normalização g/mL/unidades e Soluções → Etapa 8;
- Soluções em Pedidos → Etapa 9;
- Zebra/template final/infraestrutura física de impressão → Etapa 10;
- Manual do Usuário e decisão geral de delete lógico → Etapa 11;
- testes automatizados frontend → Etapa 12;
- refactor final de classes grandes, inclusive `Residuo` → Etapa 13.

---

## 8. Observação sobre tamanho das classes

`Residuo.java` cresceu significativamente durante a Etapa 3.

Não realizar refactor estrutural grande agora apenas para reduzir linhas, pois isso pode aumentar risco durante as etapas funcionais.

A **Etapa 13 — revisão estrutural e legibilidade** já foi criada para:

- revisar `Residuo`;
- revisar Services/DTOs/Controllers extensos;
- extrair responsabilidades reais quando necessário;
- documentar conceitos como snapshot;
- preservar contratos e comportamento;
- reexecutar os testes automatizados da Etapa 12 após o refactor.

---

## 9. Próximo passo ao abrir a nova janela

Não começar codando imediatamente.

Primeiro:

```text
1. confirmar main atualizada nos dois repositórios;
2. ler documentação canônica;
3. revisar o estado real de Residuo/Services/DTOs/frontend;
4. criar branch feat/etapa-4-residuos a partir da main atualizada;
5. detalhar a Etapa 4 em passos pequenos;
6. iniciar somente a 4.1;
```

O usuário fará as mudanças funcionais do backend manualmente.
