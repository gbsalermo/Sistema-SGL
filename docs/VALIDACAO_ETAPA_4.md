# Validação integrada — Etapa 4

**Atualizado em:** 22/09/2026  
**Estado:** roteiro preservado; execução final pendente da reconciliação da implementação antiga com a `main` atual.  
**Branch alvo de validação:** `collab/etapa-4-residuos-reconcile`

> Não usar a antiga `feat/etapa-4-residuos` como base de homologação. Ela é somente referência de implementação.

---

## 1. Pré-check técnico

Backend:

```bash
./mvnw test
./mvnw spring-boot:run
```

Confirmar:

- Flyway reconhece a sequência canônica, incluindo V16 do supervisor e as migrations reconciliadas da Etapa 4;
- Hibernate `ddl-auto=validate` não aponta divergências;
- testes existentes e testes específicos da Etapa 4 passam;
- API sobe sem erro.

Frontend:

```bash
npm install
npm run build
npm run dev
```

Confirmar que TypeScript/Vite concluem sem erro.

---

## 2. Etapa 4.1 — locais de armazenamento

1. Administrador cria um local na própria Unidade.
2. Edita o nome.
3. Inativa o local.
4. Local inativo deixa de aparecer em novas seleções.
5. Resíduo antigo preserva snapshot textual do local.
6. Na análise, testar local cadastrado + complemento.
7. Na confirmação física, testar manter o local planejado.
8. Testar corrigir para outro local cadastrado.
9. Testar corrigir para local manual.
10. Confirmar histórico das correções.
11. Tentar acesso cross-tenant e confirmar bloqueio.

---

## 3. Etapa 4.2 — Modelos de Resíduo

Na Administração:

```text
/administracao/cadastros/modelos-residuo
```

Validar:

1. modelo com componente livre;
2. modelo com Produto do catálogo;
3. mais de um componente;
4. associação de Classes;
5. riscos e Segurança/EPI;
6. `OUTRO` exige observação;
7. tratamento padrão exige descrição quando marcado;
8. edição;
9. substituição de componentes sem duplicação;
10. nome duplicado na mesma Unidade é rejeitado;
11. tenant não acessa modelo de outra Unidade;
12. inativação;
13. inativo continua visível na administração quando solicitado;
14. inativo não aparece para novas ocorrências.

---

## 4. Etapa 4.3 — modelo ou preenchimento manual

### Caminho manual

1. selecionar preenchimento manual;
2. criar Resíduo normalmente;
3. confirmar regressão zero do fluxo existente.

### Caminho por modelo

1. selecionar modelo ativo;
2. confirmar preenchimento das sugestões previstas;
3. quantidade permanece específica da ocorrência;
4. projeto permanece específico da ocorrência;
5. alterar sugestão antes do envio;
6. criar Resíduo;
7. editar o modelo depois;
8. confirmar que o Resíduo histórico não mudou.

Regra central:

```text
ModeloResiduo = definição reutilizável e mutável
Residuo       = ocorrência real independente
```

---

## 5. Etapa 4.4 — correções administrativas

Ações apenas para `ADMINISTRADOR`.

### Cancelamento

1. cancelar Resíduo elegível;
2. justificativa obrigatória;
3. status `CANCELADO`;
4. evento administrativo no histórico;
5. ator/data/motivo preservados;
6. cancelado não aparece como ativo;
7. filtro/aba de Cancelados funciona;
8. retorno de cancelado é bloqueado;
9. cancelamento direto de `DESPACHADO` é rejeitado conforme regra vigente.

### Retorno de etapa

Validar:

```text
EM_ANALISE                     → INFORMADO
LIBERADO_PARA_ARMAZENAMENTO   → EM_ANALISE
ARMAZENADO_TEMPORARIAMENTE    → LIBERADO_PARA_ARMAZENAMENTO
DESPACHADO                     → ARMAZENADO_TEMPORARIAMENTE
```

Em todos:

- justificativa obrigatória;
- histórico preservado;
- ator/data registrados;
- retorno de exatamente uma etapa;
- nova execução cria novos eventos;
- nenhuma trilha antiga é apagada.

---

## 6. Relatórios e regressão

1. relatório reconhece Cancelados;
2. filtro por `CANCELADO`;
3. PDF/XLSX;
4. Dashboard Gestão não conta cancelados como ativos;
5. Dashboard Solicitante não conta cancelados como ativos;
6. Meus Resíduos consulta Cancelados;
7. rótulo cancelado não imprime;
8. fluxo normal até `DESPACHADO`;
9. Pedidos/Estoque sem regressão;
10. isolamento por Unidade sem regressão.

---

## 7. Critério de fechamento

```text
backend test/build                          ✅
frontend build/type-check                  ✅
4.1 regressão                              ✅
4.2 CRUD de modelos                        ✅
4.3 modelo + manual                        ✅
4.4 cancelar/retornar + histórico          ✅
tenant entre Unidades                      ✅
relatórios/cancelados                      ✅
regressão do fluxo normal de Resíduos      ✅
```

Somente depois dessa bateria a Etapa 4 pode ser considerada concluída e integrada.
