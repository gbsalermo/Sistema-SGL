package com.sgl.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sgl.model.EstoqueCentral;
import com.sgl.model.Produto;
import com.sgl.model.Unidade;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.tenant.TenantContext;
import com.sgl.tenant.TenantProvider;

/**
 * Testes de integração do ProdutoRepository usando @DataJpaTest com H2 real
 * (sem mocks) — persiste entidades via TestEntityManager e valida todos os
 * métodos customizados do repositório.
 *
 * Segue o mesmo padrão dos demais Repositories desta fase:
 * - @ActiveProfiles("test") ativa application-test.properties (H2), já que o
 *   profile padrão da aplicação ("dev") usaria PostgreSQL real.
 * - @Import(TenantProvider.class) disponibiliza o bean "tenantProvider" usado
 *   pela expressão SpEL do findAll() sobrescrito.
 *
 * ATENÇÃO (não é bug a corrigir, apenas observação para quem for ler o
 * teste): a classe Produto usa @Builder do Lombok, mas os campos "risco",
 * "perecivel" e "ativo" têm apenas inicializador de campo Java (ex.:
 * "= false"), SEM a anotação @Builder.Default. Isso significa que o valor
 * do inicializador NÃO é aplicado quando o objeto é criado via builder — o
 * builder começa com esses campos em null. Como as colunas correspondentes
 * são NOT NULL no banco, os testes abaixo setam risco/perecivel/ativo
 * explicitamente em todo Produto.builder() para evitar erro de constraint.
 * (@PrePersist da entidade só preenche "fiscalizado" e as coleções quando
 * nulos, não cobre risco/perecivel/ativo.)
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TenantProvider.class)
class ProdutoRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProdutoRepository produtoRepository;

    @AfterEach
    void limparTenant() {
        TenantContext.limpar();
    }

    private Unidade criarUnidade(String sigla) {
        Unidade unidade = Unidade.builder()
                .nome("Unidade " + sigla)
                .sigla(sigla)
                .build();
        return entityManager.persistAndFlush(unidade);
    }

    private Produto criarProduto(String nome, String codigoReferencia, NivelRisco risco, boolean perecivel) {
        Produto produto = Produto.builder()
                .nome(nome)
                .codigoReferencia(codigoReferencia)
                .unidadeMedida(UnidadeMedida.UNIDADE)
                .risco(risco)
                .perecivel(perecivel)
                .ativo(true)
                .build();
        return entityManager.persistAndFlush(produto);
    }

    private EstoqueCentral criarEstoque(Unidade unidade, Produto produto) {
        EstoqueCentral estoque = EstoqueCentral.builder()
                .unidade(unidade)
                .produto(produto)
                .quantidadeAtual(10)
                .quantidadeMinima(1)
                .ativo(true)
                .build();
        return entityManager.persistAndFlush(estoque);
    }

    // --- findByPublicId -------------------------------------------------------

    @Test
    void deveEncontrarPorPublicId() {
        Produto produto = criarProduto("Ácido Clorídrico", "COD-1", NivelRisco.ALTO, false);

        Optional<Produto> resultado = produtoRepository.findByPublicId(produto.getPublicId());

        assertTrue(resultado.isPresent());
        assertEquals("Ácido Clorídrico", resultado.get().getNome());
    }

    @Test
    void naoDeveEncontrarPorPublicIdInexistente() {
        Optional<Produto> resultado = produtoRepository.findByPublicId(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    // --- findAll() com filtro de tenant via EstoqueCentral ---------------------

    @Test
    void findAllDeveRetornarTodosOsProdutosQuandoNenhumTenantEstaDefinido() {
        criarProduto("Produto A", "COD-2", NivelRisco.NENHUM, false);
        criarProduto("Produto B", "COD-3", NivelRisco.NENHUM, false);

        List<Produto> resultado = produtoRepository.findAll();

        assertEquals(2, resultado.size());
    }

    @Test
    void findAllDeveFiltrarApenasProdutoComEstoqueNaUnidadeDoTenantAtual() {
        Unidade unidadeA = criarUnidade("PR1");
        Unidade unidadeB = criarUnidade("PR2");
        Produto produtoComEstoqueEmA = criarProduto("Produto com estoque em A", "COD-4", NivelRisco.NENHUM, false);
        Produto produtoComEstoqueEmB = criarProduto("Produto com estoque em B", "COD-5", NivelRisco.NENHUM, false);
        criarEstoque(unidadeA, produtoComEstoqueEmA);
        criarEstoque(unidadeB, produtoComEstoqueEmB);

        TenantContext.definir(unidadeA.getPublicId());

        List<Produto> resultado = produtoRepository.findAll();

        assertEquals(1, resultado.size());
        assertEquals(produtoComEstoqueEmA.getId(), resultado.get(0).getId());
    }

    // --- buscarPorIdComBloqueio (@Lock PESSIMISTIC_WRITE) -----------------------

    @Test
    void deveBuscarPorIdComBloqueioQuandoProdutoExiste() {
        Produto produto = criarProduto("Produto com bloqueio", "COD-6", NivelRisco.NENHUM, false);

        Optional<Produto> resultado = produtoRepository.buscarPorIdComBloqueio(produto.getId());

        assertTrue(resultado.isPresent());
        assertEquals(produto.getId(), resultado.get().getId());
    }

    @Test
    void naoDeveBuscarPorIdComBloqueioQuandoProdutoNaoExiste() {
        Optional<Produto> resultado = produtoRepository.buscarPorIdComBloqueio(-1L);

        assertTrue(resultado.isEmpty());
    }

    // --- findByRisco -------------------------------------------------------------

    @Test
    void deveEncontrarPorRisco() {
        criarProduto("Produto de risco alto", "COD-7", NivelRisco.ALTO, false);
        criarProduto("Produto de risco baixo", "COD-8", NivelRisco.BAIXO, false);

        List<Produto> resultado = produtoRepository.findByRisco(NivelRisco.ALTO);

        assertEquals(1, resultado.size());
        assertEquals(NivelRisco.ALTO, resultado.get(0).getRisco());
    }

    @Test
    void deveRetornarListaVaziaQuandoNenhumProdutoTemORiscoInformado() {
        criarProduto("Produto de risco baixo", "COD-9", NivelRisco.BAIXO, false);

        List<Produto> resultado = produtoRepository.findByRisco(NivelRisco.ALTO);

        assertTrue(resultado.isEmpty());
    }

    // --- findByPerecivelTrue -------------------------------------------------------

    @Test
    void deveEncontrarProdutosPereciveis() {
        criarProduto("Produto perecível", "COD-10", NivelRisco.NENHUM, true);
        criarProduto("Produto não perecível", "COD-11", NivelRisco.NENHUM, false);

        List<Produto> resultado = produtoRepository.findByPerecivelTrue();

        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).getPerecivel());
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaProdutosPereciveis() {
        criarProduto("Produto não perecível", "COD-12", NivelRisco.NENHUM, false);

        List<Produto> resultado = produtoRepository.findByPerecivelTrue();

        assertTrue(resultado.isEmpty());
    }

    // --- findByNomeContainingIgnoreCase -------------------------------------------

    @Test
    void deveEncontrarPorNomeContendoTermoIgnorandoCaixa() {
        criarProduto("Álcool Etílico", "COD-13", NivelRisco.NENHUM, false);
        criarProduto("Éter Etílico", "COD-14", NivelRisco.NENHUM, false);

        List<Produto> resultado = produtoRepository.findByNomeContainingIgnoreCase("etílico");

        assertEquals(2, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoNomeNaoContemOTermo() {
        criarProduto("Álcool Etílico", "COD-15", NivelRisco.NENHUM, false);

        List<Produto> resultado = produtoRepository.findByNomeContainingIgnoreCase("inexistente");

        assertTrue(resultado.isEmpty());
    }

    // --- findDisponiveisNaUnidade ---------------------------------------------------

    @Test
    void deveEncontrarProdutosDisponiveisNaUnidade() {
        Unidade unidade = criarUnidade("PR3");
        Produto produtoComEstoque = criarProduto("Produto disponível", "COD-16", NivelRisco.NENHUM, false);
        Produto produtoSemEstoque = criarProduto("Produto indisponível", "COD-17", NivelRisco.NENHUM, false);
        criarEstoque(unidade, produtoComEstoque);

        List<Produto> resultado = produtoRepository.findDisponiveisNaUnidade(unidade.getPublicId());

        assertEquals(1, resultado.size());
        assertEquals(produtoComEstoque.getId(), resultado.get(0).getId());
        assertFalse(resultado.stream().anyMatch(p -> p.getId().equals(produtoSemEstoque.getId())));
    }

    @Test
    void deveRetornarListaVaziaQuandoUnidadeNaoTemProdutosDisponiveis() {
        Unidade unidade = criarUnidade("PR4");
        criarProduto("Produto sem estoque", "COD-18", NivelRisco.NENHUM, false);

        List<Produto> resultado = produtoRepository.findDisponiveisNaUnidade(unidade.getPublicId());

        assertTrue(resultado.isEmpty());
    }

    // --- pertenceAUnidade -------------------------------------------------------------

    @Test
    void devePertencerAUnidadeQuandoExisteEstoqueDoProdutoNaUnidade() {
        Unidade unidade = criarUnidade("PR5");
        Produto produto = criarProduto("Produto com estoque", "COD-19", NivelRisco.NENHUM, false);
        criarEstoque(unidade, produto);

        boolean pertence = produtoRepository.pertenceAUnidade(produto.getPublicId(), unidade.getPublicId());

        assertTrue(pertence);
    }

    @Test
    void naoDevePertencerAUnidadeQuandoNaoExisteEstoqueDoProdutoNaUnidade() {
        Unidade unidade = criarUnidade("PR6");
        Produto produto = criarProduto("Produto sem estoque nesta unidade", "COD-20", NivelRisco.NENHUM, false);

        boolean pertence = produtoRepository.pertenceAUnidade(produto.getPublicId(), unidade.getPublicId());

        assertFalse(pertence);
    }

    // --- existsByCodigoReferencia / existsByCodigoReferenciaAndIdNot -----------------

    @Test
    void deveExistirPorCodigoReferencia() {
        criarProduto("Produto com código", "COD-EXISTS-1", NivelRisco.NENHUM, false);

        assertTrue(produtoRepository.existsByCodigoReferencia("COD-EXISTS-1"));
    }

    @Test
    void naoDeveExistirPorCodigoReferenciaInexistente() {
        assertFalse(produtoRepository.existsByCodigoReferencia("COD-INEXISTENTE"));
    }

    @Test
    void deveExistirPorCodigoReferenciaComIdDiferenteQuandoOutroProdutoUsaOMesmoCodigo() {
        Produto outroProduto = criarProduto("Outro produto", "COD-EXISTS-2", NivelRisco.NENHUM, false);

        // Simula a checagem de duplicidade ao editar um produto diferente do
        // que já possui o código "COD-EXISTS-2".
        boolean existe = produtoRepository.existsByCodigoReferenciaAndIdNot("COD-EXISTS-2", outroProduto.getId() + 1);

        assertTrue(existe);
    }

    @Test
    void naoDeveExistirPorCodigoReferenciaComIdNotQuandoOCodigoPertenceAoProprioId() {
        Produto produto = criarProduto("Produto próprio", "COD-EXISTS-3", NivelRisco.NENHUM, false);

        // O próprio registro que possui o código é excluído da busca (caso de edição sem conflito).
        boolean existe = produtoRepository.existsByCodigoReferenciaAndIdNot("COD-EXISTS-3", produto.getId());

        assertFalse(existe);
    }
}
