package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.AprovarPedidoDTO;
import com.sgl.dto.PedidoDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.ItemPedido;
import com.sgl.model.Laboratorio;
import com.sgl.model.Pedido;
import com.sgl.model.Produto;
import com.sgl.model.Projeto;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.StatusPedido;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.HistoricoLaboratorioRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.PedidoRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private EstoqueCentralRepository estoqueCentralRepository;

    @Mock
    private HistoricoLaboratorioRepository historicoLaboratorioRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private LaboratorioRepository laboratorioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private MovimentacaoEstoqueService movimentacaoEstoqueService;

    @InjectMocks
    private PedidoService pedidoService;

    private Unidade unidade;
    private Laboratorio laboratorio;
    private Usuario solicitante;
    private Usuario aprovador;
    private Produto produto;
    private EstoqueCentral estoque;
    private ItemPedido item;
    private Pedido pedido;

    @BeforeEach
    void prepararCenario() {
        unidade = Unidade.builder()
                .id(1L)
                .nome("Unidade Central")
                .sigla("UC")
                .build();

        laboratorio = Laboratorio.builder()
                .id(2L)
                .nome("Laboratório de Química")
                .unidade(unidade)
                .ativo(true)
                .build();

        solicitante = new Usuario();
        solicitante.setId(3L);
        solicitante.setNome("Solicitante");
        solicitante.setUnidade(unidade);
        solicitante.setLaboratorio(laboratorio);
        solicitante.setAtivo(true);

        aprovador = new Usuario();
        aprovador.setId(4L);
        aprovador.setNome("Aprovador");
        aprovador.setUnidade(unidade);
        aprovador.setAtivo(true);

        produto = Produto.builder()
                .id(5L)
                .nome("Álcool 70%")
                .perecivel(false)
                .ativo(true)
                .build();

        estoque = EstoqueCentral.builder()
                .id(6L)
                .unidade(unidade)
                .produto(produto)
                .quantidadeAtual(10)
                .quantidadeMinima(2)
                .ativo(true)
                .build();

        pedido = Pedido.builder()
                .id(7L)
                .usuario(solicitante)
                .laboratorio(laboratorio)
                .dataSolicitacao(LocalDateTime.now())
                .status(StatusPedido.PENDENTE)
                .itens(new ArrayList<>())
                .build();

        item = ItemPedido.builder()
                .id(8L)
                .pedido(pedido)
                .produto(produto)
                .quantidadeSolicitada(5)
                .build();
        pedido.getItens().add(item);
    }

    @Test
    void deveAprovarPedidoDelegandoSaidaAoMovimentacaoEstoqueService() {
        AprovarPedidoDTO dto = criarAprovacaoDTO(3);

        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(aprovador));
        when(pedidoRepository.buscarPorIdComBloqueio(7L)).thenReturn(Optional.of(pedido));
        when(estoqueCentralRepository.findByUnidadeIdAndProdutoId(1L, 5L))
                .thenReturn(Optional.of(estoque));
        when(pedidoRepository.save(any(Pedido.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        PedidoDTO resultado = pedidoService.aprovar(7L, dto);

        assertEquals(StatusPedido.APROVADO, resultado.getStatus());
        assertEquals(3, item.getQuantidadeAprovada());
        assertEquals("Aprovação parcial", pedido.getObservacao());

        verify(movimentacaoEstoqueService).registrarSaida(
                6L,
                3,
                aprovador,
                OrigemMovimentacao.PEDIDO,
                pedido,
                laboratorio,
                "Aprovação parcial"
        );
        verify(pedidoRepository).save(pedido);
    }

    @Test
    void deveImpedirAprovacaoQuandoPedidoNaoEstiverPendente() {
        pedido.setStatus(StatusPedido.APROVADO);
        AprovarPedidoDTO dto = criarAprovacaoDTO(3);

        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(aprovador));
        when(pedidoRepository.buscarPorIdComBloqueio(7L)).thenReturn(Optional.of(pedido));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> pedidoService.aprovar(7L, dto)
        );

        assertEquals(
                "Apenas pedidos PENDENTES podem ser aprovados. Status atual: APROVADO",
                exception.getMessage()
        );
        verifyNoInteractions(movimentacaoEstoqueService, estoqueCentralRepository);
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void deveImpedirQuantidadeAprovadaMaiorQueSolicitada() {
        AprovarPedidoDTO dto = criarAprovacaoDTO(6);

        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(aprovador));
        when(pedidoRepository.buscarPorIdComBloqueio(7L)).thenReturn(Optional.of(pedido));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> pedidoService.aprovar(7L, dto)
        );

        assertEquals(
                "Quantidade aprovada deve ser maior que zero e não pode ser maior que a solicitada. Solicitada: 5, aprovada: 6",
                exception.getMessage()
        );
        assertEquals(null, item.getQuantidadeAprovada());
        verifyNoInteractions(movimentacaoEstoqueService, estoqueCentralRepository);
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void devePropagarFalhaQuandoNaoHaLotesValidosSuficientes() {
        AprovarPedidoDTO dto = criarAprovacaoDTO(3);

        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(aprovador));
        when(pedidoRepository.buscarPorIdComBloqueio(7L)).thenReturn(Optional.of(pedido));
        when(estoqueCentralRepository.findByUnidadeIdAndProdutoId(1L, 5L))
                .thenReturn(Optional.of(estoque));

        when(movimentacaoEstoqueService.registrarSaida(
                6L,
                3,
                aprovador,
                OrigemMovimentacao.PEDIDO,
                pedido,
                laboratorio,
                "Aprovação parcial"
        )).thenThrow(new BusinessRuleException(
                "Estoque utilizável insuficiente. Disponível nos lotes válidos: 2, solicitado: 3"
        ));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> pedidoService.aprovar(7L, dto)
        );

        assertEquals(
                "Estoque utilizável insuficiente. Disponível nos lotes válidos: 2, solicitado: 3",
                exception.getMessage()
        );
        assertEquals(null, item.getQuantidadeAprovada());
        assertEquals(StatusPedido.PENDENTE, pedido.getStatus());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void deveExigirUsuarioAprovador() {
        AprovarPedidoDTO dto = criarAprovacaoDTO(3);
        dto.setUsuarioAprovadorId(null);

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> pedidoService.aprovar(7L, dto)
        );

        assertEquals("O usuário aprovador é obrigatório.", exception.getMessage());
        verifyNoInteractions(
                usuarioRepository,
                pedidoRepository,
                estoqueCentralRepository,
                movimentacaoEstoqueService
        );
    }

    @Test
    void deveRestaurarLotesAoCancelarPedidoAprovado() {
        pedido.setStatus(StatusPedido.APROVADO);

        when(pedidoRepository.buscarPorIdComBloqueio(7L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        PedidoDTO resultado = pedidoService.cancelar(7L, "Cancelado pelo gestor");

        verify(movimentacaoEstoqueService).devolverSaidasDoPedido(
                pedido,
                null,
                "Cancelado pelo gestor"
        );
        assertEquals(StatusPedido.CANCELADO, resultado.getStatus());
    }

    @Test
    void deveListarPedidosDoProjetoNoLaboratorioEPeriodo() {
        Projeto projeto = Projeto.builder()
                .id(9L)
                .laboratorio(laboratorio)
                .nome("Projeto 1")
                .ativo(true)
                .build();
        pedido.setProjeto(projeto);
        pedido.setDataSolicitacao(LocalDateTime.of(2026, 6, 15, 14, 30));

        LocalDate dataInicio = LocalDate.of(2026, 6, 1);
        LocalDate dataFim = LocalDate.of(2026, 6, 30);
        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim.atTime(LocalTime.MAX);

        when(laboratorioRepository.existsById(2L)).thenReturn(true);
        when(projetoRepository.findById(9L)).thenReturn(Optional.of(projeto));
        when(pedidoRepository.findByLaboratorioProjetoEPeriodo(2L, 9L, inicio, fim))
                .thenReturn(List.of(pedido));

        List<PedidoDTO> resultado = pedidoService.listarPorProjetoEPeriodo(
                2L,
                9L,
                dataInicio,
                dataFim
        );

        assertEquals(1, resultado.size());
        assertEquals(7L, resultado.get(0).getId());
        assertEquals(9L, resultado.get(0).getProjetoId());
        verify(pedidoRepository).findByLaboratorioProjetoEPeriodo(2L, 9L, inicio, fim);
    }

    @Test
    void deveImpedirConsultaDePedidosQuandoProjetoNaoPertenceAoLaboratorio() {
        Laboratorio outroLaboratorio = Laboratorio.builder()
                .id(99L)
                .nome("Outro laboratório")
                .unidade(unidade)
                .ativo(true)
                .build();

        Projeto projeto = Projeto.builder()
                .id(9L)
                .laboratorio(outroLaboratorio)
                .nome("Projeto externo")
                .ativo(true)
                .build();

        when(laboratorioRepository.existsById(2L)).thenReturn(true);
        when(projetoRepository.findById(9L)).thenReturn(Optional.of(projeto));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> pedidoService.listarPorProjetoEPeriodo(
                        2L,
                        9L,
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 30)
                )
        );

        assertEquals(
                "O projeto informado não pertence ao laboratório informado.",
                exception.getMessage()
        );
        verify(pedidoRepository, never())
                .findByLaboratorioProjetoEPeriodo(any(), any(), any(), any());
    }

    @Test
    void deveImpedirConsultaDePedidosComPeriodoInvertido() {
        Projeto projeto = Projeto.builder()
                .id(9L)
                .laboratorio(laboratorio)
                .nome("Projeto 1")
                .ativo(true)
                .build();

        when(laboratorioRepository.existsById(2L)).thenReturn(true);
        when(projetoRepository.findById(9L)).thenReturn(Optional.of(projeto));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> pedidoService.listarPorProjetoEPeriodo(
                        2L,
                        9L,
                        LocalDate.of(2026, 6, 30),
                        LocalDate.of(2026, 6, 1)
                )
        );

        assertEquals(
                "A data inicial não pode ser posterior à data final.",
                exception.getMessage()
        );
        verify(pedidoRepository, never())
                .findByLaboratorioProjetoEPeriodo(any(), any(), any(), any());
    }

    private AprovarPedidoDTO criarAprovacaoDTO(Integer quantidadeAprovada) {
        AprovarPedidoDTO.ItemAprovacaoDTO itemDTO =
                new AprovarPedidoDTO.ItemAprovacaoDTO(8L, quantidadeAprovada);

        AprovarPedidoDTO dto = new AprovarPedidoDTO();
        dto.setUsuarioAprovadorId(4L);
        dto.setObservacao("Aprovação parcial");
        dto.setItens(List.of(itemDTO));
        return dto;
    }
}
