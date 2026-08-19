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
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.request.AprovarPedidoRequestDTO;
import com.sgl.dto.response.PedidoResponseDTO;
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

    private static final UUID UNIDADE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID LABORATORIO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SOLICITANTE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID APROVADOR_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID PRODUTO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID ESTOQUE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final UUID PEDIDO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000007");
    private static final UUID ITEM_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000008");
    private static final UUID PROJETO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");

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
                .publicId(UNIDADE_PUBLIC_ID)
                .nome("Unidade Central")
                .sigla("UC")
                .build();

        laboratorio = Laboratorio.builder()
                .id(2L)
                .publicId(LABORATORIO_PUBLIC_ID)
                .nome("Laboratório de Química")
                .unidade(unidade)
                .ativo(true)
                .build();

        solicitante = new Usuario();
        solicitante.setId(3L);
        solicitante.setPublicId(SOLICITANTE_PUBLIC_ID);
        solicitante.setNome("Solicitante");
        solicitante.setUnidade(unidade);
        solicitante.setLaboratorio(laboratorio);
        solicitante.setAtivo(true);

        aprovador = new Usuario();
        aprovador.setId(4L);
        aprovador.setPublicId(APROVADOR_PUBLIC_ID);
        aprovador.setNome("Aprovador");
        aprovador.setUnidade(unidade);
        aprovador.setAtivo(true);

        produto = Produto.builder()
                .id(5L)
                .publicId(PRODUTO_PUBLIC_ID)
                .nome("Álcool 70%")
                .perecivel(false)
                .ativo(true)
                .build();

        estoque = EstoqueCentral.builder()
                .id(6L)
                .publicId(ESTOQUE_PUBLIC_ID)
                .unidade(unidade)
                .produto(produto)
                .quantidadeAtual(10)
                .quantidadeMinima(2)
                .ativo(true)
                .build();

        pedido = Pedido.builder()
                .id(7L)
                .publicId(PEDIDO_PUBLIC_ID)
                .usuario(solicitante)
                .laboratorio(laboratorio)
                .dataSolicitacao(LocalDateTime.now())
                .status(StatusPedido.PENDENTE)
                .itens(new ArrayList<>())
                .build();

        item = ItemPedido.builder()
                .id(8L)
                .publicId(ITEM_PUBLIC_ID)
                .pedido(pedido)
                .produto(produto)
                .quantidadeSolicitada(5)
                .build();
        pedido.getItens().add(item);
    }

    @Test
    void deveAprovarPedidoDelegandoSaidaAoMovimentacaoEstoqueService() {
        AprovarPedidoRequestDTO dto = criarAprovacaoDTO(3);

        when(usuarioRepository.findByPublicId(APROVADOR_PUBLIC_ID)).thenReturn(Optional.of(aprovador));
        prepararBuscaPedidoComBloqueio();
        when(estoqueCentralRepository.findByUnidadeIdAndProdutoId(1L, 5L))
                .thenReturn(Optional.of(estoque));
        when(pedidoRepository.save(any(Pedido.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        PedidoResponseDTO resultado = pedidoService.aprovar(PEDIDO_PUBLIC_ID, dto);

        assertEquals(StatusPedido.APROVADO, resultado.getStatus());
        assertEquals(PEDIDO_PUBLIC_ID, resultado.getId());
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
        AprovarPedidoRequestDTO dto = criarAprovacaoDTO(3);

        when(usuarioRepository.findByPublicId(APROVADOR_PUBLIC_ID)).thenReturn(Optional.of(aprovador));
        prepararBuscaPedidoComBloqueio();

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> pedidoService.aprovar(PEDIDO_PUBLIC_ID, dto)
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
        AprovarPedidoRequestDTO dto = criarAprovacaoDTO(6);

        when(usuarioRepository.findByPublicId(APROVADOR_PUBLIC_ID)).thenReturn(Optional.of(aprovador));
        prepararBuscaPedidoComBloqueio();

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> pedidoService.aprovar(PEDIDO_PUBLIC_ID, dto)
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
        AprovarPedidoRequestDTO dto = criarAprovacaoDTO(3);

        when(usuarioRepository.findByPublicId(APROVADOR_PUBLIC_ID)).thenReturn(Optional.of(aprovador));
        prepararBuscaPedidoComBloqueio();
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
                () -> pedidoService.aprovar(PEDIDO_PUBLIC_ID, dto)
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
        AprovarPedidoRequestDTO dto = criarAprovacaoDTO(3);
        dto.setUsuarioAprovadorId(null);

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> pedidoService.aprovar(PEDIDO_PUBLIC_ID, dto)
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

        prepararBuscaPedidoComBloqueio();
        when(pedidoRepository.save(any(Pedido.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        PedidoResponseDTO resultado = pedidoService.cancelar(PEDIDO_PUBLIC_ID, "Cancelado pelo gestor");

        verify(movimentacaoEstoqueService).devolverSaidasDoPedido(
                pedido,
                null,
                "Cancelado pelo gestor"
        );
        assertEquals(StatusPedido.CANCELADO, resultado.getStatus());
        assertEquals(PEDIDO_PUBLIC_ID, resultado.getId());
    }

    @Test
    void deveListarPedidosDoProjetoNoLaboratorioEPeriodo() {
        Projeto projeto = Projeto.builder()
                .id(9L)
                .publicId(PROJETO_PUBLIC_ID)
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

        when(laboratorioRepository.findByPublicId(LABORATORIO_PUBLIC_ID)).thenReturn(Optional.of(laboratorio));
        when(projetoRepository.findByPublicId(PROJETO_PUBLIC_ID)).thenReturn(Optional.of(projeto));
        when(pedidoRepository.findByLaboratorioProjetoEPeriodo(2L, 9L, inicio, fim))
                .thenReturn(List.of(pedido));

        List<PedidoResponseDTO> resultado = pedidoService.listarPorProjetoEPeriodo(
                LABORATORIO_PUBLIC_ID,
                PROJETO_PUBLIC_ID,
                dataInicio,
                dataFim
        );

        assertEquals(1, resultado.size());
        assertEquals(PEDIDO_PUBLIC_ID, resultado.get(0).getId());
        assertEquals(PROJETO_PUBLIC_ID, resultado.get(0).getProjetoId());
        verify(pedidoRepository).findByLaboratorioProjetoEPeriodo(2L, 9L, inicio, fim);
    }

    @Test
    void deveImpedirConsultaDePedidosQuandoProjetoNaoPertenceAoLaboratorio() {
        Laboratorio outroLaboratorio = Laboratorio.builder()
                .id(99L)
                .publicId(UUID.fromString("00000000-0000-0000-0000-000000000099"))
                .nome("Outro laboratório")
                .unidade(unidade)
                .ativo(true)
                .build();

        Projeto projeto = Projeto.builder()
                .id(9L)
                .publicId(PROJETO_PUBLIC_ID)
                .laboratorio(outroLaboratorio)
                .nome("Projeto externo")
                .ativo(true)
                .build();

        when(laboratorioRepository.findByPublicId(LABORATORIO_PUBLIC_ID)).thenReturn(Optional.of(laboratorio));
        when(projetoRepository.findByPublicId(PROJETO_PUBLIC_ID)).thenReturn(Optional.of(projeto));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> pedidoService.listarPorProjetoEPeriodo(
                        LABORATORIO_PUBLIC_ID,
                        PROJETO_PUBLIC_ID,
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
                .publicId(PROJETO_PUBLIC_ID)
                .laboratorio(laboratorio)
                .nome("Projeto 1")
                .ativo(true)
                .build();

        when(laboratorioRepository.findByPublicId(LABORATORIO_PUBLIC_ID)).thenReturn(Optional.of(laboratorio));
        when(projetoRepository.findByPublicId(PROJETO_PUBLIC_ID)).thenReturn(Optional.of(projeto));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> pedidoService.listarPorProjetoEPeriodo(
                        LABORATORIO_PUBLIC_ID,
                        PROJETO_PUBLIC_ID,
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

    private void prepararBuscaPedidoComBloqueio() {
        when(pedidoRepository.findByPublicId(PEDIDO_PUBLIC_ID)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.buscarPorIdComBloqueio(7L)).thenReturn(Optional.of(pedido));
    }

    private AprovarPedidoRequestDTO criarAprovacaoDTO(Integer quantidadeAprovada) {
        AprovarPedidoRequestDTO.ItemAprovacaoDTO itemDTO =
                new AprovarPedidoRequestDTO.ItemAprovacaoDTO(ITEM_PUBLIC_ID, quantidadeAprovada);

        AprovarPedidoRequestDTO dto = new AprovarPedidoRequestDTO();
        dto.setUsuarioAprovadorId(APROVADOR_PUBLIC_ID);
        dto.setObservacao("Aprovação parcial");
        dto.setItens(List.of(itemDTO));
        return dto;
    }
}
