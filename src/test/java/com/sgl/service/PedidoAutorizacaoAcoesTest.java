package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.sgl.dto.request.AprovarPedidoRequestDTO;
import com.sgl.model.enums.Perfil;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.HistoricoLaboratorioRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.PedidoRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.security.SessaoDeTeste;

/**
 * Approving, rejecting, delivering and cancelling an order move stock and close the request, so the
 * API itself must refuse them to anyone outside management. Hiding the buttons in the web is not a
 * control: any active user could call these endpoints directly.
 */
@ExtendWith(MockitoExtension.class)
class PedidoAutorizacaoAcoesTest {

    private static final UUID SESSAO = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID OUTRO_USUARIO = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final UUID PEDIDO = UUID.fromString("00000000-0000-0000-0000-0000000000a3");

    @Mock private PedidoRepository pedidoRepository;
    @Mock private EstoqueCentralRepository estoqueCentralRepository;
    @Mock private HistoricoLaboratorioRepository historicoLaboratorioRepository;
    @Mock private ProdutoRepository produtoRepository;
    @Mock private LaboratorioRepository laboratorioRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ProjetoRepository projetoRepository;
    @Mock private MovimentacaoEstoqueService movimentacaoEstoqueService;
    @Mock private VinculoOperacionalUsuarioService vinculoOperacionalService;

    @InjectMocks private PedidoService pedidoService;

    @AfterEach
    void sair() {
        SessaoDeTeste.sair();
    }

    private static AprovarPedidoRequestDTO aprovacaoPor(UUID aprovador) {
        AprovarPedidoRequestDTO dto = new AprovarPedidoRequestDTO();
        dto.setUsuarioAprovadorId(aprovador);
        dto.setItens(List.of());
        return dto;
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class, names = { "TECNICO", "ANALISTA", "PESQUISADOR", "ESTAGIARIO" })
    void solicitanteNaoAprovaRejeitaEntregaNemCancela(Perfil perfil) {
        SessaoDeTeste.entrar(SESSAO, perfil);

        assertThrows(AccessDeniedException.class, () -> pedidoService.aprovar(PEDIDO, aprovacaoPor(SESSAO)));
        assertThrows(AccessDeniedException.class, () -> pedidoService.aprovarSolucao(PEDIDO, aprovacaoPor(SESSAO)));
        assertThrows(AccessDeniedException.class, () -> pedidoService.rejeitar(PEDIDO, "motivo"));
        assertThrows(AccessDeniedException.class, () -> pedidoService.entregar(PEDIDO));
        assertThrows(AccessDeniedException.class, () -> pedidoService.entregarSolucao(PEDIDO));
        assertThrows(AccessDeniedException.class, () -> pedidoService.cancelar(PEDIDO, "motivo"));
        assertThrows(AccessDeniedException.class, () -> pedidoService.cancelarSolucao(PEDIDO, false, "motivo"));

        verifyNoInteractions(pedidoRepository, movimentacaoEstoqueService, historicoLaboratorioRepository);
    }

    @Test
    void gestorNaoAprovaEmNomeDeOutroUsuario() {
        SessaoDeTeste.entrar(SESSAO, Perfil.GESTOR);

        assertThrows(AccessDeniedException.class, () -> pedidoService.aprovar(PEDIDO, aprovacaoPor(OUTRO_USUARIO)));
        assertThrows(AccessDeniedException.class,
                () -> pedidoService.aprovarSolucao(PEDIDO, aprovacaoPor(OUTRO_USUARIO)));

        verifyNoInteractions(pedidoRepository, movimentacaoEstoqueService);
    }
}
