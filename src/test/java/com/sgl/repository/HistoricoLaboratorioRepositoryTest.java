package com.sgl.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.sgl.model.HistoricoLaboratorio;
import com.sgl.model.Laboratorio;
import com.sgl.model.Pedido;
import com.sgl.model.Produto;
import com.sgl.model.Projeto;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.StatusPedido;
import com.sgl.model.enums.UnidadeMedida;

/**
 * Testes de integração do HistoricoLaboratorioRepository usando @DataJpaTest
 * com H2 real (sem mocks) — persiste entidades via TestEntityManager e
 * valida todos os métodos customizados do repositório, com foco especial em
 * findByLaboratorioIdAndPeriodo/findByLaboratorioProdutoEPeriodo/
 * findByLaboratorioProjetoEPeriodo (docs/testes.md itens 19-20).
 *
 * Diferente dos demais Repositories desta fase, NENHUM método deste
 * repositório usa a expressão SpEL "@tenantProvider" (o findAll() não é
 * sobrescrito aqui), então @Import(TenantProvider.class) não é necessário —
 * os métodos que precisam respeitar o tenant já recebem o
 * "unidadePublicId" explicitamente como parâmetro (ver comentário no fonte
 * do repositório sobre a correção de segurança que introduziu esses
 * métodos).
 *
 * HistoricoLaboratorio tem 3 relacionamentos @ManyToOne obrigatórios
 * (laboratorio, produto e pedido). Um método auxiliar monta a cadeia mínima
 * de dependências (Unidade -> Laboratorio -> Usuario -> Projeto -> Pedido,
 * mais um Produto) reaproveitada pela maioria dos testes.
 *
 * ATENÇÃO (não é bug a corrigir, apenas observação): assim como em Produto,
 * a classe HistoricoLaboratorio usa @Builder do Lombok, mas o campo "ativo"
 * tem apenas inicializador de campo Java ("= true"), SEM @Builder.Default —
 * por isso os testes abaixo setam "ativo" explicitamente em todo
 * HistoricoLaboratorio.builder() para evitar erro de constraint NOT NULL.
 */
@DataJpaTest
@ActiveProfiles("test")
class HistoricoLaboratorioRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private HistoricoLaboratorioRepository historicoLaboratorioRepository;

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

    private HistoricoLaboratorio criarHistorico(Laboratorio laboratorio, Produto produto, Pedido pedido,
            LocalDate dataRecebimento, boolean ativo) {
        HistoricoLaboratorio historico = HistoricoLaboratorio.builder()
                .laboratorio(laboratorio)
                .produto(produto)
                .pedido(pedido)
                .quantidade(5)
                .dataRecebimento(dataRecebimento)
                .ativo(ativo)
                .build();
        return entityManager.persistAndFlush(historico);
    }

    /** Agrupa a cadeia mínima de dependências (Unidade -> Laboratorio -> Usuario -> Projeto -> Pedido -> Produto). */
    private static final class Cenario {
        Unidade unidade;
        Laboratorio laboratorio;
        Usuario usuario;
        Projeto projeto;
        Pedido pedido;
        Produto produto;
    }

    private Cenario montarCenario(String sigla) {
        Cenario cenario = new Cenario();
        cenario.unidade = criarUnidade(sigla);
        cenario.laboratorio = criarLaboratorio(cenario.unidade, "Laboratório " + sigla);
        cenario.usuario = criarUsuario(cenario.unidade, cenario.laboratorio, "usuario" + sigla + "@exemplo.com");
        cenario.projeto = criarProjeto(cenario.laboratorio, "Projeto " + sigla);
        cenario.pedido = criarPedido(cenario.usuario, cenario.laboratorio, cenario.projeto);
        cenario.produto = criarProduto("Produto " + sigla, "COD-" + sigla);
        return cenario;
    }

    // --- findByPublicId -----------------------------------------------------------

    @Test
    void deveEncontrarPorPublicId() {
        Cenario cenario = montarCenario("HL1");
        HistoricoLaboratorio historico = criarHistorico(cenario.laboratorio, cenario.produto, cenario.pedido,
                LocalDate.of(2026, 6, 10), true);

        Optional<HistoricoLaboratorio> resultado = historicoLaboratorioRepository
                .findByPublicId(historico.getPublicId());

        assertTrue(resultado.isPresent());
    }

    @Test
    void naoDeveEncontrarPorPublicIdInexistente() {
        Optional<HistoricoLaboratorio> resultado = historicoLaboratorioRepository.findByPublicId(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    // --- findByPublicIdAndLaboratorioUnidadePublicId -------------------------------

    @Test
    void deveEncontrarPorPublicIdEUnidadeDoLaboratorio() {
        Cenario cenario = montarCenario("HL2");
        HistoricoLaboratorio historico = criarHistorico(cenario.laboratorio, cenario.produto, cenario.pedido,
                LocalDate.of(2026, 6, 10), true);

        Optional<HistoricoLaboratorio> resultado = historicoLaboratorioRepository
                .findByPublicIdAndLaboratorioUnidadePublicId(historico.getPublicId(), cenario.unidade.getPublicId());

        assertTrue(resultado.isPresent());
    }

    @Test
    void naoDeveEncontrarQuandoUnidadeInformadaNaoCasaComOLaboratorioDoHistorico() {
        Cenario cenario = montarCenario("HL3");
        Unidade outraUnidade = criarUnidade("HL4");
        HistoricoLaboratorio historico = criarHistorico(cenario.laboratorio, cenario.produto, cenario.pedido,
                LocalDate.of(2026, 6, 10), true);

        Optional<HistoricoLaboratorio> resultado = historicoLaboratorioRepository
                .findByPublicIdAndLaboratorioUnidadePublicId(historico.getPublicId(), outraUnidade.getPublicId());

        assertTrue(resultado.isEmpty());
    }

    // --- findByLaboratorioUnidadePublicId ------------------------------------------

    @Test
    void deveEncontrarPorUnidadePublicIdDoLaboratorio() {
        Cenario cenario = montarCenario("HL5");
        criarHistorico(cenario.laboratorio, cenario.produto, cenario.pedido, LocalDate.of(2026, 6, 10), true);

        List<HistoricoLaboratorio> resultado = historicoLaboratorioRepository
                .findByLaboratorioUnidadePublicId(cenario.unidade.getPublicId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoUnidadePublicIdNaoCasaComLaboratorioDoHistorico() {
        Cenario cenario = montarCenario("HL6");
        criarHistorico(cenario.laboratorio, cenario.produto, cenario.pedido, LocalDate.of(2026, 6, 10), true);

        List<HistoricoLaboratorio> resultado = historicoLaboratorioRepository
                .findByLaboratorioUnidadePublicId(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    // --- findByLaboratorioId -----------------------------------------------------------

    @Test
    void deveEncontrarPorLaboratorioId() {
        Cenario cenario = montarCenario("HL7");
        criarHistorico(cenario.laboratorio, cenario.produto, cenario.pedido, LocalDate.of(2026, 6, 10), true);

        List<HistoricoLaboratorio> resultado = historicoLaboratorioRepository
                .findByLaboratorioId(cenario.laboratorio.getId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoLaboratorioIdNaoTemHistorico() {
        Cenario cenario = montarCenario("HL8");
        Laboratorio outroLaboratorio = criarLaboratorio(cenario.unidade, "Laboratório sem histórico");

        List<HistoricoLaboratorio> resultado = historicoLaboratorioRepository
                .findByLaboratorioId(outroLaboratorio.getId());

        assertTrue(resultado.isEmpty());
    }

    // --- findByProdutoIdAndLaboratorioUnidadePublicId ------------------------------

    @Test
    void deveEncontrarPorProdutoIdEUnidadeDoLaboratorio() {
        Cenario cenario = montarCenario("HL9");
        criarHistorico(cenario.laboratorio, cenario.produto, cenario.pedido, LocalDate.of(2026, 6, 10), true);

        List<HistoricoLaboratorio> resultado = historicoLaboratorioRepository
                .findByProdutoIdAndLaboratorioUnidadePublicId(cenario.produto.getId(), cenario.unidade.getPublicId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoUnidadeNaoCasaComOLaboratorioDoHistoricoDoProduto() {
        Cenario cenario = montarCenario("HL10");
        criarHistorico(cenario.laboratorio, cenario.produto, cenario.pedido, LocalDate.of(2026, 6, 10), true);

        List<HistoricoLaboratorio> resultado = historicoLaboratorioRepository
                .findByProdutoIdAndLaboratorioUnidadePublicId(cenario.produto.getId(), UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    // --- findByPedidoIdAndLaboratorioUnidadePublicId --------------------------------

    @Test
    void deveEncontrarPorPedidoIdEUnidadeDoLaboratorio() {
        Cenario cenario = montarCenario("HL11");
        criarHistorico(cenario.laboratorio, cenario.produto, cenario.pedido, LocalDate.of(2026, 6, 10), true);

        List<HistoricoLaboratorio> resultado = historicoLaboratorioRepository
                .findByPedidoIdAndLaboratorioUnidadePublicId(cenario.pedido.getId(), cenario.unidade.getPublicId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoUnidadeNaoCasaComOLaboratorioDoHistoricoDoPedido() {
        Cenario cenario = montarCenario("HL12");
        criarHistorico(cenario.laboratorio, cenario.produto, cenario.pedido, LocalDate.of(2026, 6, 10), true);

        List<HistoricoLaboratorio> resultado = historicoLaboratorioRepository
                .findByPedidoIdAndLaboratorioUnidadePublicId(cenario.pedido.getId(), UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    // --- findByLaboratorioIdAndPeriodo -----------------------------------------------
    // BETWEEN é inclusivo nos dois extremos; ordenado por dataRecebimento ASC, id ASC.

    @Test
    void deveEncontrarHistoricoDentroDoPeriodoDoLaboratorio() {
        Cenario cenario = montarCenario("HL13");
        criarHistorico(cenario.laboratorio, cenario.produto, cenario.pedido, LocalDate.of(2026, 6, 15), true);

        List<HistoricoLaboratorio> resultado = historicoLaboratorioRepository.findByLaboratorioIdAndPeriodo(
                cenario.laboratorio.getId(), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertEquals(1, resultado.size());
    }

    @Test
    void deveConsiderarOsLimitesDoPeriodoComoInclusivosNoLaboratorio() {
        Cenario cenario = montarCenario("HL14");
        LocalDate inicio = LocalDate.of(2026, 6, 1);
        LocalDate fim = LocalDate.of(2026, 6, 30);
        criarHistorico(cenario.laboratorio, cenario.produto, cenario.pedido, inicio, true);
        criarHistorico(cenario.laboratorio, cenario.produto, cenario.pedido, fim, true);

        List<HistoricoLaboratorio> resultado = historicoLaboratorioRepository
                .findByLaboratorioIdAndPeriodo(cenario.laboratorio.getId(), inicio, fim);

        assertEquals(2, resultado.size());
    }

    @Test
    void naoDeveEncontrarHistoricoForaDoPeriodoDoLaboratorio() {
        Cenario cenario = montarCenario("HL15");
        criarHistorico(cenario.laboratorio, cenario.produto, cenario.pedido, LocalDate.of(2026, 5, 31), true);

        List<HistoricoLaboratorio> resultado = historicoLaboratorioRepository.findByLaboratorioIdAndPeriodo(
                cenario.laboratorio.getId(), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveOrdenarHistoricoDoLaboratorioPorDataRecebimentoAscendente() {
        Cenario cenario = montarCenario("HL16");
        LocalDate maisRecente = LocalDate.of(2026, 6, 20);
        LocalDate maisAntiga = LocalDate.of(2026, 6, 5);
        // Insere primeiro o mais recente para garantir que a ordenação não é por ordem de inserção/id.
        criarHistorico(cenario.laboratorio, cenario.produto, cenario.pedido, maisRecente, true);
        criarHistorico(cenario.laboratorio, cenario.produto, cenario.pedido, maisAntiga, true);

        List<HistoricoLaboratorio> resultado = historicoLaboratorioRepository.findByLaboratorioIdAndPeriodo(
                cenario.laboratorio.getId(), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertEquals(2, resultado.size());
        assertEquals(maisAntiga, resultado.get(0).getDataRecebimento());
        assertEquals(maisRecente, resultado.get(1).getDataRecebimento());
    }

    // --- findByLaboratorioProdutoEPeriodo (também filtra ativo = true) ----------------

    @Test
    void deveEncontrarHistoricoAtivoDentroDoPeriodoDoLaboratorioEProduto() {
        Cenario cenario = montarCenario("HL17");
        criarHistorico(cenario.laboratorio, cenario.produto, cenario.pedido, LocalDate.of(2026, 6, 15), true);

        List<HistoricoLaboratorio> resultado = historicoLaboratorioRepository.findByLaboratorioProdutoEPeriodo(
                cenario.laboratorio.getId(), cenario.produto.getId(),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertEquals(1, resultado.size());
    }

    @Test
    void naoDeveEncontrarHistoricoInativoMesmoDentroDoPeriodoEProduto() {
        Cenario cenario = montarCenario("HL18");
        criarHistorico(cenario.laboratorio, cenario.produto, cenario.pedido, LocalDate.of(2026, 6, 15), false);

        List<HistoricoLaboratorio> resultado = historicoLaboratorioRepository.findByLaboratorioProdutoEPeriodo(
                cenario.laboratorio.getId(), cenario.produto.getId(),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertTrue(resultado.isEmpty());
    }

    @Test
    void naoDeveEncontrarHistoricoDeOutroProdutoMesmoDentroDoPeriodo() {
        Cenario cenario = montarCenario("HL19");
        Produto outroProduto = criarProduto("Outro Produto", "COD-HL19-2");
        criarHistorico(cenario.laboratorio, outroProduto, cenario.pedido, LocalDate.of(2026, 6, 15), true);

        List<HistoricoLaboratorio> resultado = historicoLaboratorioRepository.findByLaboratorioProdutoEPeriodo(
                cenario.laboratorio.getId(), cenario.produto.getId(),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertTrue(resultado.isEmpty());
    }

    // --- findByLaboratorioProjetoEPeriodo (via pedido.projeto.id) ---------------------

    @Test
    void deveEncontrarHistoricoDentroDoPeriodoDoLaboratorioEProjeto() {
        Cenario cenario = montarCenario("HL20");
        criarHistorico(cenario.laboratorio, cenario.produto, cenario.pedido, LocalDate.of(2026, 6, 15), true);

        List<HistoricoLaboratorio> resultado = historicoLaboratorioRepository.findByLaboratorioProjetoEPeriodo(
                cenario.laboratorio.getId(), cenario.projeto.getId(),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertEquals(1, resultado.size());
    }

    @Test
    void naoDeveEncontrarHistoricoDeOutroProjetoMesmoDentroDoPeriodo() {
        Cenario cenario = montarCenario("HL21");
        Projeto outroProjeto = criarProjeto(cenario.laboratorio, "Outro Projeto");
        Pedido outroPedido = criarPedido(cenario.usuario, cenario.laboratorio, outroProjeto);
        criarHistorico(cenario.laboratorio, cenario.produto, outroPedido, LocalDate.of(2026, 6, 15), true);

        List<HistoricoLaboratorio> resultado = historicoLaboratorioRepository.findByLaboratorioProjetoEPeriodo(
                cenario.laboratorio.getId(), cenario.projeto.getId(),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertTrue(resultado.isEmpty());
    }

    @Test
    void naoDeveEncontrarHistoricoDeProjetoForaDoPeriodo() {
        Cenario cenario = montarCenario("HL22");
        criarHistorico(cenario.laboratorio, cenario.produto, cenario.pedido, LocalDate.of(2026, 5, 31), true);

        List<HistoricoLaboratorio> resultado = historicoLaboratorioRepository.findByLaboratorioProjetoEPeriodo(
                cenario.laboratorio.getId(), cenario.projeto.getId(),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertTrue(resultado.isEmpty());
    }
}
