package com.sgl.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sgl.model.EstoqueCentral;
import com.sgl.model.Lote;
import com.sgl.model.Produto;
import com.sgl.model.Unidade;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.tenant.TenantContext;
import com.sgl.tenant.TenantProvider;

/**
 * Testes de integração do LoteRepository usando @DataJpaTest com H2 real (sem
 * mocks) — persiste entidades via TestEntityManager e valida todos os
 * métodos customizados do repositório, com foco especial nos métodos usados
 * em FIFO/FEFO (buscarDisponiveisPorFefoComBloqueio,
 * buscarDisponiveisPorEntradaComBloqueio, buscarVencidosComBloqueio —
 * docs/testes.md itens 5-8, 12).
 *
 * Segue o mesmo padrão dos demais Repositories desta fase:
 * - @ActiveProfiles("test") ativa application-test.properties (H2), já que o
 *   profile padrão da aplicação ("dev") usaria PostgreSQL real.
 * - @Import(TenantProvider.class) disponibiliza o bean "tenantProvider" usado
 *   pela expressão SpEL do findAll() sobrescrito.
 *
 * ATENÇÃO (não é bug a corrigir, apenas observação): diferente de
 * EstoqueCentral/Produto, a classe Lote NÃO usa @Builder do Lombok — o
 * campo "codigoInterno" só pode ser preenchido pelo método
 * definirCodigoInterno(...), que é a forma correta (e única) de simular o
 * fluxo real de criação de lote (ver MovimentacaoEstoqueService.java, que
 * usa exatamente esse método). Por isso o helper criarLote() usa
 * "new Lote()" + setters + definirCodigoInterno(...) em vez de builder.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TenantProvider.class)
class LoteRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LoteRepository loteRepository;

    private final AtomicInteger sequencialGerador = new AtomicInteger(1);

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

    private EstoqueCentral criarEstoque(Unidade unidade, Produto produto) {
        EstoqueCentral estoque = EstoqueCentral.builder()
                .unidade(unidade)
                .produto(produto)
                .quantidadeAtual(100)
                .quantidadeMinima(1)
                .ativo(true)
                .build();
        return entityManager.persistAndFlush(estoque);
    }

    private Lote criarLote(EstoqueCentral estoqueCentral, String numeroLote, int quantidade,
            LocalDate dataEntrada, LocalDate dataValidade, boolean ativo) {
        Lote lote = new Lote();
        lote.setEstoqueCentral(estoqueCentral);
        lote.setNumeroLote(numeroLote);
        lote.definirCodigoInterno("LOT-" + numeroLote + "-" + sequencialGerador.get(), sequencialGerador.getAndIncrement());
        lote.setQuantidadeInicial(quantidade);
        lote.setQuantidadeDisponivel(quantidade);
        lote.setDataEntrada(dataEntrada);
        lote.setDataValidade(dataValidade);
        lote.setAtivo(ativo);
        return entityManager.persistAndFlush(lote);
    }

    // --- findByPublicId -------------------------------------------------------

    @Test
    void deveEncontrarPorPublicId() {
        Unidade unidade = criarUnidade("LT1");
        EstoqueCentral estoque = criarEstoque(unidade, criarProduto("Produto LT1", "COD-LT1"));
        Lote lote = criarLote(estoque, "L001", 10, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), true);

        Optional<Lote> resultado = loteRepository.findByPublicId(lote.getPublicId());

        assertTrue(resultado.isPresent());
    }

    @Test
    void naoDeveEncontrarPorPublicIdInexistente() {
        Optional<Lote> resultado = loteRepository.findByPublicId(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    // --- findByPublicIdAndEstoqueCentralUnidadePublicId -------------------------

    @Test
    void deveEncontrarPorPublicIdEUnidadeDoEstoque() {
        Unidade unidade = criarUnidade("LT2");
        EstoqueCentral estoque = criarEstoque(unidade, criarProduto("Produto LT2", "COD-LT2"));
        Lote lote = criarLote(estoque, "L002", 10, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), true);

        Optional<Lote> resultado = loteRepository
                .findByPublicIdAndEstoqueCentralUnidadePublicId(lote.getPublicId(), unidade.getPublicId());

        assertTrue(resultado.isPresent());
    }

    @Test
    void naoDeveEncontrarQuandoUnidadeInformadaNaoCasaComOEstoqueDoLote() {
        Unidade unidade = criarUnidade("LT3");
        Unidade outraUnidade = criarUnidade("LT4");
        EstoqueCentral estoque = criarEstoque(unidade, criarProduto("Produto LT3", "COD-LT3"));
        Lote lote = criarLote(estoque, "L003", 10, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), true);

        Optional<Lote> resultado = loteRepository
                .findByPublicIdAndEstoqueCentralUnidadePublicId(lote.getPublicId(), outraUnidade.getPublicId());

        assertTrue(resultado.isEmpty());
    }

    // --- findAll() com filtro de tenant via SpEL --------------------------------

    @Test
    void findAllDeveRetornarTodosOsLotesQuandoNenhumTenantEstaDefinido() {
        Unidade unidadeA = criarUnidade("LT5");
        Unidade unidadeB = criarUnidade("LT6");
        criarLote(criarEstoque(unidadeA, criarProduto("Produto A", "COD-LT5")), "L005", 10,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), true);
        criarLote(criarEstoque(unidadeB, criarProduto("Produto B", "COD-LT6")), "L006", 10,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), true);

        List<Lote> resultado = loteRepository.findAll();

        assertEquals(2, resultado.size());
    }

    @Test
    void findAllDeveFiltrarApenasLoteDaUnidadeDoTenantAtual() {
        Unidade unidadeA = criarUnidade("LT7");
        Unidade unidadeB = criarUnidade("LT8");
        Lote loteA = criarLote(criarEstoque(unidadeA, criarProduto("Produto A2", "COD-LT7")), "L007", 10,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), true);
        criarLote(criarEstoque(unidadeB, criarProduto("Produto B2", "COD-LT8")), "L008", 10,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), true);

        TenantContext.definir(unidadeA.getPublicId());

        List<Lote> resultado = loteRepository.findAll();

        assertEquals(1, resultado.size());
        assertEquals(loteA.getId(), resultado.get(0).getId());
    }

    // --- findByEstoqueCentralId / findByEstoqueCentralUnidadePublicId -----------

    @Test
    void deveEncontrarPorEstoqueCentralId() {
        Unidade unidade = criarUnidade("LT9");
        EstoqueCentral estoque = criarEstoque(unidade, criarProduto("Produto LT9", "COD-LT9"));
        criarLote(estoque, "L009", 10, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), true);

        List<Lote> resultado = loteRepository.findByEstoqueCentralId(estoque.getId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoEstoqueCentralIdNaoTemLote() {
        Unidade unidade = criarUnidade("LT10");
        EstoqueCentral estoque = criarEstoque(unidade, criarProduto("Produto LT10", "COD-LT10"));

        List<Lote> resultado = loteRepository.findByEstoqueCentralId(estoque.getId());

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveEncontrarPorUnidadePublicIdDoEstoque() {
        Unidade unidade = criarUnidade("LT11");
        EstoqueCentral estoque = criarEstoque(unidade, criarProduto("Produto LT11", "COD-LT11"));
        criarLote(estoque, "L011", 10, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), true);

        List<Lote> resultado = loteRepository.findByEstoqueCentralUnidadePublicId(unidade.getPublicId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoUnidadePublicIdNaoTemLote() {
        List<Lote> resultado = loteRepository.findByEstoqueCentralUnidadePublicId(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    // --- findByEstoqueCentralIdAndAtivoTrue -------------------------------------

    @Test
    void deveEncontrarPorEstoqueCentralIdApenasAtivos() {
        Unidade unidade = criarUnidade("LT12");
        EstoqueCentral estoque = criarEstoque(unidade, criarProduto("Produto LT12", "COD-LT12"));
        criarLote(estoque, "L012A", 10, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), true);
        criarLote(estoque, "L012B", 10, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), false);

        List<Lote> resultado = loteRepository.findByEstoqueCentralIdAndAtivoTrue(estoque.getId());

        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).getAtivo());
    }

    @Test
    void deveRetornarListaVaziaQuandoTodosOsLotesDoEstoqueEstaoInativos() {
        Unidade unidade = criarUnidade("LT13");
        EstoqueCentral estoque = criarEstoque(unidade, criarProduto("Produto LT13", "COD-LT13"));
        criarLote(estoque, "L013", 10, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), false);

        List<Lote> resultado = loteRepository.findByEstoqueCentralIdAndAtivoTrue(estoque.getId());

        assertTrue(resultado.isEmpty());
    }

    // --- findByEstoqueCentralIdAndNumeroLote / existsByEstoqueCentralIdAndNumeroLote

    @Test
    void deveEncontrarPorEstoqueCentralIdENumeroLote() {
        Unidade unidade = criarUnidade("LT14");
        EstoqueCentral estoque = criarEstoque(unidade, criarProduto("Produto LT14", "COD-LT14"));
        criarLote(estoque, "L014", 10, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), true);

        Optional<Lote> resultado = loteRepository.findByEstoqueCentralIdAndNumeroLote(estoque.getId(), "L014");

        assertTrue(resultado.isPresent());
    }

    @Test
    void naoDeveEncontrarPorEstoqueCentralIdENumeroLoteQuandoNumeroNaoCasa() {
        Unidade unidade = criarUnidade("LT15");
        EstoqueCentral estoque = criarEstoque(unidade, criarProduto("Produto LT15", "COD-LT15"));
        criarLote(estoque, "L015", 10, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), true);

        Optional<Lote> resultado = loteRepository.findByEstoqueCentralIdAndNumeroLote(estoque.getId(), "INEXISTENTE");

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveExistirPorEstoqueCentralIdENumeroLote() {
        Unidade unidade = criarUnidade("LT16");
        EstoqueCentral estoque = criarEstoque(unidade, criarProduto("Produto LT16", "COD-LT16"));
        criarLote(estoque, "L016", 10, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), true);

        assertTrue(loteRepository.existsByEstoqueCentralIdAndNumeroLote(estoque.getId(), "L016"));
    }

    @Test
    void naoDeveExistirPorEstoqueCentralIdENumeroLoteInexistente() {
        Unidade unidade = criarUnidade("LT17");
        EstoqueCentral estoque = criarEstoque(unidade, criarProduto("Produto LT17", "COD-LT17"));

        assertFalse(loteRepository.existsByEstoqueCentralIdAndNumeroLote(estoque.getId(), "INEXISTENTE"));
    }

    // --- existsByCodigoInterno ----------------------------------------------------

    @Test
    void deveExistirPorCodigoInterno() {
        Unidade unidade = criarUnidade("LT18");
        EstoqueCentral estoque = criarEstoque(unidade, criarProduto("Produto LT18", "COD-LT18"));
        Lote lote = criarLote(estoque, "L018", 10, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), true);

        assertTrue(loteRepository.existsByCodigoInterno(lote.getCodigoInterno()));
    }

    @Test
    void naoDeveExistirPorCodigoInternoInexistente() {
        assertFalse(loteRepository.existsByCodigoInterno("CODIGO-INEXISTENTE"));
    }

    // --- buscarMaiorSequencialInternoPorProduto (COALESCE MAX) -------------------

    @Test
    void deveBuscarMaiorSequencialInternoPorProduto() {
        Unidade unidade = criarUnidade("LT19");
        Produto produto = criarProduto("Produto LT19", "COD-LT19");
        EstoqueCentral estoque = criarEstoque(unidade, produto);
        criarLote(estoque, "L019A", 10, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), true);
        criarLote(estoque, "L019B", 10, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), true);

        Integer maiorSequencial = loteRepository.buscarMaiorSequencialInternoPorProduto(produto.getId());

        assertEquals(2, maiorSequencial);
    }

    @Test
    void deveRetornarZeroComoMaiorSequencialQuandoProdutoNaoTemLote() {
        Produto produto = criarProduto("Produto LT20", "COD-LT20");

        Integer maiorSequencial = loteRepository.buscarMaiorSequencialInternoPorProduto(produto.getId());

        assertEquals(0, maiorSequencial);
    }

    // --- findByDataValidadeBeforeAndAtivoTrue -------------------------------------

    @Test
    void deveEncontrarLotesVencidosAtivos() {
        Unidade unidade = criarUnidade("LT21");
        EstoqueCentral estoque = criarEstoque(unidade, criarProduto("Produto LT21", "COD-LT21"));
        criarLote(estoque, "L021", 10, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 30), true);

        List<Lote> resultado = loteRepository.findByDataValidadeBeforeAndAtivoTrue(LocalDate.of(2026, 1, 1));

        assertEquals(1, resultado.size());
    }

    @Test
    void naoDeveEncontrarLotesComValidadeAposADataDeReferencia() {
        Unidade unidade = criarUnidade("LT22");
        EstoqueCentral estoque = criarEstoque(unidade, criarProduto("Produto LT22", "COD-LT22"));
        criarLote(estoque, "L022", 10, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 6, 30), true);

        List<Lote> resultado = loteRepository.findByDataValidadeBeforeAndAtivoTrue(LocalDate.of(2026, 1, 1));

        assertTrue(resultado.isEmpty());
    }

    // --- findByEstoqueCentralUnidadePublicIdAndDataValidadeBeforeAndAtivoTrue ----

    @Test
    void deveEncontrarLotesVencidosDaUnidadeInformada() {
        Unidade unidade = criarUnidade("LT23");
        EstoqueCentral estoque = criarEstoque(unidade, criarProduto("Produto LT23", "COD-LT23"));
        criarLote(estoque, "L023", 10, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 30), true);

        List<Lote> resultado = loteRepository.findByEstoqueCentralUnidadePublicIdAndDataValidadeBeforeAndAtivoTrue(
                unidade.getPublicId(), LocalDate.of(2026, 1, 1));

        assertEquals(1, resultado.size());
    }

    @Test
    void naoDeveEncontrarLotesVencidosDeOutraUnidade() {
        Unidade unidade = criarUnidade("LT24");
        Unidade outraUnidade = criarUnidade("LT25");
        EstoqueCentral estoque = criarEstoque(unidade, criarProduto("Produto LT24", "COD-LT24"));
        criarLote(estoque, "L024", 10, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 30), true);

        List<Lote> resultado = loteRepository.findByEstoqueCentralUnidadePublicIdAndDataValidadeBeforeAndAtivoTrue(
                outraUnidade.getPublicId(), LocalDate.of(2026, 1, 1));

        assertTrue(resultado.isEmpty());
    }

    // --- buscarPorIdComBloqueio (@Lock PESSIMISTIC_WRITE) -----------------------

    @Test
    void deveBuscarPorIdComBloqueioQuandoLoteExiste() {
        Unidade unidade = criarUnidade("LT26");
        EstoqueCentral estoque = criarEstoque(unidade, criarProduto("Produto LT26", "COD-LT26"));
        Lote lote = criarLote(estoque, "L026", 10, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), true);

        Optional<Lote> resultado = loteRepository.buscarPorIdComBloqueio(lote.getId());

        assertTrue(resultado.isPresent());
    }

    @Test
    void naoDeveBuscarPorIdComBloqueioQuandoLoteNaoExiste() {
        Optional<Lote> resultado = loteRepository.buscarPorIdComBloqueio(-1L);

        assertTrue(resultado.isEmpty());
    }

    // --- buscarDisponiveisPorFefoComBloqueio (ordem: validade, entrada, id) -----

    @Test
    void deveBuscarDisponiveisPorFefoOrdenandoPorValidadeAscendente() {
        Unidade unidade = criarUnidade("LT27");
        EstoqueCentral estoque = criarEstoque(unidade, criarProduto("Produto LT27", "COD-LT27"));
        // Insere primeiro o lote que vence depois, para garantir que a ordenação não é por id/inserção.
        Lote loteVenceDepois = criarLote(estoque, "L027A", 10, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), true);
        Lote loteVenceAntes = criarLote(estoque, "L027B", 10, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), true);

        List<Lote> resultado = loteRepository.buscarDisponiveisPorFefoComBloqueio(estoque.getId(), LocalDate.of(2026, 1, 1));

        assertEquals(2, resultado.size());
        assertEquals(loteVenceAntes.getId(), resultado.get(0).getId());
        assertEquals(loteVenceDepois.getId(), resultado.get(1).getId());
    }

    @Test
    void naoDeveBuscarDisponiveisPorFefoQuandoLoteJaVenceu() {
        Unidade unidade = criarUnidade("LT28");
        EstoqueCentral estoque = criarEstoque(unidade, criarProduto("Produto LT28", "COD-LT28"));
        criarLote(estoque, "L028", 10, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 30), true);

        List<Lote> resultado = loteRepository.buscarDisponiveisPorFefoComBloqueio(estoque.getId(), LocalDate.of(2026, 1, 1));

        assertTrue(resultado.isEmpty());
    }

    @Test
    void naoDeveBuscarDisponiveisPorFefoQuandoLoteSemQuantidadeDisponivel() {
        Unidade unidade = criarUnidade("LT29");
        EstoqueCentral estoque = criarEstoque(unidade, criarProduto("Produto LT29", "COD-LT29"));
        Lote lote = criarLote(estoque, "L029", 10, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), true);
        lote.setQuantidadeDisponivel(0);
        entityManager.persistAndFlush(lote);

        List<Lote> resultado = loteRepository.buscarDisponiveisPorFefoComBloqueio(estoque.getId(), LocalDate.of(2026, 1, 1));

        assertTrue(resultado.isEmpty());
    }

    // --- buscarDisponiveisPorEntradaComBloqueio (ordem: entrada, id) ------------

    @Test
    void deveBuscarDisponiveisPorEntradaOrdenandoPorDataDeEntradaAscendente() {
        Unidade unidade = criarUnidade("LT30");
        EstoqueCentral estoque = criarEstoque(unidade, criarProduto("Produto LT30", "COD-LT30"));
        // Insere primeiro o lote de entrada mais recente, para garantir que a ordenação não é por id/inserção.
        Lote loteEntradaRecente = criarLote(estoque, "L030A", 10, LocalDate.of(2026, 6, 1), null, true);
        Lote loteEntradaAntiga = criarLote(estoque, "L030B", 10, LocalDate.of(2026, 1, 1), null, true);

        List<Lote> resultado = loteRepository.buscarDisponiveisPorEntradaComBloqueio(estoque.getId());

        assertEquals(2, resultado.size());
        assertEquals(loteEntradaAntiga.getId(), resultado.get(0).getId());
        assertEquals(loteEntradaRecente.getId(), resultado.get(1).getId());
    }

    @Test
    void naoDeveBuscarDisponiveisPorEntradaQuandoLoteInativo() {
        Unidade unidade = criarUnidade("LT31");
        EstoqueCentral estoque = criarEstoque(unidade, criarProduto("Produto LT31", "COD-LT31"));
        criarLote(estoque, "L031", 10, LocalDate.of(2026, 1, 1), null, false);

        List<Lote> resultado = loteRepository.buscarDisponiveisPorEntradaComBloqueio(estoque.getId());

        assertTrue(resultado.isEmpty());
    }

    // --- buscarVencidosComBloqueio (dataValidade < dataReferencia) ---------------

    @Test
    void deveBuscarVencidosQuandoDataValidadeAnteriorADataReferencia() {
        Unidade unidade = criarUnidade("LT32");
        EstoqueCentral estoque = criarEstoque(unidade, criarProduto("Produto LT32", "COD-LT32"));
        criarLote(estoque, "L032", 10, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 30), true);

        List<Lote> resultado = loteRepository.buscarVencidosComBloqueio(estoque.getId(), LocalDate.of(2026, 1, 1));

        assertEquals(1, resultado.size());
    }

    @Test
    void naoDeveBuscarVencidosQuandoDataValidadeIgualOuPosteriorADataReferencia() {
        Unidade unidade = criarUnidade("LT33");
        EstoqueCentral estoque = criarEstoque(unidade, criarProduto("Produto LT33", "COD-LT33"));
        criarLote(estoque, "L033", 10, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), true);

        List<Lote> resultado = loteRepository.buscarVencidosComBloqueio(estoque.getId(), LocalDate.of(2026, 1, 1));

        assertTrue(resultado.isEmpty());
    }
}
