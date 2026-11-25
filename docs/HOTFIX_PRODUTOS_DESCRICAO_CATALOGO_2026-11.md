# Hotfix isolado — cadastro e listagem de produtos

> **Estado:** branch baseada em `release`, sem merge e sem deploy. Aguardar validação local e homologação no banco existente. Sem incluir modalidades de bolsa nem alterações de massa DEMO.

## Escopo exato

- `Produto.java`: `descricao` como `TEXT`, `unidadeCadastro` nullable.
- Migration **V49__descricao_produto_texto_e_unidade_cadastro.sql**, pois `release` termina em V48. A migration **V49__modalidades_bolsa_por_unidade.sql** está em outra branch não homologada e **não faz parte deste hotfix**; ao retomá-la, deverá ser renumerada/rebaseada, sem duplicar V49.
- `ProdutoService.criar`: novos produtos recebem a Unidade de operação da sessão.
- `GET /api/v1/produtos/catalogo`: consulta administrativa para registros originários da Unidade e registros vinculados ao estoque local. Acesso restrito a administradores conforme regras atuais.
- `GET /api/v1/produtos` e `ProdutoRepository.pertenceAUnidade` permanecem iguais. Não criar estoque fictício, não alterar pedidos.
- `ProdutoService.buscarProdutoNoTenant`: edição/consulta de um produto pertencente à Unidade de cadastro, mesmo sem estoque.
- `RestExceptionHandler`: SQLState legível (`22001` tamanho, `23505` duplicidade, `23502` obrigatório, `23503` referência, `23514` check), sem expor SQL.
- `ProdutoRequestDTO`: validação de limites dos campos que continuam `VARCHAR`.
- Frontend administrativo consulta `/v1/produtos/catalogo` com fallback apenas para erro HTTP 404 da versão antiga.
- Testes unitários e de integração adicionados, mas não executados aqui.

## Preservação de dados

- V49 **não contém** `DELETE`, `TRUNCATE` ou `DROP TABLE`; altera a coluna de `VARCHAR(255)` para `TEXT` e cria uma coluna `unidade_cadastro_id` opcional com FK.
- A migração tenta recuperar produtos históricos sem estoque e sem Unidade **somente se o banco possuir exatamente uma Unidade**.
- Com múltiplas Unidades, **um produto histórico sem estoque e sem `unidade_cadastro_id` pode continuar invisível**. Confirmar a origem real antes de atribuí-lo; não inferir pela simples existência do código.
- Produtos já associados a `EstoqueCentral` da Unidade aparecem na listagem administrativa. Registro `ativo = false` poderá exigir a opção de mostrar inativos.
- A existência de código duplicado indica apenas a existência do registro. Uma tentativa anterior que devolveu HTTP 409 por descrição longa provavelmente foi revertida e **não garante a descrição completa persistida**.

## Diagnóstico sem alterar banco

Antes da aplicação (quando ainda não existe `unidade_cadastro_id`):

```sql
SELECT COUNT(*) AS qtd_unidades FROM unidades;

SELECT p.id, p.public_id, p.nome, p.codigo_referencia, p.ativo,
       length(p.descricao) AS caracteres_descricao,
       EXISTS (
          SELECT 1 FROM estoque_central ec WHERE ec.produto_id = p.id
       ) AS tem_estoque
FROM produtos p
WHERE p.nome ILIKE '%BDA%'
   OR p.nome ILIKE '%Batata Dextrose%'
   OR p.codigo_referencia ILIKE '%BDA%';
```

Depois da migration, acrescentar `p.unidade_cadastro_id` à consulta e verificar qual Unidade possui o produto.

Em um **banco com várias Unidades**, a correção de origem de um produto específico, depois de confirmar documentalmente sua Unidade, pode ser feita em transação controlada:

```sql
-- Exemplo de manutenção; substitua os dois parâmetros por IDs REAIS validados.
BEGIN;
UPDATE produtos
   SET unidade_cadastro_id = :unidade_id_validada
 WHERE id = :produto_id_validado
   AND unidade_cadastro_id IS NULL
   AND NOT EXISTS (
       SELECT 1 FROM estoque_central ec WHERE ec.produto_id = produtos.id
   );
-- Confira o resultado exato da alteração antes do COMMIT.
-- Na dúvida, ROLLBACK.
```

## Validação local obrigatória antes da produção

1. Criar backup do PostgreSQL real; nunca usar ambiente DEMO/H2 para afirmar que a migração em produção está segura.
2. Atualizar backend e frontend **da mesma branch isolada**.
3. Rodar `./mvnw clean verify`, `npm run build` e `npm run test`.
4. No DEV PostgreSQL (base representativa), conferir Flyway V49 e inicialização com `ddl-auto=validate`.
5. Confirmar a contagem de Unidades e o estado do BDA antes/depois da migração.
6. Conferir que o BDA antigo (se salvo) aparece na lista administrativa, e que o novo cadastro com 360 caracteres salva, exibe e persiste a descrição inteira.
7. Confirmar produto sem estoque visível apenas no cadastro administrativo, **não em pedidos/retiradas**.
8. Confirmar que editar/inativar funciona para produtos sem estoque na Unidade correspondente e não funciona a partir de outra Unidade.
9. Conferir situações de código duplicado e excesso de caracteres com mensagens claras.
10. Só depois de aprovação, aplicar em produção com backup, janela de reversão e publicação por **tag** a partir do commit homologado. Não apontar deploy para branch de desenvolvimento.

**Atenção:** se a migration V49 for aplicada a um banco e depois a definição de V49 for modificada, o Flyway poderá detectar incompatibilidade de checksum. Uma migration executada em ambiente compartilhado não deve ser alterada retroativamente.
