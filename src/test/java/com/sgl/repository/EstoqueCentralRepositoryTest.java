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
 * Testes de integração do EstoqueCentralRepository usando @DataJpaTest com H2
 * real (sem mocks) — persiste entidades via TestEntityManager e valida todos
 * os métodos customizados do repositório.
 *
 * Segue o mesmo padrão dos demais Repositories desta fase:
 * - @ActiveProfiles("test") ativa application-test.properties (H2), já que o
 *   profile padrão da aplicação ("dev") usaria PostgreSQL real.
 * - @Import(TenantProvider.class) disponibiliza o bean "tenantProvider" usado
 *   pela expressão SpEL do findAll() sobrescrito.
 *
 * ATENÇÃO (não é bug a corrigir, apenas observação para quem for ler o
 * teste): EstoqueCentral usa @Builder do Lombok, mas o campo "ativo" tem
 * apenas inicializador de campo Java ("= true"), SEM @Builder.Default — por
 * isso os testes abaixo setam "ativo" explicitamente em todo
 * EstoqueCentral.builder() para evitar erro de constraint NOT NULL. O mesmo
 * vale para "risco"/"perecivel"/"ativo" de Produto (já documentado em
 * ProdutoRepositoryTest).
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TenantProvider.class)
class EstoqueCentralRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EstoqueCentralRepository estoqueCentralRepository;

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

    private Produto criarProduto(String nome, String codigoReferencia) {
        Produto produto = Produto.builder()
                .nome(nome)
                .codigoReferencia(codigoReferencia)
                .unidadeMedida(UnidadeMedida.UNIDADE)
                .risco(NivelRisco.NENHUM)
                .perecivel(false)
                .ativo(true)
                .build();
        return entityManager.persistAndFlush(produto);
    }

    private EstoqueCentral criarEstoque(Unidade unidade, Produto produto, int quantidadeAtual,
            int quantidadeMinima, boolean ativo) {
        EstoqueCentral estoque = EstoqueCentral.builder()
                .unidade(unidade)
                .produto(produto)
                .quantidadeAtual(quantidadeAtual)
                .quantidadeMinima(quantidadeMinima)
                .ativo(ativo)
                .build();
        return entityManager.persistAndFlush(estoque);
    }

    // --- findByPublicId -------------------------------------------------------

    @Test
    void deveEncontrarPorPublicId() {
        Unidade unidade = criarUnidade("EC1");
        Produto produto = criarProduto("Produto EC1", "COD-EC1");
        EstoqueCentral estoque = criarEstoque(unidade, produto, 10, 1, true);

        Optional<EstoqueCentral> resultado = estoqueCentralRepository.findByPublicId(estoque.getPublicId());

        assertTrue(resultado.isPresent());
        assertEquals(estoque.getId(), resultado.get().getId());
    }

    @Test
    void naoDeveEncontrarPorPublicIdInexistente() {
        Optional<EstoqueCentral> resultado = estoqueCentralRepository.findByPublicId(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    // --- findByPublicIdAndUnidadePublicId --------------------------------------

    @Test
    void deveEncontrarPorPublicIdEUnidadePublicId() {
        Unidade unidade = criarUnidade("EC2");
        Produto produto = criarProduto("Produto EC2", "COD-EC2");
        EstoqueCentral estoque = criarEstoque(unidade, produto, 10, 1, true);

        Optional<EstoqueCentral> resultado = estoqueCentralRepository
                .findByPublicIdAndUnidadePublicId(estoque.getPublicId(), unidade.getPublicId());

        assertTrue(resultado.isPresent());
    }

    @Test
    void naoDeveEncontrarQuandoUnidadePublicIdNaoCasaComOEstoque() {
        Unidade unidade = criarUnidade("EC3");
        Unidade outraUnidade = criarUnidade("EC4");
        Produto produto = criarProduto("Produto EC3", "COD-EC3");
        EstoqueCentral estoque = criarEstoque(unidade, produto, 10, 1, true);

        Optional<EstoqueCentral> resultado = estoqueCentralRepository
                .findByPublicIdAndUnidadePublicId(estoque.getPublicId(), outraUnidade.getPublicId());

        assertTrue(resultado.isEmpty());
    }

    // --- findAll() com filtro de tenant via SpEL --------------------------------

    @Test
    void findAllDeveRetornarTodosOsEstoquesQuandoNenhumTenantEstaDefinido() {
        Unidade unidadeA = criarUnidade("EC5");
        Unidade unidadeB = criarUnidade("EC6");
        criarEstoque(unidadeA, criarProduto("Produto A", "COD-EC5"), 10, 1, true);
        criarEstoque(unidadeB, criarProduto("Produto B", "COD-EC6"), 10, 1, true);

        List<EstoqueCentral> resultado = estoqueCentralRepository.findAll();

        assertEquals(2, resultado.size());
    }

    @Test
    void findAllDeveFiltrarApenasEstoqueDaUnidadeDoTenantAtual() {
        Unidade unidadeA = criarUnidade("EC7");
        Unidade unidadeB = criarUnidade("EC8");
        EstoqueCentral estoqueA = criarEstoque(unidadeA, criarProduto("Produto A2", "COD-EC7"), 10, 1, true);
        criarEstoque(unidadeB, criarProduto("Produto B2", "COD-EC8"), 10, 1, true);

        TenantContext.definir(unidadeA.getPublicId());

        List<EstoqueCentral> resultado = estoqueCentralRepository.findAll();

        assertEquals(1, resultado.size());
        assertEquals(estoqueA.getId(), resultado.get(0).getId());
    }

    // --- findByUnidadeIdAndProdutoId / existsByUnidadeIdAndProdutoId -----------

    @Test
    void deveEncontrarPorUnidadeIdEProdutoId() {
        Unidade unidade = criarUnidade("EC9");
        Produto produto = criarProduto("Produto EC9", "COD-EC9");
        EstoqueCentral estoque = criarEstoque(unidade, produto, 10, 1, true);

        Optional<EstoqueCentral> resultado = estoqueCentralRepository
                .findByUnidadeIdAndProdutoId(unidade.getId(), produto.getId());

        assertTrue(resultado.isPresent());
        assertEquals(estoque.getId(), resultado.get().getId());
    }

    @Test
    void naoDeveEncontrarPorUnidadeIdEProdutoIdQuandoNaoExisteEstoque() {
        Unidade unidade = criarUnidade("EC10");
        Produto produto = criarProduto("Produto EC10", "COD-EC10");

        Optional<EstoqueCentral> resultado = estoqueCentralRepository
                .findByUnidadeIdAndProdutoId(unidade.getId(), produto.getId());

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveExistirPorUnidadeIdEProdutoId() {
        Unidade unidade = criarUnidade("EC11");
        Produto produto = criarProduto("Produto EC11", "COD-EC11");
        criarEstoque(unidade, produto, 10, 1, true);

        assertTrue(estoqueCentralRepository.existsByUnidadeIdAndProdutoId(unidade.getId(), produto.getId()));
    }

    @Test
    void naoDeveExistirPorUnidadeIdEProdutoIdQuandoNaoExisteEstoque() {
        Unidade unidade = criarUnidade("EC12");
        Produto produto = criarProduto("Produto EC12", "COD-EC12");

        assertFalse(estoqueCentralRepository.existsByUnidadeIdAndProdutoId(unidade.getId(), produto.getId()));
    }

    // --- buscarPorIdComBloqueio (@Lock PESSIMISTIC_WRITE) -----------------------

    @Test
    void deveBuscarPorIdComBloqueioQuandoEstoqueExiste() {
        Unidade unidade = criarUnidade("EC13");
        Produto produto = criarProduto("Produto EC13", "COD-EC13");
        EstoqueCentral estoque = criarEstoque(unidade, produto, 10, 1, true);

        Optional<EstoqueCentral> resultado = estoqueCentralRepository.buscarPorIdComBloqueio(estoque.getId());

        assertTrue(resultado.isPresent());
    }

    @Test
    void naoDeveBuscarPorIdComBloqueioQuandoEstoqueNaoExiste() {
        Optional<EstoqueCentral> resultado = estoqueCentralRepository.buscarPorIdComBloqueio(-1L);

        assertTrue(resultado.isEmpty());
    }

    // --- buscarPorUnidadeEProdutoComBloqueio (@Lock PESSIMISTIC_WRITE) ----------

    @Test
    void deveBuscarPorUnidadeEProdutoComBloqueioQuandoEstoqueExiste() {
        Unidade unidade = criarUnidade("EC14");
        Produto produto = criarProduto("Produto EC14", "COD-EC14");
        EstoqueCentral estoque = criarEstoque(unidade, produto, 10, 1, true);

        Optional<EstoqueCentral> resultado = estoqueCentralRepository
                .buscarPorUnidadeEProdutoComBloqueio(unidade.getId(), produto.getId());

        assertTrue(resultado.isPresent());
        assertEquals(estoque.getId(), resultado.get().getId());
    }

    @Test
    void naoDeveBuscarPorUnidadeEProdutoComBloqueioQuandoNaoExisteEstoque() {
        Unidade unidade = criarUnidade("EC15");
        Produto produto = criarProduto("Produto EC15", "COD-EC15");

        Optional<EstoqueCentral> resultado = estoqueCentralRepository
                .buscarPorUnidadeEProdutoComBloqueio(unidade.getId(), produto.getId());

        assertTrue(resultado.isEmpty());
    }

    // --- findByUnidadeId / findByUnidadePublicId --------------------------------

    @Test
    void deveEncontrarPorUnidadeId() {
        Unidade unidade = criarUnidade("EC16");
        criarEstoque(unidade, criarProduto("Produto EC16", "COD-EC16"), 10, 1, true);

        List<EstoqueCentral> resultado = estoqueCentralRepository.findByUnidadeId(unidade.getId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoUnidadeIdNaoTemEstoque() {
        Unidade unidade = criarUnidade("EC17");

        List<EstoqueCentral> resultado = estoqueCentralRepository.findByUnidadeId(unidade.getId());

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveEncontrarPorUnidadePublicId() {
        Unidade unidade = criarUnidade("EC18");
        criarEstoque(unidade, criarProduto("Produto EC18", "COD-EC18"), 10, 1, true);

        List<EstoqueCentral> resultado = estoqueCentralRepository.findByUnidadePublicId(unidade.getPublicId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoUnidadePublicIdNaoTemEstoque() {
        List<EstoqueCentral> resultado = estoqueCentralRepository.findByUnidadePublicId(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    // --- findByUnidadeIdAndAtivoTrue / findByUnidadePublicIdAndAtivoTrue -------

    @Test
    void deveEncontrarPorUnidadeIdApenasAtivos() {
        Unidade unidade = criarUnidade("EC19");
        criarEstoque(unidade, criarProduto("Produto ativo EC19", "COD-EC19-1"), 10, 1, true);
        criarEstoque(unidade, criarProduto("Produto inativo EC19", "COD-EC19-2"), 10, 1, false);

        List<EstoqueCentral> resultado = estoqueCentralRepository.findByUnidadeIdAndAtivoTrue(unidade.getId());

        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).getAtivo());
    }

    @Test
    void deveRetornarListaVaziaQuandoTodosOsEstoquesDaUnidadeEstaoInativos() {
        Unidade unidade = criarUnidade("EC20");
        criarEstoque(unidade, criarProduto("Produto inativo EC20", "COD-EC20"), 10, 1, false);

        List<EstoqueCentral> resultado = estoqueCentralRepository.findByUnidadeIdAndAtivoTrue(unidade.getId());

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveEncontrarPorUnidadePublicIdApenasAtivos() {
        Unidade unidade = criarUnidade("EC21");
        criarEstoque(unidade, criarProduto("Produto ativo EC21", "COD-EC21-1"), 10, 1, true);
        criarEstoque(unidade, criarProduto("Produto inativo EC21", "COD-EC21-2"), 10, 1, false);

        List<EstoqueCentral> resultado = estoqueCentralRepository
                .findByUnidadePublicIdAndAtivoTrue(unidade.getPublicId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoUnidadePublicIdSoTemEstoqueInativo() {
        Unidade unidade = criarUnidade("EC22");
        criarEstoque(unidade, criarProduto("Produto inativo EC22", "COD-EC22"), 10, 1, false);

        List<EstoqueCentral> resultado = estoqueCentralRepository
                .findByUnidadePublicIdAndAtivoTrue(unidade.getPublicId());

        assertTrue(resultado.isEmpty());
    }

    // --- findByAtivoTrue ---------------------------------------------------------

    @Test
    void deveEncontrarTodosOsEstoquesAtivos() {
        Unidade unidade = criarUnidade("EC23");
        criarEstoque(unidade, criarProduto("Produto ativo EC23", "COD-EC23-1"), 10, 1, true);
        criarEstoque(unidade, criarProduto("Produto inativo EC23", "COD-EC23-2"), 10, 1, false);

        List<EstoqueCentral> resultado = estoqueCentralRepository.findByAtivoTrue();

        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).getAtivo());
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaEstoquesAtivos() {
        Unidade unidade = criarUnidade("EC24");
        criarEstoque(unidade, criarProduto("Produto inativo EC24", "COD-EC24"), 10, 1, false);

        List<EstoqueCentral> resultado = estoqueCentralRepository.findByAtivoTrue();

        assertTrue(resultado.isEmpty());
    }
}
