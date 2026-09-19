package com.sgl.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sgl.model.Laboratorio;
import com.sgl.model.Pedido;
import com.sgl.model.Projeto;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.StatusPedido;
import com.sgl.tenant.TenantContext;
import com.sgl.tenant.TenantProvider;

/**
 * Testes de integração do PedidoRepository usando @DataJpaTest com H2 real
 * (sem mocks). Segue o mesmo padrão dos demais Repositories desta fase:
 * - @ActiveProfiles("test") ativa application-test.properties (H2), já que o
 *   profile padrão da aplicação ("dev") usaria PostgreSQL real.
 * - @Import(TenantProvider.class) disponibiliza o bean "tenantProvider" usado
 *   pela expressão SpEL do findAll() sobrescrito.
 *
 * Pedido tem 3 relacionamentos @ManyToOne (usuario, laboratorio e projeto),
 * sendo usuario e laboratorio obrigatórios (nullable = false). Um @BeforeEach
 * monta a cadeia mínima de dependências (Unidade -> Laboratorio -> Usuario e
 * Projeto) reaproveitada pela maioria dos testes; casos que precisam de uma
 * segunda unidade/laboratorio/usuário/projeto (para provar que o filtro NÃO
 * casa) criam suas próprias entidades extras.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TenantProvider.class)
class PedidoRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PedidoRepository pedidoRepository;

    private Unidade unidade;
    private Laboratorio laboratorio;
    private Usuario usuario;
    private Projeto projeto;

    @BeforeEach
    void montarCadeiaBasica() {
        unidade = criarUnidade("PD1");
        laboratorio = criarLaboratorio(unidade, "Laboratório Base");
        usuario = criarUsuario(unidade, laboratorio, "usuario@exemplo.com");
        projeto = criarProjeto(laboratorio, "Projeto Base");
    }

    @AfterEach
    void limparTenant() {
        TenantContext.limpar();
    }

    private Unidade criarUnidade(String sigla) {
        Unidade u = Unidade.builder()
                .nome("Unidade " + sigla)
                .sigla(sigla)
                .build();
        return entityManager.persistAndFlush(u);
    }

    private Laboratorio criarLaboratorio(Unidade unidade, String nome) {
        Laboratorio l = Laboratorio.builder()
                .unidade(unidade)
                .nome(nome)
                .ativo(true)
                .build();
        return entityManager.persistAndFlush(l);
    }

    private Usuario criarUsuario(Unidade unidade, Laboratorio laboratorio, String email) {
        Usuario u = new Usuario();
        u.setNome("Usuário " + email);
        u.setEmail(email);
        u.setSenha("senha-123");
        u.setPerfil(Perfil.TECNICO);
        u.setUnidade(unidade);
        u.setLaboratorio(laboratorio);
        u.setAtivo(true);
        return entityManager.persistAndFlush(u);
    }

    private Projeto criarProjeto(Laboratorio laboratorio, String nome) {
        Projeto p = Projeto.builder()
                .laboratorio(laboratorio)
                .nome(nome)
                .ativo(true)
                .build();
        return entityManager.persistAndFlush(p);
    }

    private Pedido criarPedido(Usuario usuario, Laboratorio laboratorio, Projeto projeto,
            LocalDateTime dataSolicitacao, StatusPedido status, boolean urgente) {
        Pedido pedido = Pedido.builder()
                .usuario(usuario)
                .laboratorio(laboratorio)
                .projeto(projeto)
                .dataSolicitacao(dataSolicitacao)
                .status(status)
                .urgente(urgente)
                .build();
        return entityManager.persistAndFlush(pedido);
    }

    // --- findByPublicId ---------------------------------------------------

    @Test
    void deveEncontrarPorPublicId() {
        Pedido pedido = criarPedido(usuario, laboratorio, projeto,
                LocalDateTime.now(), StatusPedido.PENDENTE, false);

        Optional<Pedido> resultado = pedidoRepository.findByPublicId(pedido.getPublicId());

        assertTrue(resultado.isPresent());
    }

    @Test
    void naoDeveEncontrarPorPublicIdInexistente() {
        Optional<Pedido> resultado = pedidoRepository.findByPublicId(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    // --- findByPublicIdAndLaboratorioUnidadePublicId -----------------------

    @Test
    void deveEncontrarPorPublicIdEUnidadeDoLaboratorio() {
        Pedido pedido = criarPedido(usuario, laboratorio, projeto,
                LocalDateTime.now(), StatusPedido.PENDENTE, false);

        Optional<Pedido> resultado = pedidoRepository
                .findByPublicIdAndLaboratorioUnidadePublicId(pedido.getPublicId(), unidade.getPublicId());

        assertTrue(resultado.isPresent());
    }

    @Test
    void naoDeveEncontrarPorPublicIdQuandoUnidadeNaoCasaComALaboratorioDoPedido() {
        Unidade outraUnidade = criarUnidade("PD2");
        Pedido pedido = criarPedido(usuario, laboratorio, projeto,
                LocalDateTime.now(), StatusPedido.PENDENTE, false);

        Optional<Pedido> resultado = pedidoRepository
                .findByPublicIdAndLaboratorioUnidadePublicId(pedido.getPublicId(), outraUnidade.getPublicId());

        assertTrue(resultado.isEmpty());
    }

    // --- findAll() com filtro de tenant -------------------------------------

    @Test
    void findAllDeveRetornarTodosOsPedidosQuandoNenhumTenantEstaDefinido() {
        criarPedido(usuario, laboratorio, projeto, LocalDateTime.now(), StatusPedido.PENDENTE, false);
        criarPedido(usuario, laboratorio, projeto, LocalDateTime.now(), StatusPedido.APROVADO, false);

        List<Pedido> resultado = pedidoRepository.findAll();

        assertEquals(2, resultado.size());
    }

    @Test
    void findAllDeveFiltrarApenasPedidosDaUnidadeDoTenantAtual() {
        Unidade outraUnidade = criarUnidade("PD3");
        Laboratorio outroLaboratorio = criarLaboratorio(outraUnidade, "Laboratório Outro");
        Usuario outroUsuario = criarUsuario(outraUnidade, outroLaboratorio, "outro@exemplo.com");

        criarPedido(usuario, laboratorio, projeto, LocalDateTime.now(), StatusPedido.PENDENTE, false);
        criarPedido(outroUsuario, outroLaboratorio, null, LocalDateTime.now(), StatusPedido.PENDENTE, false);

        TenantContext.definir(unidade.getPublicId());

        List<Pedido> resultado = pedidoRepository.findAll();

        assertEquals(1, resultado.size());
    }

    // --- buscarPorIdComBloqueio (@Lock PESSIMISTIC_WRITE) -------------------

    @Test
    void deveBuscarPorIdComBloqueioQuandoPedidoExiste() {
        Pedido pedido = criarPedido(usuario, laboratorio, projeto,
                LocalDateTime.now(), StatusPedido.PENDENTE, false);

        Optional<Pedido> resultado = pedidoRepository.buscarPorIdComBloqueio(pedido.getId());

        assertTrue(resultado.isPresent());
        assertEquals(pedido.getId(), resultado.get().getId());
    }

    @Test
    void naoDeveBuscarPorIdComBloqueioQuandoPedidoNaoExiste() {
        Optional<Pedido> resultado = pedidoRepository.buscarPorIdComBloqueio(-1L);

        assertTrue(resultado.isEmpty());
    }

    // --- findByUsuarioId / findByUsuarioIdAndLaboratorioUnidadePublicId -----

    @Test
    void deveEncontrarPorUsuarioId() {
        criarPedido(usuario, laboratorio, projeto, LocalDateTime.now(), StatusPedido.PENDENTE, false);

        List<Pedido> resultado = pedidoRepository.findByUsuarioId(usuario.getId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoUsuarioIdNaoTemPedidos() {
        Usuario outroUsuario = criarUsuario(unidade, laboratorio, "semPedidos@exemplo.com");

        List<Pedido> resultado = pedidoRepository.findByUsuarioId(outroUsuario.getId());

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveEncontrarPorUsuarioIdEUnidadeDoLaboratorio() {
        criarPedido(usuario, laboratorio, projeto, LocalDateTime.now(), StatusPedido.PENDENTE, false);

        List<Pedido> resultado = pedidoRepository
                .findByUsuarioIdAndLaboratorioUnidadePublicId(usuario.getId(), unidade.getPublicId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoUsuarioIdNaoCasaComUnidadeInformada() {
        Unidade outraUnidade = criarUnidade("PD4");
        criarPedido(usuario, laboratorio, projeto, LocalDateTime.now(), StatusPedido.PENDENTE, false);

        List<Pedido> resultado = pedidoRepository
                .findByUsuarioIdAndLaboratorioUnidadePublicId(usuario.getId(), outraUnidade.getPublicId());

        assertTrue(resultado.isEmpty());
    }

    // --- findByLaboratorioId / findByLaboratorioUnidadePublicId -------------

    @Test
    void deveEncontrarPorLaboratorioId() {
        criarPedido(usuario, laboratorio, projeto, LocalDateTime.now(), StatusPedido.PENDENTE, false);

        List<Pedido> resultado = pedidoRepository.findByLaboratorioId(laboratorio.getId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoLaboratorioIdNaoTemPedidos() {
        Laboratorio outroLaboratorio = criarLaboratorio(unidade, "Laboratório sem pedidos");

        List<Pedido> resultado = pedidoRepository.findByLaboratorioId(outroLaboratorio.getId());

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveEncontrarPorUnidadePublicIdDoLaboratorio() {
        criarPedido(usuario, laboratorio, projeto, LocalDateTime.now(), StatusPedido.PENDENTE, false);

        List<Pedido> resultado = pedidoRepository.findByLaboratorioUnidadePublicId(unidade.getPublicId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoUnidadePublicIdNaoCasaComLaboratorioDoPedido() {
        criarPedido(usuario, laboratorio, projeto, LocalDateTime.now(), StatusPedido.PENDENTE, false);

        List<Pedido> resultado = pedidoRepository.findByLaboratorioUnidadePublicId(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    // --- findByStatus / findByLaboratorioUnidadePublicIdAndStatus -----------

    @Test
    void deveEncontrarPorStatus() {
        criarPedido(usuario, laboratorio, projeto, LocalDateTime.now(), StatusPedido.APROVADO, false);
        criarPedido(usuario, laboratorio, projeto, LocalDateTime.now(), StatusPedido.PENDENTE, false);

        List<Pedido> resultado = pedidoRepository.findByStatus(StatusPedido.APROVADO);

        assertEquals(1, resultado.size());
        assertEquals(StatusPedido.APROVADO, resultado.get(0).getStatus());
    }

    @Test
    void deveRetornarListaVaziaQuandoNenhumPedidoTemOStatusInformado() {
        criarPedido(usuario, laboratorio, projeto, LocalDateTime.now(), StatusPedido.PENDENTE, false);

        List<Pedido> resultado = pedidoRepository.findByStatus(StatusPedido.CANCELADO);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveEncontrarPorUnidadeDoLaboratorioEStatus() {
        criarPedido(usuario, laboratorio, projeto, LocalDateTime.now(), StatusPedido.APROVADO, false);

        List<Pedido> resultado = pedidoRepository
                .findByLaboratorioUnidadePublicIdAndStatus(unidade.getPublicId(), StatusPedido.APROVADO);

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoUnidadeCasaMasStatusNao() {
        criarPedido(usuario, laboratorio, projeto, LocalDateTime.now(), StatusPedido.PENDENTE, false);

        List<Pedido> resultado = pedidoRepository
                .findByLaboratorioUnidadePublicIdAndStatus(unidade.getPublicId(), StatusPedido.APROVADO);

        assertTrue(resultado.isEmpty());
    }

    // --- findByUrgente / findByLaboratorioUnidadePublicIdAndUrgente ---------

    @Test
    void deveEncontrarPedidosUrgentes() {
        criarPedido(usuario, laboratorio, projeto, LocalDateTime.now(), StatusPedido.PENDENTE, true);
        criarPedido(usuario, laboratorio, projeto, LocalDateTime.now(), StatusPedido.PENDENTE, false);

        List<Pedido> resultado = pedidoRepository.findByUrgente(true);

        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).getUrgente());
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaPedidosUrgentes() {
        criarPedido(usuario, laboratorio, projeto, LocalDateTime.now(), StatusPedido.PENDENTE, false);

        List<Pedido> resultado = pedidoRepository.findByUrgente(true);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveEncontrarPorUnidadeDoLaboratorioEUrgente() {
        criarPedido(usuario, laboratorio, projeto, LocalDateTime.now(), StatusPedido.PENDENTE, true);

        List<Pedido> resultado = pedidoRepository
                .findByLaboratorioUnidadePublicIdAndUrgente(unidade.getPublicId(), true);

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoUnidadeCasaMasNaoHaUrgentes() {
        criarPedido(usuario, laboratorio, projeto, LocalDateTime.now(), StatusPedido.PENDENTE, false);

        List<Pedido> resultado = pedidoRepository
                .findByLaboratorioUnidadePublicIdAndUrgente(unidade.getPublicId(), true);

        assertTrue(resultado.isEmpty());
    }

    // --- findByLaboratorioIdAndStatus ---------------------------------------

    @Test
    void deveEncontrarPorLaboratorioIdEStatus() {
        criarPedido(usuario, laboratorio, projeto, LocalDateTime.now(), StatusPedido.ENTREGUE, false);

        List<Pedido> resultado = pedidoRepository
                .findByLaboratorioIdAndStatus(laboratorio.getId(), StatusPedido.ENTREGUE);

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoLaboratorioCasaMasStatusNao() {
        criarPedido(usuario, laboratorio, projeto, LocalDateTime.now(), StatusPedido.PENDENTE, false);

        List<Pedido> resultado = pedidoRepository
                .findByLaboratorioIdAndStatus(laboratorio.getId(), StatusPedido.ENTREGUE);

        assertTrue(resultado.isEmpty());
    }

    // --- findByLaboratorioProjetoEPeriodo ------------------------------------
    // Método por trás de PedidoService.listarPorProjetoEPeriodo (docs/testes.md
    // itens 15-17): retorna pedidos do laboratório e projeto informados, cuja
    // dataSolicitacao esteja dentro do período (BETWEEN é inclusivo nos dois
    // extremos), ordenados por dataSolicitacao ASC e depois id ASC.

    @Test
    void deveEncontrarPedidoDentroDoPeriodoDoLaboratorioEProjeto() {
        LocalDateTime dataSolicitacao = LocalDateTime.of(2026, 6, 15, 10, 0);
        criarPedido(usuario, laboratorio, projeto, dataSolicitacao, StatusPedido.PENDENTE, false);

        LocalDateTime inicio = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime fim = LocalDateTime.of(2026, 6, 30, 23, 59, 59);

        List<Pedido> resultado = pedidoRepository
                .findByLaboratorioProjetoEPeriodo(laboratorio.getId(), projeto.getId(), inicio, fim);

        assertEquals(1, resultado.size());
    }

    @Test
    void deveConsiderarOsLimitesDoPeriodoComoInclusivos() {
        LocalDateTime inicio = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime fim = LocalDateTime.of(2026, 6, 30, 23, 59, 59);
        // Um pedido exatamente na borda de início e outro exatamente na borda de fim.
        criarPedido(usuario, laboratorio, projeto, inicio, StatusPedido.PENDENTE, false);
        criarPedido(usuario, laboratorio, projeto, fim, StatusPedido.PENDENTE, false);

        List<Pedido> resultado = pedidoRepository
                .findByLaboratorioProjetoEPeriodo(laboratorio.getId(), projeto.getId(), inicio, fim);

        assertEquals(2, resultado.size());
    }

    @Test
    void naoDeveEncontrarPedidoForaDoPeriodoInformado() {
        LocalDateTime foraDoPeriodo = LocalDateTime.of(2026, 5, 31, 23, 59, 59);
        criarPedido(usuario, laboratorio, projeto, foraDoPeriodo, StatusPedido.PENDENTE, false);

        LocalDateTime inicio = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime fim = LocalDateTime.of(2026, 6, 30, 23, 59, 59);

        List<Pedido> resultado = pedidoRepository
                .findByLaboratorioProjetoEPeriodo(laboratorio.getId(), projeto.getId(), inicio, fim);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void naoDeveEncontrarPedidoDeOutroProjetoMesmoDentroDoPeriodo() {
        Projeto outroProjeto = criarProjeto(laboratorio, "Outro Projeto");
        LocalDateTime dataSolicitacao = LocalDateTime.of(2026, 6, 15, 10, 0);
        criarPedido(usuario, laboratorio, outroProjeto, dataSolicitacao, StatusPedido.PENDENTE, false);

        LocalDateTime inicio = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime fim = LocalDateTime.of(2026, 6, 30, 23, 59, 59);

        List<Pedido> resultado = pedidoRepository
                .findByLaboratorioProjetoEPeriodo(laboratorio.getId(), projeto.getId(), inicio, fim);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void naoDeveEncontrarPedidoDeOutroLaboratorioMesmoDentroDoPeriodo() {
        Laboratorio outroLaboratorio = criarLaboratorio(unidade, "Outro Laboratório");
        // O projeto pertence ao laboratório original, mas o pedido é registrado em outro laboratório.
        LocalDateTime dataSolicitacao = LocalDateTime.of(2026, 6, 15, 10, 0);
        criarPedido(usuario, outroLaboratorio, projeto, dataSolicitacao, StatusPedido.PENDENTE, false);

        LocalDateTime inicio = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime fim = LocalDateTime.of(2026, 6, 30, 23, 59, 59);

        List<Pedido> resultado = pedidoRepository
                .findByLaboratorioProjetoEPeriodo(laboratorio.getId(), projeto.getId(), inicio, fim);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveOrdenarResultadoPorDataSolicitacaoAscendente() {
        LocalDateTime maisRecente = LocalDateTime.of(2026, 6, 20, 10, 0);
        LocalDateTime maisAntiga = LocalDateTime.of(2026, 6, 5, 10, 0);
        // Insere primeiro o mais recente para garantir que a ordenação não é por ordem de inserção/id.
        criarPedido(usuario, laboratorio, projeto, maisRecente, StatusPedido.PENDENTE, false);
        criarPedido(usuario, laboratorio, projeto, maisAntiga, StatusPedido.PENDENTE, false);

        LocalDateTime inicio = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime fim = LocalDateTime.of(2026, 6, 30, 23, 59, 59);

        List<Pedido> resultado = pedidoRepository
                .findByLaboratorioProjetoEPeriodo(laboratorio.getId(), projeto.getId(), inicio, fim);

        assertEquals(2, resultado.size());
        assertEquals(maisAntiga, resultado.get(0).getDataSolicitacao());
        assertEquals(maisRecente, resultado.get(1).getDataSolicitacao());
    }
}
