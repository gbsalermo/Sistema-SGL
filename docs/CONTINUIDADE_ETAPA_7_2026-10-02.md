# Continuidade — Etapa 7 — Relatórios consolidados

**Data:** 02/10/2026  
**Projeto:** SGL — Sistema de Gestão de Laboratórios  
**Branch backend:** `collab/etapa-7-relatorios-consolidados`  
**Branch frontend:** `collab/etapa-7-relatorios-consolidados`  
**Dependências:** Etapas 5 e 6 concluídas e validadas  
**Estado:** Etapa 7 iniciada; implementação ainda não iniciada

---

## 1. Observação de integração

Na verificação de 02/10/2026, a branch `collab/etapa-6-estagiarios-vinculos` ainda aparece à frente de `main` no GitHub em ambos os repositórios.

Portanto:

- a Etapa 6 está funcionalmente concluída e validada;
- o merge ainda não está refletido na `main` do GitHub;
- a branch da Etapa 7 foi criada diretamente a partir da branch validada da Etapa 6 para preservar toda a continuidade;
- antes do merge final da Etapa 7, confirmar que a Etapa 6 já foi integrada à `main` canônica.

Não fazer `force push` para resolver essa diferença.

---

## 2. Objetivo da Etapa 7

Consolidar a Central de Relatórios sobre os domínios estabilizados nas Etapas 5 e 6.

A Etapa 7 não substitui os hubs operacionais de Projeto, Estagiários, Estoque ou Resíduos. Ela organiza consulta, agregação, filtros e exportação.

---

## 3. Escopo confirmado

### 7.1 — Revisão dos relatórios atuais

Inventariar e validar:

- Estagiários;
- Produtos;
- Movimentações;
- Resumo operacional;
- Estoque e lotes;
- Fiscalização;
- Resíduos;
- Pessoas por laboratório.

Objetivo: identificar sobreposição, filtros faltantes e contratos que podem ser reaproveitados.

### 7.2 — Projetos como relatório próprio

Adicionar `Projetos` à Central de Relatórios.

Cobertura base:

- Projeto;
- Código SEG;
- Laboratório;
- responsável/líder;
- início/fim;
- status;
- situação de execução;
- recurso externo/empresa;
- quantidade de SCI;
- quantidade de Atividades.

A Etapa 6 permite incorporar, quando útil:

- Estagiários associados;
- Orientador;
- Bolsa/vínculo;
- Curso/Formação;
- Cultura;
- Atividade;
- situação do vínculo.

A relação deve continuar derivada de:

```text
Projeto
→ SCI
→ Atividade
→ VinculoEstagioAtividade
→ VinculoEstagio
→ Estagiario
```

Não criar relação artificial direta Projeto ↔ Estagiário.

### 7.3 — Movimentações + Resumo operacional

A Central deve apresentar uma única opção principal:

```text
Movimentações
├── Resumo
└── Detalhamento
```

Inicialmente:

- `Resumo` pode consumir o endpoint atual de resumo operacional;
- `Detalhamento` pode consumir o endpoint atual de movimentações;
- não é obrigatório fundir os endpoints backend nesta primeira rodada.

A unificação inicial é de experiência/interface e deve reduzir risco de regressão.

### 7.4 — Filtros e dimensões

Dimensões candidatas:

- Unidade/tenant implícito;
- Laboratório;
- Projeto;
- Código SEG;
- responsável/líder;
- SCI;
- Atividade;
- Estagiário;
- Orientador;
- Bolsa;
- Curso/Formação;
- Cultura;
- situação/status;
- período.

Cada relatório deve expor somente filtros que façam sentido para seu contrato.

### 7.5 — Exportação

Preservar a regra:

```text
prévia JSON
+ PDF
+ XLSX
→ mesma consulta
→ mesmos filtros
```

Não recriar cálculo oficial no frontend.

---

## 4. Estado herdado da Etapa 6

A Etapa 6 foi concluída e validada com:

- `Usuario → Estagiario → VinculoEstagio → VinculoEstagioAtividade`;
- múltiplas participações por vínculo;
- Projeto/Laboratório derivados da Atividade;
- Bolsa/vínculo com histórico;
- prorrogação e nova ocorrência de Bolsa;
- referência/especificação da Bolsa;
- Curso e Cultura por Unidade;
- observações auditáveis;
- treinamento de segurança auditável;
- integração institucional prioritária;
- testes automatizados finais validados.

Checkpoint: `docs/VALIDACAO_ETAPA_6_6.md`.

---

## 5. Ordem sugerida de implementação

```text
7.0 auditoria dos contratos atuais de relatórios
→ 7.1 consolidar modelo/filtros
→ 7.2 relatório Projetos no backend
→ 7.3 preview/exportação Projetos
→ 7.4 frontend: Projetos na Central
→ 7.5 frontend: Movimentações Resumo/Detalhamento
→ 7.6 integrar dimensões da Etapa 6 onde aprovadas
→ 7.7 testes automatizados
→ 7.8 validação manual/visual
→ documentação e fechamento
```

---

## 6. Regras de trabalho

- não alterar `main` diretamente;
- não usar `--force`;
- GitLab/main continua sendo a fonte canônica conforme a documentação de sincronização;
- mudanças funcionais backend seguem revisão e validação formal;
- não antecipar Etapa 8;
- manter compatibilidade com endpoints atuais quando a mudança puder ser somente de interface.

---

## 7. Documentos a ler antes de implementar

1. `CONTINUIDADE.md`
2. `docs/PLANO_PRE_PRODUCAO.md`
3. este arquivo
4. `docs/RELATORIOS.md`
5. `docs/VALIDACAO_ETAPA_6_6.md`
6. `docs/DECISAO_CONTEXTO_OPERACIONAL_ESTAGIARIO_PEDIDOS.md`
7. Swagger/OpenAPI em execução
8. frontend `CONTINUIDADE.md`
9. frontend `docs/ROADMAP_INTERFACE_GESTAO.md`

---

## 8. Gate inicial da Etapa 7

Antes de implementar, confirmar no código atual:

- DTOs/Services/Controllers dos relatórios existentes;
- estrutura atual da Central de Relatórios;
- quais endpoints já aceitam filtros reutilizáveis;
- como PDF/XLSX compartilham a consulta;
- quais dados de Projeto/SCI/Atividade já possuem DTO de relatório reutilizável;
- quais dimensões de Estagiários podem ser adicionadas sem multiplicar linhas ou gerar contagens incorretas.

A primeira tarefa da Etapa 7 deve ser **auditoria do módulo de relatórios**, não implementação imediata.
