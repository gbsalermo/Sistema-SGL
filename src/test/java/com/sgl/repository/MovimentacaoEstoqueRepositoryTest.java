package com.sgl.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
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
import com.sgl.model.Laboratorio;
import com.sgl.model.Lote;
import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.Pedido;
import com.sgl.model.Produto;
import com.sgl.model.Projeto;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.StatusPedido;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.tenant.TenantContext;
import com.sgl.tenant.TenantProvider;

/**
 * Testes de integração do MovimentacaoEstoqueRepository usando @DataJpaTest
 * com H2 real (sem mocks) — persiste entidades via TestEntityManager e
 * valida todos os métodos customizados do repositório, com foco especial nos
 * métodos usados em FIFO/FEFO (listarPorLote / ordenação por
 * dataMovimentacao — docs/testes.md itens 5-8, 12).
 *
 * Segue o mesmo padrão dos demais Repositories desta fase:
 * - @ActiveProfiles("test") ativa application-test.properties (H2), já que o
 *   profile padrão da aplicação ("dev") usaria PostgreSQL real.
 * - @Import(TenantProvider.class) disponibiliza o bean "tenantProvider" usado
 *   pela expressão SpEL do findAll() sobrescrito e da maioria dos outros
 *   métodos @Query deste repositório (todos filtram por
 *   estoqueCentral.unidade.publicId).
 *
 * ATENÇÃO (não é bug a corrigir, apenas observação): assim como em
 * Produto/EstoqueCentral, a classe MovimentacaoEstoque usa @Builder do
 * Lombok — aqui todos os campos NOT NULL são setados explicitamente no
 * builder porque nenhum deles tem valor de inicialização de campo Java, então
 * o comportamento é o esperado.
 *
 * MovimentacaoEstoque tem 3 relacionamentos @ManyToOne obrigatórios (produto,
 * usuario, estoqueCentral) e 3 opcionais (laboratorio, pedido, lote). Um
 * método auxiliar monta a cadeia mínima de dependências reaproveitada pela
 * maioria dos testes.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TenantProvider.class)
class MovimentacaoEstoqueRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

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

    private Laboratorio criarLaboratorio(Unidade unidade, String nome) {
        Laboratorio laboratorio = Laboratorio.builder()
                .unidade(unidade)
                .nome(nome)
                .ativo(true)
                .build();
        return entityManager.persistAndFlush(laboratorio);
    }

    private Usuario criarUsuario(Unidade unidade, Laboratorio laboratorio, String email) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário " + email);
        usuario.setEmail(email);
        usuario.setSenha("senha-123");
        usuario.setPerfil(Perfil.TECNICO);
        usuario.setUnidade(unidade);
        usuario.setLaboratorio(laboratorio);
        usuario.setAtivo(true);
        return entityManager.persistAndFlush(usuario);
    }

    private Projeto criarProjeto(Laboratorio laboratorio, String nome) {
        Projeto projeto = Projeto.builder()
                .laboratorio(laboratorio)
                .nome(nome)
                .ativo(true)
                .build();
        return entityManager.persistAndFlush(projeto);
    }

    private Pedido criarPedido(Usuario usuario, Laboratorio laboratorio, Projeto projeto) {
        Pedido pedido = Pedido.builder()
                .usuario(usuario)
                .laboratorio(laboratorio)
                .projeto(projeto)
                .dataSolicitacao(LocalDateTime.now())
                .status(StatusPedido.ENTREGUE)
                .urgente(false)
                .build();
        return entityManager.persistAndFlush(pedido);
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

    private Lote criarLote(EstoqueCentral estoqueCentral, String numeroLote) {
        Lote lote = new Lote();
        lote.setEstoqueCentral(estoqueCentral);
        lote.setNumeroLote(numeroLote);
        lote.definirCodigoInterno("LOT-" + numeroLote, 1);
        lote.setQuantidadeInicial(50);
        lote.setQuantidadeDisponivel(50);
        lote.setDataEntrada(LocalDateTime.now().toLocalDate());
        lote.setAtivo(true);
        return entityManager.persistAndFlush(lote);
    }

    private MovimentacaoEstoque criarMovimentacao(EstoqueCentral estoque, Produto produto, Usuario usuario,
            Laboratorio laboratorio, Pedido pedido, Lote lote, TipoMovimentacao tipo, LocalDateTime dataMovimentacao) {
        MovimentacaoEstoque movimentacao = MovimentacaoEstoque.builder()
                .produto(produto)
                .laboratorio(laboratorio)
                .usuario(usuario)
                .pedido(pedido)
                .lote(lote)
                .tipoMovimentacao(tipo)
                .origem(OrigemMovimentacao.AJUSTE)
                .quantidadeMovimentada(5)
                .quantidadeAnterior(10)
                .quantidadeAtual(5)
                .dataMovimentacao(dataMovimentacao)
                .estoqueCentral(estoque)
                .build();
        return entityManager.persistAndFlush(movimentacao);
    }

    /** Agrupa a cadeia mínima de dependências reaproveitada pela maioria dos testes. */
    private static final class Cenario {
        Unidade unidade;
        Laboratorio laboratorio;
        Usuario usuario;
        Projeto projeto;
        Pedido pedido;
        Produto produto;
        EstoqueCentral estoque;
    }

    private Cenario montarCenario(String sigla) {
        Cenario cenario = new Cenario();
        cenario.unidade = criarUnidade(sigla);
        cenario.laboratorio = criarLaboratorio(cenario.unidade, "Laboratório " + sigla);
        cenario.usuario = criarUsuario(cenario.unidade, cenario.laboratorio, "usuario" + sigla + "@exemplo.com");
        cenario.projeto = criarProjeto(cenario.laboratorio, "Projeto " + sigla);
        cenario.pedido = criarPedido(cenario.usuario, cenario.laboratorio, cenario.projeto);
        cenario.produto = criarProduto("Produto " + sigla, "COD-" + sigla);
        cenario.estoque = criarEstoque(cenario.unidade, cenario.produto);
        return cenario;
    }

    // --- findByPublicId -------------------------------------------------------

    @Test
    void deveEncontrarPorPublicId() {
        Cenario cenario = montarCenario("ME1");
        MovimentacaoEstoque movimentacao = criarMovimentacao(cenario.estoque, cenario.produto, cenario.usuario,
                cenario.laboratorio, cenario.pedido, null, TipoMovimentacao.SAIDA, LocalDateTime.now());

        Optional<MovimentacaoEstoque> resultado = movimentacaoEstoqueRepository
                .findByPublicId(movimentacao.getPublicId());

        assertTrue(resultado.isPresent());
    }

    @Test
    void naoDeveEncontrarPorPublicIdInexistente() {
        Optional<MovimentacaoEstoque> resultado = movimentacaoEstoqueRepository.findByPublicId(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    // --- findByPublicIdAndEstoqueCentralUnidadePublicId -------------------------

    @Test
    void deveEncontrarPorPublicIdEUnidadeDoEstoque() {
        Cenario cenario = montarCenario("ME2");
        MovimentacaoEstoque movimentacao = criarMovimentacao(cenario.estoque, cenario.produto, cenario.usuario,
                cenario.laboratorio, cenario.pedido, null, TipoMovimentacao.SAIDA, LocalDateTime.now());

        Optional<MovimentacaoEstoque> resultado = movimentacaoEstoqueRepository
                .findByPublicIdAndEstoqueCentralUnidadePublicId(movimentacao.getPublicId(), cenario.unidade.getPublicId());

        assertTrue(resultado.isPresent());
    }

    @Test
    void naoDeveEncontrarQuandoUnidadeInformadaNaoCasaComOEstoqueDaMovimentacao() {
        Cenario cenario = montarCenario("ME3");
        Unidade outraUnidade = criarUnidade("ME4");
        MovimentacaoEstoque movimentacao = criarMovimentacao(cenario.estoque, cenario.produto, cenario.usuario,
                cenario.laboratorio, cenario.pedido, null, TipoMovimentacao.SAIDA, LocalDateTime.now());

        Optional<MovimentacaoEstoque> resultado = movimentacaoEstoqueRepository
                .findByPublicIdAndEstoqueCentralUnidadePublicId(movimentacao.getPublicId(), outraUnidade.getPublicId());

        assertTrue(resultado.isEmpty());
    }

    // --- findByEstoqueCentralUnidadePublicId ------------------------------------

    @Test
    void deveEncontrarPorUnidadePublicIdDoEstoque() {
        Cenario cenario = montarCenario("ME5");
        criarMovimentacao(cenario.estoque, cenario.produto, cenario.usuario, cenario.laboratorio, cenario.pedido,
                null, TipoMovimentacao.SAIDA, LocalDateTime.now());

        List<MovimentacaoEstoque> resultado = movimentacaoEstoqueRepository
                .findByEstoqueCentralUnidadePublicId(cenario.unidade.getPublicId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoUnidadePublicIdNaoTemMovimentacao() {
        List<MovimentacaoEstoque> resultado = movimentacaoEstoqueRepository
                .findByEstoqueCentralUnidadePublicId(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    // --- findAll() com filtro de tenant via SpEL, ordenado por dataMovimentacao DESC

    @Test
    void findAllDeveRetornarTodasAsMovimentacoesQuandoNenhumTenantEstaDefinido() {
        Cenario cenarioA = montarCenario("ME6");
        Cenario cenarioB = montarCenario("ME7");
        criarMovimentacao(cenarioA.estoque, cenarioA.produto, cenarioA.usuario, cenarioA.laboratorio,
                cenarioA.pedido, null, TipoMovimentacao.SAIDA, LocalDateTime.now());
        criarMovimentacao(cenarioB.estoque, cenarioB.produto, cenarioB.usuario, cenarioB.laboratorio,
                cenarioB.pedido, null, TipoMovimentacao.SAIDA, LocalDateTime.now());

        List<MovimentacaoEstoque> resultado = movimentacaoEstoqueRepository.findAll();

        assertEquals(2, resultado.size());
    }

    @Test
    void findAllDeveFiltrarApenasMovimentacaoDaUnidadeDoTenantAtual() {
        Cenario cenarioA = montarCenario("ME8");
        Cenario cenarioB = montarCenario("ME9");
        MovimentacaoEstoque movimentacaoA = criarMovimentacao(cenarioA.estoque, cenarioA.produto, cenarioA.usuario,
                cenarioA.laboratorio, cenarioA.pedido, null, TipoMovimentacao.SAIDA, LocalDateTime.now());
        criarMovimentacao(cenarioB.estoque, cenarioB.produto, cenarioB.usuario, cenarioB.laboratorio,
                cenarioB.pedido, null, TipoMovimentacao.SAIDA, LocalDateTime.now());

        TenantContext.definir(cenarioA.unidade.getPublicId());

        List<MovimentacaoEstoque> resultado = movimentacaoEstoqueRepository.findAll();

        assertEquals(1, resultado.size());
        assertEquals(movimentacaoA.getId(), resultado.get(0).getId());
    }

    @Test
    void findAllDeveOrdenarPorDataMovimentacaoDescendente() {
        Cenario cenario = montarCenario("ME10");
        LocalDateTime maisAntiga = LocalDateTime.now().minusDays(2);
        LocalDateTime maisRecente = LocalDateTime.now();
        // Insere primeiro a mais antiga para garantir que a ordenação não é por ordem de inserção/id.
        MovimentacaoEstoque movimentacaoAntiga = criarMovimentacao(cenario.estoque, cenario.produto, cenario.usuario,
                cenario.laboratorio, cenario.pedido, null, TipoMovimentacao.ENTRADA, maisAntiga);
        MovimentacaoEstoque movimentacaoRecente = criarMovimentacao(cenario.estoque, cenario.produto, cenario.usuario,
                cenario.laboratorio, cenario.pedido, null, TipoMovimentacao.SAIDA, maisRecente);

        List<MovimentacaoEstoque> resultado = movimentacaoEstoqueRepository.findAll();

        assertEquals(2, resultado.size());
        assertEquals(movimentacaoRecente.getId(), resultado.get(0).getId());
        assertEquals(movimentacaoAntiga.getId(), resultado.get(1).getId());
    }

    // --- findByProdutoId (filtro de tenant via SpEL) ----------------------------

    @Test
    void deveEncontrarPorProdutoId() {
        Cenario cenario = montarCenario("ME11");
        criarMovimentacao(cenario.estoque, cenario.produto, cenario.usuario, cenario.laboratorio, cenario.pedido,
                null, TipoMovimentacao.SAIDA, LocalDateTime.now());

        List<MovimentacaoEstoque> resultado = movimentacaoEstoqueRepository.findByProdutoId(cenario.produto.getId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoProdutoIdNaoTemMovimentacao() {
        Cenario cenario = montarCenario("ME12");
        Produto outroProduto = criarProduto("Outro Produto ME12", "COD-ME12-2");

        List<MovimentacaoEstoque> resultado = movimentacaoEstoqueRepository.findByProdutoId(outroProduto.getId());

        assertTrue(resultado.isEmpty());
    }

    // --- findByLaboratorioId (filtro de tenant via SpEL) ------------------------

    @Test
    void deveEncontrarPorLaboratorioId() {
        Cenario cenario = montarCenario("ME13");
        criarMovimentacao(cenario.estoque, cenario.produto, cenario.usuario, cenario.laboratorio, cenario.pedido,
                null, TipoMovimentacao.SAIDA, LocalDateTime.now());

        List<MovimentacaoEstoque> resultado = movimentacaoEstoqueRepository
                .findByLaboratorioId(cenario.laboratorio.getId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoLaboratorioIdNaoTemMovimentacao() {
        Cenario cenario = montarCenario("ME14");
        Laboratorio outroLaboratorio = criarLaboratorio(cenario.unidade, "Outro Laboratório ME14");

        List<MovimentacaoEstoque> resultado = movimentacaoEstoqueRepository
                .findByLaboratorioId(outroLaboratorio.getId());

        assertTrue(resultado.isEmpty());
    }

    // --- findByPedidoId (filtro de tenant via SpEL) -----------------------------

    @Test
    void deveEncontrarPorPedidoId() {
        Cenario cenario = montarCenario("ME15");
        criarMovimentacao(cenario.estoque, cenario.produto, cenario.usuario, cenario.laboratorio, cenario.pedido,
                null, TipoMovimentacao.SAIDA, LocalDateTime.now());

        List<MovimentacaoEstoque> resultado = movimentacaoEstoqueRepository.findByPedidoId(cenario.pedido.getId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoPedidoIdNaoTemMovimentacao() {
        Cenario cenario = montarCenario("ME16");
        Pedido outroPedido = criarPedido(cenario.usuario, cenario.laboratorio, cenario.projeto);

        List<MovimentacaoEstoque> resultado = movimentacaoEstoqueRepository.findByPedidoId(outroPedido.getId());

        assertTrue(resultado.isEmpty());
    }

    // --- findByLoteIdOrderByDataMovimentacaoDesc (filtro de tenant via SpEL) ----

    @Test
    void deveEncontrarPorLoteIdOrdenadoPorDataMovimentacaoDescendente() {
        Cenario cenario = montarCenario("ME17");
        Lote lote = criarLote(cenario.estoque, "L-ME17");
        LocalDateTime maisAntiga = LocalDateTime.now().minusDays(1);
        LocalDateTime maisRecente = LocalDateTime.now();
        MovimentacaoEstoque movimentacaoAntiga = criarMovimentacao(cenario.estoque, cenario.produto, cenario.usuario,
                cenario.laboratorio, cenario.pedido, lote, TipoMovimentacao.ENTRADA, maisAntiga);
        MovimentacaoEstoque movimentacaoRecente = criarMovimentacao(cenario.estoque, cenario.produto, cenario.usuario,
                cenario.laboratorio, cenario.pedido, lote, TipoMovimentacao.SAIDA, maisRecente);

        List<MovimentacaoEstoque> resultado = movimentacaoEstoqueRepository
                .findByLoteIdOrderByDataMovimentacaoDesc(lote.getId());

        assertEquals(2, resultado.size());
        assertEquals(movimentacaoRecente.getId(), resultado.get(0).getId());
        assertEquals(movimentacaoAntiga.getId(), resultado.get(1).getId());
    }

    @Test
    void deveRetornarListaVaziaQuandoLoteIdNaoTemMovimentacao() {
        Cenario cenario = montarCenario("ME18");
        Lote lote = criarLote(cenario.estoque, "L-ME18");

        List<MovimentacaoEstoque> resultado = movimentacaoEstoqueRepository
                .findByLoteIdOrderByDataMovimentacaoDesc(lote.getId());

        assertTrue(resultado.isEmpty());
    }

    // --- findByPedidoIdAndTipoMovimentacaoOrderByIdAsc (derivado, sem SpEL) -----

    @Test
    void deveEncontrarPorPedidoIdETipoMovimentacaoOrdenadoPorIdAscendente() {
        Cenario cenario = montarCenario("ME19");
        MovimentacaoEstoque primeira = criarMovimentacao(cenario.estoque, cenario.produto, cenario.usuario,
                cenario.laboratorio, cenario.pedido, null, TipoMovimentacao.SAIDA, LocalDateTime.now());
        MovimentacaoEstoque segunda = criarMovimentacao(cenario.estoque, cenario.produto, cenario.usuario,
                cenario.laboratorio, cenario.pedido, null, TipoMovimentacao.SAIDA, LocalDateTime.now());

        List<MovimentacaoEstoque> resultado = movimentacaoEstoqueRepository
                .findByPedidoIdAndTipoMovimentacaoOrderByIdAsc(cenario.pedido.getId(), TipoMovimentacao.SAIDA);

        assertEquals(2, resultado.size());
        assertEquals(primeira.getId(), resultado.get(0).getId());
        assertEquals(segunda.getId(), resultado.get(1).getId());
    }

    @Test
    void deveRetornarListaVaziaQuandoTipoMovimentacaoNaoCasaComOPedido() {
        Cenario cenario = montarCenario("ME20");
        criarMovimentacao(cenario.estoque, cenario.produto, cenario.usuario, cenario.laboratorio, cenario.pedido,
                null, TipoMovimentacao.SAIDA, LocalDateTime.now());

        List<MovimentacaoEstoque> resultado = movimentacaoEstoqueRepository
                .findByPedidoIdAndTipoMovimentacaoOrderByIdAsc(cenario.pedido.getId(), TipoMovimentacao.ENTRADA);

        assertTrue(resultado.isEmpty());
    }

    // --- findByTipoMovimentacao (filtro de tenant via SpEL) ---------------------

    @Test
    void deveEncontrarPorTipoMovimentacao() {
        Cenario cenario = montarCenario("ME21");
        criarMovimentacao(cenario.estoque, cenario.produto, cenario.usuario, cenario.laboratorio, cenario.pedido,
                null, TipoMovimentacao.DEVOLUCAO, LocalDateTime.now());

        List<MovimentacaoEstoque> resultado = movimentacaoEstoqueRepository
                .findByTipoMovimentacao(TipoMovimentacao.DEVOLUCAO);

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoTipoMovimentacaoNaoCasa() {
        Cenario cenario = montarCenario("ME22");
        criarMovimentacao(cenario.estoque, cenario.produto, cenario.usuario, cenario.laboratorio, cenario.pedido,
                null, TipoMovimentacao.DEVOLUCAO, LocalDateTime.now());

        List<MovimentacaoEstoque> resultado = movimentacaoEstoqueRepository
                .findByTipoMovimentacao(TipoMovimentacao.DESCARTE_VENCIMENTO);

        assertTrue(resultado.isEmpty());
    }

    // --- findByUsuarioId (filtro de tenant via SpEL) ----------------------------

    @Test
    void deveEncontrarPorUsuarioId() {
        Cenario cenario = montarCenario("ME23");
        criarMovimentacao(cenario.estoque, cenario.produto, cenario.usuario, cenario.laboratorio, cenario.pedido,
                null, TipoMovimentacao.SAIDA, LocalDateTime.now());

        List<MovimentacaoEstoque> resultado = movimentacaoEstoqueRepository.findByUsuarioId(cenario.usuario.getId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoUsuarioIdNaoTemMovimentacao() {
        Cenario cenario = montarCenario("ME24");
        Usuario outroUsuario = criarUsuario(cenario.unidade, cenario.laboratorio, "outro-me24@exemplo.com");

        List<MovimentacaoEstoque> resultado = movimentacaoEstoqueRepository.findByUsuarioId(outroUsuario.getId());

        assertTrue(resultado.isEmpty());
    }
}
