package com.sgl.service;

import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.request.EntradaLoteRequestDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.Lote;
import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.MovimentacaoRecipiente;
import com.sgl.model.Produto;
import com.sgl.model.RecipienteEstoque;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.EstadoRecipienteEstoque;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.TipoEmbalagem;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.LoteRepository;
import com.sgl.repository.MovimentacaoEstoqueRepository;
import com.sgl.repository.MovimentacaoRecipienteRepository;
import com.sgl.repository.PedidoRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.RecipienteEstoqueRepository;
import com.sgl.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class MovimentacaoEstoqueServiceTest {

    private static final UUID ESTOQUE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Mock
    private MovimentacaoEstoqueRepository movimentacaoRepository;

    @Mock
    private EstoqueCentralRepository estoqueCentralRepository;

    @Mock
    private LoteRepository loteRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private LaboratorioRepository laboratorioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private RecipienteEstoqueRepository recipienteEstoqueRepository;

    @Mock
    private MovimentacaoRecipienteRepository movimentacaoRecipienteRepository;

    @InjectMocks
    private MovimentacaoEstoqueService service;

    private Produto produto;
    private Usuario usuario;
    private EstoqueCentral estoque;

    @BeforeEach
    void prepararCenario() {
        produto = Produto.builder()
                .id(1L)
                .publicId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .nome("Produto Teste")
                .codigoReferencia("PROD-TESTE")
                .unidadeMedida(UnidadeMedida.UNIDADE)
                .perecivel(false)
                .ativo(true)
                .build();

        Unidade unidade = Unidade.builder()
                .id(10L)
                .publicId(UUID.fromString("00000000-0000-0000-0000-000000000010"))
                .nome("Unidade Teste")
                .sigla("UT")
                .build();

        usuario = new Usuario();
        usuario.setId(2L);
        usuario.setPublicId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        usuario.setNome("Responsável");
        usuario.setAtivo(true);

        estoque = EstoqueCentral.builder()
                .id(3L)
                .publicId(ESTOQUE_PUBLIC_ID)
                .unidade(unidade)
                .produto(produto)
                .quantidadeAtual(BigDecimal.valueOf(10))
                .quantidadeMinima(BigDecimal.valueOf(2))
                .ativo(true)
                .build();

        lenient().when(movimentacaoRepository.save(any(MovimentacaoEstoque.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        lenient().when(produtoRepository.buscarPorIdComBloqueio(1L))
                .thenReturn(Optional.of(produto));
        lenient().when(loteRepository.buscarMaiorSequencialInternoPorProduto(1L))
                .thenReturn(0);
        lenient().when(loteRepository.existsByCodigoInterno(any()))
                .thenReturn(false);
    }

    @Test
    void deveRegistrarEntradaCriandoLoteEAtualizandoSaldo() {
        EntradaLoteRequestDTO dto = new EntradaLoteRequestDTO(
                "LT-001",
                5,
                null,
                OrigemMovimentacao.COMPRA,
                "Entrada de teste"
        );

        when(estoqueCentralRepository.findByPublicId(ESTOQUE_PUBLIC_ID))
                .thenReturn(Optional.of(estoque));
        when(estoqueCentralRepository.buscarPorIdComBloqueio(3L))
                .thenReturn(Optional.of(estoque));
        when(loteRepository.existsByEstoqueCentralIdAndNumeroLote(3L, "LT-001"))
                .thenReturn(false);

        service.registrarEntradaLote(ESTOQUE_PUBLIC_ID, dto, usuario);

        assertEquals(BigDecimal.valueOf(15), estoque.getQuantidadeAtual());

        ArgumentCaptor<Lote> loteCaptor = ArgumentCaptor.forClass(Lote.class);
        verify(loteRepository).save(loteCaptor.capture());

        Lote lote = loteCaptor.getValue();
        assertEquals("LOT-PROD-TESTE-001", lote.getCodigoInterno());
        assertEquals(1, lote.getSequencialInterno());
        assertEquals("LT-001", lote.getNumeroLote());
        assertEquals(BigDecimal.valueOf(5), lote.getQuantidadeInicial());
        assertEquals(BigDecimal.valueOf(5), lote.getQuantidadeDisponivel());
        assertEquals(null, lote.getDataValidade());

        ArgumentCaptor<MovimentacaoEstoque> movCaptor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoRepository).save(movCaptor.capture());

        MovimentacaoEstoque mov = movCaptor.getValue();
        assertEquals(TipoMovimentacao.ENTRADA, mov.getTipoMovimentacao());
        assertEquals(lote, mov.getLote());
        assertEquals(BigDecimal.valueOf(10), mov.getQuantidadeAnterior());
        assertEquals(BigDecimal.valueOf(15), mov.getQuantidadeAtual());
    }

    @Test
    void deveMaterializarRecipientesFisicosAoRegistrarEntrada() {
        estoque.setQuantidadeAtual(BigDecimal.ZERO);
        produto.setUnidadeMedida(UnidadeMedida.ML);

        EntradaLoteRequestDTO dto = new EntradaLoteRequestDTO();
        dto.setNumeroLote("FAB-ETANOL-001");
        dto.setTipoEmbalagem(TipoEmbalagem.FRASCO);
        dto.setApresentacao("frasco de 500 mL");
        dto.setQuantidade(3);
        dto.setConteudoPorApresentacao(BigDecimal.valueOf(500));
        dto.setFracionavel(true);
        dto.setOrigem(OrigemMovimentacao.COMPRA);

        when(estoqueCentralRepository.findByPublicId(ESTOQUE_PUBLIC_ID))
                .thenReturn(Optional.of(estoque));
        when(estoqueCentralRepository.buscarPorIdComBloqueio(3L))
                .thenReturn(Optional.of(estoque));
        when(loteRepository.existsByEstoqueCentralIdAndNumeroLote(3L, "FAB-ETANOL-001"))
                .thenReturn(false);

        service.registrarEntradaLote(ESTOQUE_PUBLIC_ID, dto, usuario);

        @SuppressWarnings({ "rawtypes", "unchecked" })
        ArgumentCaptor<Iterable<RecipienteEstoque>> recipienteCaptor =
                ArgumentCaptor.forClass((Class) Iterable.class);

        verify(recipienteEstoqueRepository).saveAll(recipienteCaptor.capture());

        List<RecipienteEstoque> recipientes = StreamSupport
                .stream(recipienteCaptor.getValue().spliterator(), false)
                .toList();

        assertEquals(3, recipientes.size());

        assertEquals("LOT-PROD-TESTE-001-R001", recipientes.get(0).getCodigoInterno());
        assertEquals("LOT-PROD-TESTE-001-R002", recipientes.get(1).getCodigoInterno());
        assertEquals("LOT-PROD-TESTE-001-R003", recipientes.get(2).getCodigoInterno());

        for (int i = 0; i < recipientes.size(); i++) {
            RecipienteEstoque recipiente = recipientes.get(i);

            assertEquals(i + 1, recipiente.getSequencial());
            assertEquals(TipoEmbalagem.FRASCO, recipiente.getTipoEmbalagem());
            assertEquals(0, BigDecimal.valueOf(500).compareTo(recipiente.getCapacidadeInicial()));
            assertEquals(0, BigDecimal.valueOf(500).compareTo(recipiente.getQuantidadeDisponivel()));
            assertEquals(UnidadeMedida.ML, recipiente.getUnidadeMedida());
            assertEquals(EstadoRecipienteEstoque.FECHADO, recipiente.getEstado());
        }

        assertEquals(0, BigDecimal.valueOf(1500).compareTo(estoque.getQuantidadeAtual()));

        ArgumentCaptor<Lote> loteCaptor = ArgumentCaptor.forClass(Lote.class);
        verify(loteRepository).save(loteCaptor.capture());

        Lote lote = loteCaptor.getValue();
        assertEquals(3, lote.getQuantidadeApresentacoes());
        assertEquals(0, BigDecimal.valueOf(500).compareTo(lote.getConteudoPorApresentacao()));
        assertEquals(0, BigDecimal.valueOf(1500).compareTo(lote.getQuantidadeInicial()));
        assertEquals(0, BigDecimal.valueOf(1500).compareTo(lote.getQuantidadeDisponivel()));

        ArgumentCaptor<MovimentacaoEstoque> movimentacaoCaptor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoRepository).save(movimentacaoCaptor.capture());

        MovimentacaoEstoque movimentacao = movimentacaoCaptor.getValue();
        assertEquals(TipoMovimentacao.ENTRADA, movimentacao.getTipoMovimentacao());
        assertEquals(0, BigDecimal.valueOf(1500).compareTo(movimentacao.getQuantidadeMovimentada()));
        assertEquals(0, BigDecimal.ZERO.compareTo(movimentacao.getQuantidadeAnterior()));
        assertEquals(0, BigDecimal.valueOf(1500).compareTo(movimentacao.getQuantidadeAtual()));
    }

    @Test
    void deveExigirValidadeParaProdutoPerecivel() {
        produto.setPerecivel(true);

        EntradaLoteRequestDTO dto = new EntradaLoteRequestDTO(
                "LT-PER",
                5,
                null,
                OrigemMovimentacao.COMPRA,
                null
        );

        when(estoqueCentralRepository.findByPublicId(ESTOQUE_PUBLIC_ID))
                .thenReturn(Optional.of(estoque));
        when(estoqueCentralRepository.buscarPorIdComBloqueio(3L))
                .thenReturn(Optional.of(estoque));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.registrarEntradaLote(ESTOQUE_PUBLIC_ID, dto, usuario)
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
        when(recipienteEstoqueRepository.findByLoteIdOrderBySequencialAsc(10L))
                .thenReturn(List.of(criarRecipiente(primeiro, 1, 4, 4, EstadoRecipienteEstoque.FECHADO)));
        when(recipienteEstoqueRepository.findByLoteIdOrderBySequencialAsc(11L))
                .thenReturn(List.of(criarRecipiente(segundo, 1, 6, 6, EstadoRecipienteEstoque.FECHADO)));

        service.registrarSaida(
                3L,
                BigDecimal.valueOf(5),
                usuario,
                OrigemMovimentacao.AJUSTE,
                null,
                null,
                "Saída FIFO"
        );

        assertEquals(BigDecimal.valueOf(0), primeiro.getQuantidadeDisponivel());
        assertEquals(BigDecimal.valueOf(5), segundo.getQuantidadeDisponivel());
        assertEquals(BigDecimal.valueOf(5), estoque.getQuantidadeAtual());
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
        when(recipienteEstoqueRepository.findByLoteIdOrderBySequencialAsc(20L))
                .thenReturn(List.of(criarRecipiente(vencePrimeiro, 1, 3, 3, EstadoRecipienteEstoque.FECHADO)));
        when(recipienteEstoqueRepository.findByLoteIdOrderBySequencialAsc(21L))
                .thenReturn(List.of(criarRecipiente(venceDepois, 1, 7, 7, EstadoRecipienteEstoque.FECHADO)));

        service.registrarSaida(
                3L,
                BigDecimal.valueOf(5),
                usuario,
                OrigemMovimentacao.PEDIDO,
                null,
                null,
                "Saída FEFO"
        );

        assertEquals(BigDecimal.valueOf(0), vencePrimeiro.getQuantidadeDisponivel());
        assertEquals(BigDecimal.valueOf(5), venceDepois.getQuantidadeDisponivel());
        assertEquals(BigDecimal.valueOf(5), estoque.getQuantidadeAtual());

        ArgumentCaptor<MovimentacaoEstoque> captor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoRepository, org.mockito.Mockito.times(2)).save(captor.capture());

        assertEquals(20L, captor.getAllValues().get(0).getLote().getId());
        assertEquals(BigDecimal.valueOf(3), captor.getAllValues().get(0).getQuantidadeMovimentada());
        assertEquals(21L, captor.getAllValues().get(1).getLote().getId());
        assertEquals(BigDecimal.valueOf(2), captor.getAllValues().get(1).getQuantidadeMovimentada());
    }

    @Test
    void devePriorizarRecipienteFechadoParaRetiradaInteira() {
        produto.setUnidadeMedida(UnidadeMedida.ML);
        estoque.setQuantidadeAtual(BigDecimal.valueOf(1300));

        Lote lote = criarLote(60L, "FISICO-500", 1300, null, LocalDate.now().minusDays(1));
        lote.setTipoEmbalagem(TipoEmbalagem.FRASCO);
        lote.setConteudoPorApresentacao(BigDecimal.valueOf(500));
        lote.setFracionavel(true);

        RecipienteEstoque aberto =
                criarRecipiente(lote, 1, 500, 300, EstadoRecipienteEstoque.ABERTO);
        RecipienteEstoque fechado1 =
                criarRecipiente(lote, 2, 500, 500, EstadoRecipienteEstoque.FECHADO);
        RecipienteEstoque fechado2 =
                criarRecipiente(lote, 3, 500, 500, EstadoRecipienteEstoque.FECHADO);

        when(estoqueCentralRepository.buscarPorIdComBloqueio(3L))
                .thenReturn(Optional.of(estoque));
        when(loteRepository.buscarDisponiveisPorEntradaComBloqueio(3L))
                .thenReturn(List.of(lote));
        when(recipienteEstoqueRepository.findByLoteIdOrderBySequencialAsc(60L))
                .thenReturn(List.of(aberto, fechado1, fechado2));

        service.registrarSaida(
                3L,
                BigDecimal.valueOf(500),
                usuario,
                OrigemMovimentacao.PEDIDO,
                null,
                null,
                "Retirada inteira"
        );

        assertEquals(0, BigDecimal.valueOf(300).compareTo(aberto.getQuantidadeDisponivel()));
        assertEquals(EstadoRecipienteEstoque.ABERTO, aberto.getEstado());

        assertEquals(0, BigDecimal.ZERO.compareTo(fechado1.getQuantidadeDisponivel()));
        assertEquals(EstadoRecipienteEstoque.ESGOTADO, fechado1.getEstado());
        assertEquals(EstadoRecipienteEstoque.FECHADO, fechado2.getEstado());

        assertEquals(0, BigDecimal.valueOf(800).compareTo(lote.getQuantidadeDisponivel()));
        assertEquals(0, BigDecimal.valueOf(800).compareTo(estoque.getQuantidadeAtual()));

        List<MovimentacaoRecipiente> detalhes = capturarDetalhesDeRecipiente();
        assertEquals(1, detalhes.size());
        assertEquals(fechado1, detalhes.get(0).getRecipienteEstoque());
        assertEquals(EstadoRecipienteEstoque.FECHADO, detalhes.get(0).getEstadoAnterior());
        assertEquals(EstadoRecipienteEstoque.ESGOTADO, detalhes.get(0).getEstadoAtual());
        assertEquals(false, detalhes.get(0).getAbriuRecipiente());
        assertEquals(true, detalhes.get(0).getEsgotouRecipiente());
    }

    @Test
    void devePriorizarRecipienteAbertoParaRetiradaFracionaria() {
        produto.setUnidadeMedida(UnidadeMedida.ML);
        estoque.setQuantidadeAtual(BigDecimal.valueOf(800));

        Lote lote = criarLote(61L, "FISICO-200", 800, null, LocalDate.now().minusDays(1));
        lote.setTipoEmbalagem(TipoEmbalagem.FRASCO);
        lote.setConteudoPorApresentacao(BigDecimal.valueOf(500));
        lote.setFracionavel(true);

        RecipienteEstoque aberto =
                criarRecipiente(lote, 1, 500, 300, EstadoRecipienteEstoque.ABERTO);
        RecipienteEstoque fechado =
                criarRecipiente(lote, 2, 500, 500, EstadoRecipienteEstoque.FECHADO);

        when(estoqueCentralRepository.buscarPorIdComBloqueio(3L))
                .thenReturn(Optional.of(estoque));
        when(loteRepository.buscarDisponiveisPorEntradaComBloqueio(3L))
                .thenReturn(List.of(lote));
        when(recipienteEstoqueRepository.findByLoteIdOrderBySequencialAsc(61L))
                .thenReturn(List.of(aberto, fechado));

        service.registrarSaida(
                3L,
                BigDecimal.valueOf(200),
                usuario,
                OrigemMovimentacao.PEDIDO,
                null,
                null,
                "Retirada fracionária"
        );

        assertEquals(0, BigDecimal.valueOf(100).compareTo(aberto.getQuantidadeDisponivel()));
        assertEquals(EstadoRecipienteEstoque.ABERTO, aberto.getEstado());
        assertEquals(0, BigDecimal.valueOf(500).compareTo(fechado.getQuantidadeDisponivel()));
        assertEquals(EstadoRecipienteEstoque.FECHADO, fechado.getEstado());

        assertEquals(0, BigDecimal.valueOf(600).compareTo(lote.getQuantidadeDisponivel()));
        assertEquals(0, BigDecimal.valueOf(600).compareTo(estoque.getQuantidadeAtual()));

        List<MovimentacaoRecipiente> detalhes = capturarDetalhesDeRecipiente();
        assertEquals(1, detalhes.size());
        assertEquals(aberto, detalhes.get(0).getRecipienteEstoque());
        assertEquals(false, detalhes.get(0).getAbriuRecipiente());
        assertEquals(false, detalhes.get(0).getEsgotouRecipiente());
    }

    @Test
    void deveCombinarRecipienteFechadoEAbertoNaRetiradaDeSetecentos() {
        produto.setUnidadeMedida(UnidadeMedida.ML);
        estoque.setQuantidadeAtual(BigDecimal.valueOf(1300));

        Lote lote = criarLote(62L, "FISICO-700", 1300, null, LocalDate.now().minusDays(1));
        lote.setTipoEmbalagem(TipoEmbalagem.FRASCO);
        lote.setConteudoPorApresentacao(BigDecimal.valueOf(500));
        lote.setFracionavel(true);

        RecipienteEstoque aberto =
                criarRecipiente(lote, 1, 500, 300, EstadoRecipienteEstoque.ABERTO);
        RecipienteEstoque fechado1 =
                criarRecipiente(lote, 2, 500, 500, EstadoRecipienteEstoque.FECHADO);
        RecipienteEstoque fechado2 =
                criarRecipiente(lote, 3, 500, 500, EstadoRecipienteEstoque.FECHADO);

        when(estoqueCentralRepository.buscarPorIdComBloqueio(3L))
                .thenReturn(Optional.of(estoque));
        when(loteRepository.buscarDisponiveisPorEntradaComBloqueio(3L))
                .thenReturn(List.of(lote));
        when(recipienteEstoqueRepository.findByLoteIdOrderBySequencialAsc(62L))
                .thenReturn(List.of(aberto, fechado1, fechado2));

        service.registrarSaida(
                3L,
                BigDecimal.valueOf(700),
                usuario,
                OrigemMovimentacao.PEDIDO,
                null,
                null,
                "Retirada 700 mL"
        );

        assertEquals(0, BigDecimal.ZERO.compareTo(fechado1.getQuantidadeDisponivel()));
        assertEquals(EstadoRecipienteEstoque.ESGOTADO, fechado1.getEstado());

        assertEquals(0, BigDecimal.valueOf(100).compareTo(aberto.getQuantidadeDisponivel()));
        assertEquals(EstadoRecipienteEstoque.ABERTO, aberto.getEstado());

        assertEquals(0, BigDecimal.valueOf(500).compareTo(fechado2.getQuantidadeDisponivel()));
        assertEquals(EstadoRecipienteEstoque.FECHADO, fechado2.getEstado());

        assertEquals(0, BigDecimal.valueOf(600).compareTo(lote.getQuantidadeDisponivel()));
        assertEquals(0, BigDecimal.valueOf(600).compareTo(estoque.getQuantidadeAtual()));

        List<MovimentacaoRecipiente> detalhes = capturarDetalhesDeRecipiente();
        assertEquals(2, detalhes.size());

        MovimentacaoRecipiente detalheFechado = detalhes.stream()
                .filter(d -> d.getRecipienteEstoque() == fechado1)
                .findFirst()
                .orElseThrow();
        MovimentacaoRecipiente detalheAberto = detalhes.stream()
                .filter(d -> d.getRecipienteEstoque() == aberto)
                .findFirst()
                .orElseThrow();

        assertEquals(0, BigDecimal.valueOf(500).compareTo(detalheFechado.getQuantidadeMovimentada()));
        assertEquals(true, detalheFechado.getEsgotouRecipiente());
        assertEquals(false, detalheFechado.getAbriuRecipiente());

        assertEquals(0, BigDecimal.valueOf(200).compareTo(detalheAberto.getQuantidadeMovimentada()));
        assertEquals(false, detalheAberto.getEsgotouRecipiente());
        assertEquals(false, detalheAberto.getAbriuRecipiente());
    }

    @Test
    void deveAbrirRecipienteFechadoQuandoNaoExisteAbertoParaFracao() {
        produto.setUnidadeMedida(UnidadeMedida.ML);
        estoque.setQuantidadeAtual(BigDecimal.valueOf(500));

        Lote lote = criarLote(63L, "FISICO-ABRIR", 500, null, LocalDate.now().minusDays(1));
        lote.setTipoEmbalagem(TipoEmbalagem.FRASCO);
        lote.setConteudoPorApresentacao(BigDecimal.valueOf(500));
        lote.setFracionavel(true);

        RecipienteEstoque fechado =
                criarRecipiente(lote, 1, 500, 500, EstadoRecipienteEstoque.FECHADO);

        when(estoqueCentralRepository.buscarPorIdComBloqueio(3L))
                .thenReturn(Optional.of(estoque));
        when(loteRepository.buscarDisponiveisPorEntradaComBloqueio(3L))
                .thenReturn(List.of(lote));
        when(recipienteEstoqueRepository.findByLoteIdOrderBySequencialAsc(63L))
                .thenReturn(List.of(fechado));

        service.registrarSaida(
                3L,
                BigDecimal.valueOf(200),
                usuario,
                OrigemMovimentacao.PEDIDO,
                null,
                null,
                "Abrir frasco"
        );

        assertEquals(0, BigDecimal.valueOf(300).compareTo(fechado.getQuantidadeDisponivel()));
        assertEquals(EstadoRecipienteEstoque.ABERTO, fechado.getEstado());
        assertEquals(true, fechado.getDataAbertura() != null);
        assertEquals(null, fechado.getDataEsgotamento());

        assertEquals(0, BigDecimal.valueOf(300).compareTo(lote.getQuantidadeDisponivel()));
        assertEquals(0, BigDecimal.valueOf(300).compareTo(estoque.getQuantidadeAtual()));

        List<MovimentacaoRecipiente> detalhes = capturarDetalhesDeRecipiente();
        assertEquals(1, detalhes.size());
        assertEquals(EstadoRecipienteEstoque.FECHADO, detalhes.get(0).getEstadoAnterior());
        assertEquals(EstadoRecipienteEstoque.ABERTO, detalhes.get(0).getEstadoAtual());
        assertEquals(true, detalhes.get(0).getAbriuRecipiente());
        assertEquals(false, detalhes.get(0).getEsgotouRecipiente());
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
                        BigDecimal.valueOf(3),
                        usuario,
                        OrigemMovimentacao.PEDIDO,
                        null,
                        null,
                        null
                )
        );

        assertEquals(
                "Estoque utilizável insuficiente para a forma de retirada selecionada. Disponível nos lotes compatíveis: 2, solicitado: 3",
                exception.getMessage()
        );
        assertEquals(BigDecimal.valueOf(10), estoque.getQuantidadeAtual());
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

        when(estoqueCentralRepository.findByPublicId(ESTOQUE_PUBLIC_ID))
                .thenReturn(Optional.of(estoque));
        when(estoqueCentralRepository.buscarPorIdComBloqueio(3L))
                .thenReturn(Optional.of(estoque));
        when(loteRepository.buscarVencidosComBloqueio(any(), any(LocalDate.class)))
                .thenReturn(List.of(vencido1, vencido2));

        service.registrarDescarteVencimento(ESTOQUE_PUBLIC_ID, BigDecimal.valueOf(5), "Vencidos", usuario);

        assertEquals(BigDecimal.valueOf(0), vencido1.getQuantidadeDisponivel());
        assertEquals(BigDecimal.valueOf(1), vencido2.getQuantidadeDisponivel());
        assertEquals(BigDecimal.valueOf(5), estoque.getQuantidadeAtual());
    }

    @Test
    void deveRestaurarOsMesmosLotesConsumidosNoCancelamento() {
        Lote loteA = criarLote(50L, "RET-A", 0, null, LocalDate.now().minusDays(10));
        loteA.setQuantidadeInicial(BigDecimal.valueOf(5));
        Lote loteB = criarLote(51L, "RET-B", 2, null, LocalDate.now().minusDays(5));
        loteB.setQuantidadeInicial(BigDecimal.valueOf(5));

        estoque.setQuantidadeAtual(BigDecimal.valueOf(2));

        com.sgl.model.Pedido pedido = com.sgl.model.Pedido.builder()
                .id(60L)
                .build();

        MovimentacaoEstoque saidaA = MovimentacaoEstoque.builder()
                .id(70L)
                .estoqueCentral(estoque)
                .lote(loteA)
                .pedido(pedido)
                .quantidadeMovimentada(BigDecimal.valueOf(5))
                .tipoMovimentacao(TipoMovimentacao.SAIDA)
                .build();

        MovimentacaoEstoque saidaB = MovimentacaoEstoque.builder()
                .id(71L)
                .estoqueCentral(estoque)
                .lote(loteB)
                .pedido(pedido)
                .quantidadeMovimentada(BigDecimal.valueOf(3))
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

        assertEquals(BigDecimal.valueOf(5), loteA.getQuantidadeDisponivel());
        assertEquals(BigDecimal.valueOf(5), loteB.getQuantidadeDisponivel());
        assertEquals(BigDecimal.valueOf(10), estoque.getQuantidadeAtual());
    }

    private RecipienteEstoque criarRecipiente(
            Lote lote,
            int sequencial,
            int capacidade,
            int quantidadeDisponivel,
            EstadoRecipienteEstoque estado) {

        RecipienteEstoque recipiente = new RecipienteEstoque();
        recipiente.setId(1000L + lote.getId() * 10 + sequencial);
        recipiente.setPublicId(UUID.randomUUID());
        recipiente.setLote(lote);
        recipiente.definirIdentificacao(
                lote.getCodigoInterno() + "-R" + String.format("%03d", sequencial),
                sequencial
        );
        recipiente.setTipoEmbalagem(
                lote.getTipoEmbalagem() == null ? TipoEmbalagem.UNITARIO : lote.getTipoEmbalagem()
        );
        recipiente.setCapacidadeInicial(BigDecimal.valueOf(capacidade));
        recipiente.setQuantidadeDisponivel(BigDecimal.valueOf(quantidadeDisponivel));
        recipiente.setUnidadeMedida(produto.getUnidadeMedida());
        recipiente.setEstado(estado);

        if (estado == EstadoRecipienteEstoque.ABERTO) {
            recipiente.setDataAbertura(LocalDateTime.now().minusDays(1));
        }

        if (estado == EstadoRecipienteEstoque.ESGOTADO) {
            recipiente.setDataEsgotamento(LocalDateTime.now().minusHours(1));
        }

        return recipiente;
    }

    private List<MovimentacaoRecipiente> capturarDetalhesDeRecipiente() {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        ArgumentCaptor<Iterable<MovimentacaoRecipiente>> captor =
                ArgumentCaptor.forClass((Class) Iterable.class);

        verify(movimentacaoRecipienteRepository).saveAll(captor.capture());

        return StreamSupport
                .stream(captor.getValue().spliterator(), false)
                .toList();
    }

    private Lote criarLote(
            Long id,
            String numero,
            int quantidade,
            LocalDate validade,
            LocalDate entrada) {

        Lote lote = new Lote();
        lote.setId(id);
        lote.setPublicId(UUID.randomUUID());
        lote.setEstoqueCentral(estoque);
        lote.definirCodigoInterno("LOT-TESTE-" + String.format("%03d", id), id.intValue());
        lote.setNumeroLote(numero);
        lote.setQuantidadeInicial(BigDecimal.valueOf(quantidade));
        lote.setQuantidadeDisponivel(BigDecimal.valueOf(quantidade));
        lote.setDataValidade(validade);
        lote.setDataEntrada(entrada);
        lote.setAtivo(true);
        return lote;
    }
}
