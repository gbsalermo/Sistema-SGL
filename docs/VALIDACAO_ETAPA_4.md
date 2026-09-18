# Validação integrada — Etapa 4

**Data de preparação:** 18/09/2026  
**Branch:** `feat/etapa-4-residuos`  
**Estado:** implementação concluída; validação manual integrada pendente.

Este roteiro existe para que a validação da Etapa 4 possa ser feita sem novas implementações durante a sessão de testes.

## 1. Pré-check técnico

Backend:

```bash
cd backend/sgl-backend
./mvnw test
./mvnw spring-boot:run
```

Confirmar:

- Flyway reconhece o schema até V17;
- Hibernate `ddl-auto=validate` não aponta divergências;
- testes existentes e os testes de ModeloResiduo/correção administrativa passam;
- API sobe sem erro.

Frontend:

```bash
npm install
npm run build
npm run dev
```

Confirmar que `vue-tsc` e Vite concluem o build sem erro.

## 2. Etapa 4.1 — locais de armazenamento

1. Administrador cria um local de armazenamento na própria Unidade.
2. Edita o nome.
3. Inativa o local.
4. Local inativo deixa de aparecer para novas seleções.
5. Resíduo antigo continua preservando o snapshot textual do local.
6. Na análise, testar local cadastrado + complemento.
7. Na confirmação física, testar manter o local planejado.
8. Testar corrigir para outro local cadastrado.
9. Testar corrigir para local manual.
10. Confirmar histórico das correções.

## 3. Etapa 4.2 — Modelos de Resíduo

Na Administração, abrir:

```text
/administracao/cadastros/modelos-residuo
```

Validar:

1. criar modelo com componente livre;
2. criar modelo com Produto do catálogo;
3. usar mais de um componente;
4. associar Classes;
5. configurar riscos e segurança/EPI;
6. `OUTRO` exige observação;
7. tratamento padrão exige descrição quando marcado;
8. editar modelo;
9. editar componentes e confirmar que os antigos são substituídos, sem duplicação;
10. nome duplicado na mesma Unidade é rejeitado;
11. modelo de outra Unidade não fica acessível pelo tenant atual;
12. inativar modelo;
13. modelo inativo permanece visível na administração quando o filtro de inativos estiver habilitado;
14. modelo inativo não aparece para novas ocorrências.

## 4. Etapa 4.3 — modelo ou preenchimento manual

Em `/residuos/novo`:

### Caminho manual

1. manter "Preencher manualmente";
2. preencher um Resíduo normalmente;
3. enviar;
4. confirmar que o fluxo antigo permanece funcional.

### Caminho por modelo

1. selecionar um modelo ativo;
2. confirmar preenchimento de descrição, processo, estado físico, tratamento, recipiente, unidade, riscos, Classes, EPI e componentes;
3. confirmar que quantidade continua vazia e precisa ser informada para a ocorrência;
4. confirmar que projeto permanece específico da ocorrência;
5. alterar um dos valores sugeridos antes de enviar;
6. criar o Resíduo;
7. editar o modelo posteriormente;
8. abrir o Resíduo já criado e confirmar que seus dados históricos não mudaram.

Esta é a validação principal da separação:

```text
ModeloResiduo = definição reutilizável e mutável
Residuo       = ocorrência real independente
```

## 5. Etapa 4.4 — correções administrativas

As ações devem aparecer apenas para perfil `ADMINISTRADOR`.

### Cancelamento

1. abrir Resíduo `INFORMADO`;
2. cancelar;
3. confirmar obrigatoriedade da justificativa;
4. confirmar status `CANCELADO`;
5. confirmar evento `RESIDUO_CANCELADO_ADMINISTRATIVAMENTE` no histórico;
6. confirmar que a justificativa e o Administrador aparecem na rastreabilidade;
7. confirmar que cancelado não aparece como Resíduo ativo;
8. confirmar filtro/aba de Cancelados;
9. tentar retornar um cancelado e confirmar bloqueio.

### Retorno de etapa

Validar pelo menos uma vez cada transição:

```text
EM_ANALISE                     → INFORMADO
LIBERADO_PARA_ARMAZENAMENTO   → EM_ANALISE
ARMAZENADO_TEMPORARIAMENTE    → LIBERADO_PARA_ARMAZENAMENTO
DESPACHADO                     → ARMAZENADO_TEMPORARIAMENTE
```

Em todos os casos:

- justificativa é obrigatória;
- evento `RETORNO_ADMINISTRATIVO_DE_ETAPA` é registrado;
- ator/data permanecem no histórico;
- retorno é de exatamente uma etapa;
- nova execução da etapa cria novos eventos sem apagar eventos anteriores.

Para `DESPACHADO`, confirmar que o retorno limpa a confirmação do despacho antes de permitir novo ciclo.

Cancelamento direto de `DESPACHADO` deve ser rejeitado; primeiro é necessário retornar uma etapa.

## 6. Relatórios e regressão

1. relatório de Resíduos exibe contagem de Cancelados;
2. filtro por `CANCELADO` funciona;
3. exportações PDF/XLSX continuam funcionando e incluem a contagem de Cancelados no resumo;
4. Dashboard de Gestão não contabiliza cancelados como ativos;
5. Dashboard do Solicitante não contabiliza cancelados como ativos;
6. Meus Resíduos permite consultar Cancelados;
7. rótulo de Resíduo cancelado não deve ficar liberado para impressão;
8. fluxo normal até `DESPACHADO` continua funcionando;
9. fluxo de Pedidos/Estoque não deve sofrer alteração pela Etapa 4.

## 7. Critério de fechamento

A Etapa 4 pode ser integrada à `main` quando:

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

Após a validação, o próximo bloco do roadmap é a **Etapa 5 — Projetos e Atividades**, respeitando o portão de confirmação de requisitos já documentado no plano de pré-produção.
