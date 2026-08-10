package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.EntradaLoteDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.Lote;
import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.Produto;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.LoteRepository;
import com.sgl.repository.MovimentacaoEstoqueRepository;

@ExtendWith(MockitoExtension.class)
class MovimentacaoEstoqueServiceTest {

    @Mock
    private MovimentacaoEstoqueRepository movimentacaoRepository;

    @Mock
    private EstoqueCentralRepository estoqueCentralRepository;

    @Mock
    private LoteRepository loteRepository;

    @InjectMocks
    private MovimentacaoEstoqueService service;

    private Produto produto;
    private Usuario usuario;
    private EstoqueCentral estoque;

    @BeforeEach
    void prepararCenario() {
        produto = Produto.builder()
                .id(1L)
                .nome("Produto Teste")
                .perecivel(false)
                .ativo(true)
                .build();

        Unidade unidade = Unidade.builder()
                .id(10L)
                .nome("Unidade Teste")
                .sigla("UT")
                .build();

        usuario = new Usuario();
        usuario.setId(2L);
        usuario.setNome("Responsável");
        usuario.setAtivo(true);

        estoque = EstoqueCentral.builder()
                .id(3L)
                .unidade(unidade)
                .produto(produto)
                .quantidadeAtual(10)
                .quantidadeMinima(2)
                .ativo(true)
                .build();

        lenient().when(movimentacaoRepository.save(any(MovimentacaoEstoque.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Test
    void deveRegistrarEntradaCriandoLoteEAtualizandoSaldo() {
        EntradaLoteDTO dto = new EntradaLoteDTO(
                "LT-001",
                5,
                null,
                OrigemMovimentacao.COMPRA,
                "Entrada de teste"
        );

        when(estoqueCentralRepository.buscarPorIdComBloqueio(3L))
                .thenReturn(Optional.of(estoque));
        when(loteRepository.existsByEstoqueCentralIdAndNumeroLote(3L, "LT-001"))
                .thenReturn(false);

        service.registrarEntradaLote(3L, dto, usuario);

        assertEquals(15, estoque.getQuantidadeAtual());

        ArgumentCaptor<Lote> loteCaptor = ArgumentCaptor.forClass(Lote.class);
        verify(loteRepository).save(loteCaptor.capture());

        Lote lote = loteCaptor.getValue();
        assertEquals("LT-001", lote.getNumeroLote());
        assertEquals(5, lote.getQuantidadeInicial());
        assertEquals(5, lote.getQuantidadeDisponivel());
        assertEquals(null, lote.getDataValidade());

        ArgumentCaptor<MovimentacaoEstoque> movCaptor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoRepository).save(movCaptor.capture());

        MovimentacaoEstoque mov = movCaptor.getValue();
        assertEquals(TipoMovimentacao.ENTRADA, mov.getTipoMovimentacao());
        assertEquals(lote, mov.getLote());
        assertEquals(10, mov.getQuantidadeAnterior());
        assertEquals(15, mov.getQuantidadeAtual());
    }

    @Test
    void deveExigirValidadeParaProdutoPerecivel() {
        produto.setPerecivel(true);

        EntradaLoteDTO dto = new EntradaLoteDTO(
                "LT-PER",
                5,
                null,
                OrigemMovimentacao.COMPRA,
                null
        );

        when(estoqueCentralRepository.buscarPorIdComBloqueio(3L))
                .thenReturn(Optional.of(estoque));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.registrarEntradaLote(3L, dto, usuario)
        );

        assertEquals(
                "Data de validade é obrigatória para produto perecível.",
                exception.getMessage()
        );

        verify(loteRepository, never()).save(any());
        verify(estoqueCentralRepository, never()).save(any());
        verify(movimentacaoRepository, never()).save(any());
    }

    @Test
    void deveUsarFifoParaProdutoNaoPerecivel() {
        Lote primeiro = criarLote(10L, "FIFO-1", 4, null, LocalDate.now().minusDays(10));
        Lote segundo = criarLote(11L, "FIFO-2", 6, null, LocalDate.now().minusDays(5));

        when(estoqueCentralRepository.buscarPorIdComBloqueio(3L))
                .thenReturn(Optional.of(estoque));
        when(loteRepository.buscarDisponiveisPorEntradaComBloqueio(3L))
                .thenReturn(List.of(primeiro, segundo));

        service.registrarSaida(
                3L,
                5,
                usuario,
                OrigemMovimentacao.AJUSTE,
                null,
                null,
                "Saída FIFO"
        );

        assertEquals(0, primeiro.getQuantidadeDisponivel());
        assertEquals(5, segundo.getQuantidadeDisponivel());
        assertEquals(5, estoque.getQuantidadeAtual());
        verify(loteRepository, never()).buscarDisponiveisPorFefoComBloqueio(any(), any());
    }

    @Test
    void deveUsarFefoEConsumirMaisDeUmLote() {
        produto.setPerecivel(true);

        Lote vencePrimeiro = criarLote(
                20L,
                "FEFO-1",
                3,
                LocalDate.now().plusDays(5),
                LocalDate.now().minusDays(2)
        );
        Lote venceDepois = criarLote(
                21L,
                "FEFO-2",
                7,
                LocalDate.now().plusDays(30),
                LocalDate.now().minusDays(1)
        );

        when(estoqueCentralRepository.buscarPorIdComBloqueio(3L))
                .thenReturn(Optional.of(estoque));
        when(loteRepository.buscarDisponiveisPorFefoComBloqueio(any(), any(LocalDate.class)))
                .thenReturn(List.of(vencePrimeiro, venceDepois));

        service.registrarSaida(
                3L,
                5,
                usuario,
                OrigemMovimentacao.PEDIDO,
                null,
                null,
                "Saída FEFO"
        );

        assertEquals(0, vencePrimeiro.getQuantidadeDisponivel());
        assertEquals(5, venceDepois.getQuantidadeDisponivel());
        assertEquals(5, estoque.getQuantidadeAtual());

        ArgumentCaptor<MovimentacaoEstoque> captor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoRepository, org.mockito.Mockito.times(2)).save(captor.capture());

        assertEquals(20L, captor.getAllValues().get(0).getLote().getId());
        assertEquals(3, captor.getAllValues().get(0).getQuantidadeMovimentada());
        assertEquals(21L, captor.getAllValues().get(1).getLote().getId());
        assertEquals(2, captor.getAllValues().get(1).getQuantidadeMovimentada());
    }

    @Test
    void deveImpedirSaidaQuandoLotesValidosForemInsuficientes() {
        produto.setPerecivel(true);

        Lote valido = criarLote(
                30L,
                "VALIDO",
                2,
                LocalDate.now().plusDays(10),
                LocalDate.now()
        );

        when(estoqueCentralRepository.buscarPorIdComBloqueio(3L))
                .thenReturn(Optional.of(estoque));
        when(loteRepository.buscarDisponiveisPorFefoComBloqueio(any(), any(LocalDate.class)))
                .thenReturn(List.of(valido));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.registrarSaida(
                        3L,
                        3,
                        usuario,
                        OrigemMovimentacao.PEDIDO,
                        null,
                        null,
                        null
                )
        );

        assertEquals(
                "Estoque utilizável insuficiente. Disponível nos lotes válidos: 2, solicitado: 3",
                exception.getMessage()
        );
        assertEquals(10, estoque.getQuantidadeAtual());
        verify(movimentacaoRepository, never()).save(any());
    }

    @Test
    void deveDescartarSomenteSaldoDeLotesVencidos() {
        produto.setPerecivel(true);

        Lote vencido1 = criarLote(
                40L,
                "VENC-1",
                2,
                LocalDate.now().minusDays(20),
                LocalDate.now().minusMonths(2)
        );
        Lote vencido2 = criarLote(
                41L,
                "VENC-2",
                4,
                LocalDate.now().minusDays(5),
                LocalDate.now().minusMonths(1)
        );

        when(estoqueCentralRepository.buscarPorIdComBloqueio(3L))
                .thenReturn(Optional.of(estoque));
        when(loteRepository.buscarVencidosComBloqueio(any(), any(LocalDate.class)))
                .thenReturn(List.of(vencido1, vencido2));

        service.registrarDescarteVencimento(3L, 5, "Vencidos", usuario);

        assertEquals(0, vencido1.getQuantidadeDisponivel());
        assertEquals(1, vencido2.getQuantidadeDisponivel());
        assertEquals(5, estoque.getQuantidadeAtual());
    }

    @Test
    void deveRestaurarOsMesmosLotesConsumidosNoCancelamento() {
        Lote loteA = criarLote(50L, "RET-A", 0, null, LocalDate.now().minusDays(10));
        loteA.setQuantidadeInicial(5);
        Lote loteB = criarLote(51L, "RET-B", 2, null, LocalDate.now().minusDays(5));
        loteB.setQuantidadeInicial(5);

        estoque.setQuantidadeAtual(2);

        com.sgl.model.Pedido pedido = com.sgl.model.Pedido.builder()
                .id(60L)
                .build();

        MovimentacaoEstoque saidaA = MovimentacaoEstoque.builder()
                .id(70L)
                .estoqueCentral(estoque)
                .lote(loteA)
                .pedido(pedido)
                .quantidadeMovimentada(5)
                .tipoMovimentacao(TipoMovimentacao.SAIDA)
                .build();

        MovimentacaoEstoque saidaB = MovimentacaoEstoque.builder()
                .id(71L)
                .estoqueCentral(estoque)
                .lote(loteB)
                .pedido(pedido)
                .quantidadeMovimentada(3)
                .tipoMovimentacao(TipoMovimentacao.SAIDA)
                .build();

        when(movimentacaoRepository.findByPedidoIdAndTipoMovimentacaoOrderByIdAsc(
                60L,
                TipoMovimentacao.SAIDA
        )).thenReturn(List.of(saidaA, saidaB));
        when(estoqueCentralRepository.buscarPorIdComBloqueio(3L))
                .thenReturn(Optional.of(estoque));
        when(loteRepository.buscarPorIdComBloqueio(50L))
                .thenReturn(Optional.of(loteA));
        when(loteRepository.buscarPorIdComBloqueio(51L))
                .thenReturn(Optional.of(loteB));

        service.devolverSaidasDoPedido(pedido, null, "Cancelamento");

        assertEquals(5, loteA.getQuantidadeDisponivel());
        assertEquals(5, loteB.getQuantidadeDisponivel());
        assertEquals(10, estoque.getQuantidadeAtual());
    }

    private Lote criarLote(
            Long id,
            String numero,
            int quantidade,
            LocalDate validade,
            LocalDate entrada) {

        Lote lote = new Lote();
        lote.setId(id);
        lote.setEstoqueCentral(estoque);
        lote.setNumeroLote(numero);
        lote.setQuantidadeInicial(quantidade);
        lote.setQuantidadeDisponivel(quantidade);
        lote.setDataValidade(validade);
        lote.setDataEntrada(entrada);
        lote.setAtivo(true);
        return lote;
    }
}
