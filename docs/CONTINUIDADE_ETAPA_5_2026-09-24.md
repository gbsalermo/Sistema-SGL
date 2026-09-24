# Continuidade SGL — Etapa 5

**Criado em:** 24/09/2026  
**Etapa anterior:** Etapa 4 — Expansão operacional de Resíduos ✅ concluída e validada  
**Etapa atual:** Etapa 5 — Projetos e Atividades 🔧 iniciada  
**Bloco atual:** 5.0 — Portão de confirmação  
**Branch de trabalho:** `collab/etapa-5-projetos-atividades`  
**Fonte canônica de `main`:** GitLab institucional  

---

## 1. Objetivo

A Etapa 5 estabiliza o domínio de Projetos e, se confirmado, introduz Atividades subordinadas.

Roadmap:

```text
5.0 Portão de confirmação
→ 5.1 Projeto base
→ 5.2 Código SEG
→ 5.3 Atividades — condicional
→ 5.4 Interface
```

Nenhuma alteração estrutural de domínio deve ser feita antes do fechamento do 5.0.

---

## 2. Estado atual do Projeto

O SGL já possui um cadastro funcional de `Projeto`.

Hoje:

```text
Projeto
├── UUID público
├── Laboratório obrigatório
├── nome
├── descrição
├── dataInicio
├── dataFim
├── responsavel (texto livre)
└── ativo
```

Relação vigente:

```text
Laboratório 1 → N Projetos
```

O CRUD atual já é tenant-safe e a Administração possui interface de cadastro/edição.

A Etapa 5 deve evoluir essa base; não criar um segundo domínio de Projeto paralelo.

---

## 3. Portão 5.0 — confirmações obrigatórias

Antes de migrations/entities novas, fechar com o responsável/cliente:

1. **Código SEG**
   - confirmar regra exata do formato institucional;
   - referência atual: `AAAA.MM.DD.XX.XXX`;
   - definir se é informado manualmente ou gerado pelo SGL;
   - definir unicidade e possibilidade de alteração.

2. **Atividade**
   - confirmar se é entidade subordinada a Projeto;
   - se confirmada: `Projeto 1 → N Atividades`;
   - definir dados mínimos e ciclo de vida.

3. **SCI**
   - confirmar se SCI é um tipo de Projeto ou um domínio distinto.

4. **Situação de execução**
   - obter lista oficial de situações;
   - não inventar enum antes dessa confirmação.

---

## 4. Projeto base planejado

Após o portão:

- nome/descrição;
- Laboratório obrigatório;
- líder/responsável;
- início/fim;
- financiador;
- ciclo de vida;
- situação de execução;
- tipo, se confirmado;
- Código SEG;
- Código SGL/rastreabilidade interna.

Ciclo inicialmente proposto no roadmap:

```text
CRIADO
→ ATIVO
→ ENCERRADO_COM_AVALIACAO_PENDENTE
→ CONCLUIDO
```

Esse ciclo ainda deve ser tratado como proposta até o fechamento do 5.0.

---

## 5. Regras de implementação

- partir da `main` já contendo a Etapa 4;
- preservar isolamento por Unidade;
- manter `Laboratório 1 → N Projetos`;
- evoluir `Projeto` existente, não duplicar entidade;
- preservar compatibilidade com Resíduos e Estagiários que já referenciam Projeto;
- migrations aplicadas são imutáveis;
- backend primeiro, frontend somente após estabilização do contrato;
- mudanças funcionais devem ser testadas antes de avançar de bloco.

---

## 6. Próximo passo exato

Fechar as quatro decisões do **5.0**.

Somente depois:

```text
analisar schema atual
→ definir migration seguinte
→ evoluir Projeto
→ DTOs
→ Repository/Service
→ Controller
→ testes
→ frontend
```
