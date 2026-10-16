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

import com.sgl.dto.request.AjusteEstoqueRequestDTO;
import com.sgl.dto.request.EntradaLoteRequestDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.StockConflictException;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.Lote;
import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.MovimentacaoRecipiente;
import com.sgl.model.Produto;
import com.sgl.model.RecipienteEstoque;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.DestinoAjusteEntrada;
import com.sgl.model.enums.EstadoRecipienteEstoque;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.TipoAjusteEstoque;
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
        usuario.setPerfil(Perfil.GESTOR);
        usuario.setUnidade(unidade);
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
    void devePreservarQuantidadeDecimalNaEntradaFisica() {
        estoque.setQuantidadeAtual(BigDecimal.ZERO);
        produto.setUnidadeMedida(UnidadeMedida.L);

        EntradaLoteRequestDTO dto = new EntradaLoteRequestDTO();
        dto.setNumeroLote("FAB-DECIMAL-001");
        dto.setTipoEmbalagem(TipoEmbalagem.FRASCO);
        dto.setApresentacao("frasco de 250 mL");
        dto.setQuantidade(3);
        dto.setConteudoPorApresentacao(new BigDecimal("0.250"));
        dto.setFracionavel(true);
        dto.setOrigem(OrigemMovimentacao.COMPRA);

        when(estoqueCentralRepository.findByPublicId(ESTOQUE_PUBLIC_ID))
                .thenReturn(Optional.of(estoque));
        when(estoqueCentralRepository.buscarPorIdComBloqueio(3L))
                .thenReturn(Optional.of(estoque));
        when(loteRepository.existsByEstoqueCentralIdAndNumeroLote(3L, "FAB-DECIMAL-001"))
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
        recipientes.forEach(recipiente -> {
            assertEquals(0, new BigDecimal("0.250").compareTo(recipiente.getCapacidadeInicial()));
            assertEquals(0, new BigDecimal("0.250").compareTo(recipiente.getQuantidadeDisponivel()));
            assertEquals(UnidadeMedida.L, recipiente.getUnidadeMedida());
        });

        ArgumentCaptor<Lote> loteCaptor = ArgumentCaptor.forClass(Lote.class);
        verify(loteRepository).save(loteCaptor.capture());

        Lote lote = loteCaptor.getValue();
        assertEquals(0, new BigDecimal("0.750").compareTo(lote.getQuantidadeInicial()));
        assertEquals(0, new BigDecimal("0.750").compareTo(lote.getQuantidadeDisponivel()));
        assertEquals(0, new BigDecimal("0.750").compareTo(estoque.getQuantidadeAtual()));

        ArgumentCaptor<MovimentacaoEstoque> movimentacaoCaptor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoRepository).save(movimentacaoCaptor.capture());

        assertEquals(
                0,
                new BigDecimal("0.750")
                        .compareTo(movimentacaoCaptor.getValue().getQuantidadeMovimentada())
        );
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
        when(recipienteEstoqueRepository.buscarDisponiveisPorLoteComBloqueio(10L))
                .thenReturn(List.of(criarRecipiente(primeiro, 1, 4, 4, EstadoRecipienteEstoque.FECHADO)));
        when(recipienteEstoqueRepository.buscarDisponiveisPorLoteComBloqueio(11L))
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
        when(recipienteEstoqueRepository.buscarDisponiveisPorLoteComBloqueio(20L))
                .thenReturn(List.of(criarRecipiente(vencePrimeiro, 1, 3, 3, EstadoRecipienteEstoque.FECHADO)));
        when(recipienteEstoqueRepository.buscarDisponiveisPorLoteComBloqueio(21L))
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
        when(recipienteEstoqueRepository.buscarDisponiveisPorLoteComBloqueio(60L))
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
    void devePreservarQuantidadeDecimalNaSaidaFracionaria() {
        produto.setUnidadeMedida(UnidadeMedida.L);
        estoque.setQuantidadeAtual(new BigDecimal("0.750"));

        Lote lote = criarLote(65L, "FISICO-DECIMAL", 1, null, LocalDate.now().minusDays(1));
        lote.setTipoEmbalagem(TipoEmbalagem.FRASCO);
        lote.setConteudoPorApresentacao(new BigDecimal("0.750"));
        lote.setFracionavel(true);
        lote.setQuantidadeInicial(new BigDecimal("0.750"));
        lote.setQuantidadeDisponivel(new BigDecimal("0.750"));

        RecipienteEstoque recipiente =
                criarRecipiente(lote, 1, 1, 1, EstadoRecipienteEstoque.FECHADO);
        recipiente.setCapacidadeInicial(new BigDecimal("0.750"));
        recipiente.setQuantidadeDisponivel(new BigDecimal("0.750"));
        recipiente.setUnidadeMedida(UnidadeMedida.L);

        when(estoqueCentralRepository.buscarPorIdComBloqueio(3L))
                .thenReturn(Optional.of(estoque));
        when(loteRepository.buscarDisponiveisPorEntradaComBloqueio(3L))
                .thenReturn(List.of(lote));
        when(recipienteEstoqueRepository.buscarDisponiveisPorLoteComBloqueio(65L))
                .thenReturn(List.of(recipiente));

        service.registrarSaida(
                3L,
                new BigDecimal("0.125"),
                usuario,
                OrigemMovimentacao.PEDIDO,
                null,
                null,
                "Retirada decimal"
        );

        assertEquals(0, new BigDecimal("0.625").compareTo(recipiente.getQuantidadeDisponivel()));
        assertEquals(EstadoRecipienteEstoque.ABERTO, recipiente.getEstado());
        assertEquals(0, new BigDecimal("0.625").compareTo(lote.getQuantidadeDisponivel()));
        assertEquals(0, new BigDecimal("0.625").compareTo(estoque.getQuantidadeAtual()));

        List<MovimentacaoRecipiente> detalhes = capturarDetalhesDeRecipiente();
        assertEquals(1, detalhes.size());
        assertEquals(0, new BigDecimal("0.125").compareTo(detalhes.get(0).getQuantidadeMovimentada()));
        assertEquals(0, new BigDecimal("0.625").compareTo(detalhes.get(0).getQuantidadeAtual()));
        assertEquals(true, detalhes.get(0).getAbriuRecipiente());
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
        when(recipienteEstoqueRepository.buscarDisponiveisPorLoteComBloqueio(61L))
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
        when(recipienteEstoqueRepository.buscarDisponiveisPorLoteComBloqueio(62L))
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
        when(recipienteEstoqueRepository.buscarDisponiveisPorLoteComBloqueio(63L))
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
    void deveFalharComConflitoQuandoSaldoDoLoteDivergeDosRecipientesAposLock() {
        produto.setUnidadeMedida(UnidadeMedida.ML);
        estoque.setQuantidadeAtual(BigDecimal.valueOf(500));

        Lote lote = criarLote(
                64L,
                "FISICO-DIVERGENTE",
                500,
                null,
                LocalDate.now().minusDays(1)
        );
        lote.setTipoEmbalagem(TipoEmbalagem.FRASCO);
        lote.setConteudoPorApresentacao(BigDecimal.valueOf(500));
        lote.setFracionavel(true);

        RecipienteEstoque recipiente =
                criarRecipiente(
                        lote,
                        1,
                        500,
                        300,
                        EstadoRecipienteEstoque.ABERTO
                );

        when(estoqueCentralRepository.buscarPorIdComBloqueio(3L))
                .thenReturn(Optional.of(estoque));
        when(loteRepository.buscarDisponiveisPorEntradaComBloqueio(3L))
                .thenReturn(List.of(lote));
        when(recipienteEstoqueRepository.buscarDisponiveisPorLoteComBloqueio(64L))
                .thenReturn(List.of(recipiente));

        StockConflictException exception = assertThrows(
                StockConflictException.class,
                () -> service.registrarSaida(
                        3L,
                        BigDecimal.valueOf(200),
                        usuario,
                        OrigemMovimentacao.PEDIDO,
                        null,
                        null,
                        "Revalidar saldo físico"
                )
        );

        assertEquals(
                "O estoque físico foi alterado ou está inconsistente com o saldo do lote "
                        + lote.getCodigoInterno()
                        + ". Revise a disponibilidade antes de aprovar.",
                exception.getMessage()
        );

        assertEquals(0, BigDecimal.valueOf(500).compareTo(estoque.getQuantidadeAtual()));
        assertEquals(0, BigDecimal.valueOf(500).compareTo(lote.getQuantidadeDisponivel()));
        assertEquals(0, BigDecimal.valueOf(300).compareTo(recipiente.getQuantidadeDisponivel()));

        verify(recipienteEstoqueRepository, never()).saveAll(any());
        verify(loteRepository, never()).save(any());
        verify(estoqueCentralRepository, never()).save(any());
        verify(movimentacaoRepository, never()).save(any());
        verify(movimentacaoRecipienteRepository, never()).saveAll(any());
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
                "Estoque utilizável insuficiente para a forma de retirada selecionada. Disponível nos lotes compatíveis: 2, solicitado: 3. Revise a disponibilidade antes de aprovar.",
                exception.getMessage()
        );
        assertEquals(BigDecimal.valueOf(10), estoque.getQuantidadeAtual());
        verify(movimentacaoRepository, never()).save(any());
    }

    @Test
    void deveRegistrarAjusteEntradaCriandoNovoRecipienteAberto() {
        produto.setUnidadeMedida(UnidadeMedida.ML);
        estoque.setQuantidadeAtual(BigDecimal.valueOf(500));

        Lote lote = criarLote(70L, "AJUSTE-NOVO", 500, null, LocalDate.now().minusDays(1));
        lote.setTipoEmbalagem(TipoEmbalagem.FRASCO);
        lote.setConteudoPorApresentacao(BigDecimal.valueOf(500));
        lote.setFracionavel(true);

        RecipienteEstoque existente =
                criarRecipiente(lote, 1, 500, 500, EstadoRecipienteEstoque.FECHADO);

        AjusteEstoqueRequestDTO dto = novoAjusteBase(
                lote,
                TipoAjusteEstoque.ENTRADA,
                BigDecimal.valueOf(20)
        );
        dto.setDestinoEntrada(DestinoAjusteEntrada.NOVO_RECIPIENTE);
        dto.setTipoEmbalagem(TipoEmbalagem.FRASCO);

        prepararAjuste(estoque, lote, List.of(existente));
        when(recipienteEstoqueRepository.buscarMaiorSequencialPorLote(70L)).thenReturn(1);
        when(recipienteEstoqueRepository.existsByCodigoInterno(any())).thenReturn(false);

        service.ajustarEstoque(ESTOQUE_PUBLIC_ID, dto, usuario);

        ArgumentCaptor<RecipienteEstoque> recipienteCaptor =
                ArgumentCaptor.forClass(RecipienteEstoque.class);
        verify(recipienteEstoqueRepository).save(recipienteCaptor.capture());

        RecipienteEstoque criado = recipienteCaptor.getValue();
        assertEquals("LOT-TESTE-070-R002", criado.getCodigoInterno());
        assertEquals(2, criado.getSequencial());
        assertEquals(EstadoRecipienteEstoque.ABERTO, criado.getEstado());
        assertEquals(UnidadeMedida.ML, criado.getUnidadeMedida());
        assertEquals(0, BigDecimal.valueOf(20).compareTo(criado.getCapacidadeInicial()));
        assertEquals(0, BigDecimal.valueOf(20).compareTo(criado.getQuantidadeDisponivel()));

        assertEquals(0, BigDecimal.valueOf(520).compareTo(lote.getQuantidadeDisponivel()));
        assertEquals(0, BigDecimal.valueOf(520).compareTo(estoque.getQuantidadeAtual()));

        ArgumentCaptor<MovimentacaoEstoque> movimentoCaptor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoRepository).save(movimentoCaptor.capture());

        MovimentacaoEstoque movimento = movimentoCaptor.getValue();
        assertEquals(TipoMovimentacao.AJUSTE_ENTRADA, movimento.getTipoMovimentacao());
        assertEquals(OrigemMovimentacao.AJUSTE, movimento.getOrigem());
        assertEquals(0, BigDecimal.valueOf(20).compareTo(movimento.getQuantidadeMovimentada()));
        assertEquals(0, BigDecimal.valueOf(500).compareTo(movimento.getQuantidadeAnterior()));
        assertEquals(0, BigDecimal.valueOf(520).compareTo(movimento.getQuantidadeAtual()));
    }

    @Test
    void deveRegistrarAjusteEntradaEmRecipienteExistenteSemRestaurarLacre() {
        produto.setUnidadeMedida(UnidadeMedida.ML);
        estoque.setQuantidadeAtual(BigDecimal.valueOf(200));

        Lote lote = criarLote(71L, "AJUSTE-EXISTENTE", 200, null, LocalDate.now().minusDays(1));
        lote.setTipoEmbalagem(TipoEmbalagem.FRASCO);
        lote.setConteudoPorApresentacao(BigDecimal.valueOf(500));
        lote.setFracionavel(true);

        RecipienteEstoque recipiente =
                criarRecipiente(lote, 1, 500, 200, EstadoRecipienteEstoque.ABERTO);

        AjusteEstoqueRequestDTO dto = novoAjusteBase(
                lote,
                TipoAjusteEstoque.ENTRADA,
                BigDecimal.valueOf(50)
        );
        dto.setDestinoEntrada(DestinoAjusteEntrada.RECIPIENTE_EXISTENTE);
        dto.setRecipienteId(recipiente.getPublicId());

        prepararAjuste(estoque, lote, List.of(recipiente));
        when(recipienteEstoqueRepository.findByPublicId(recipiente.getPublicId()))
                .thenReturn(Optional.of(recipiente));
        when(recipienteEstoqueRepository.buscarPorIdComBloqueio(recipiente.getId()))
                .thenReturn(Optional.of(recipiente));

        service.ajustarEstoque(ESTOQUE_PUBLIC_ID, dto, usuario);

        assertEquals(0, BigDecimal.valueOf(250).compareTo(recipiente.getQuantidadeDisponivel()));
        assertEquals(EstadoRecipienteEstoque.ABERTO, recipiente.getEstado());
        assertEquals(0, BigDecimal.valueOf(250).compareTo(lote.getQuantidadeDisponivel()));
        assertEquals(0, BigDecimal.valueOf(250).compareTo(estoque.getQuantidadeAtual()));

        ArgumentCaptor<MovimentacaoRecipiente> detalheCaptor =
                ArgumentCaptor.forClass(MovimentacaoRecipiente.class);
        verify(movimentacaoRecipienteRepository).save(detalheCaptor.capture());

        MovimentacaoRecipiente detalhe = detalheCaptor.getValue();
        assertEquals(0, BigDecimal.valueOf(200).compareTo(detalhe.getQuantidadeAnterior()));
        assertEquals(0, BigDecimal.valueOf(50).compareTo(detalhe.getQuantidadeMovimentada()));
        assertEquals(0, BigDecimal.valueOf(250).compareTo(detalhe.getQuantidadeAtual()));
        assertEquals(EstadoRecipienteEstoque.ABERTO, detalhe.getEstadoAnterior());
        assertEquals(EstadoRecipienteEstoque.ABERTO, detalhe.getEstadoAtual());
        assertEquals(false, detalhe.getAbriuRecipiente());
    }

    @Test
    void deveBloquearAjusteEntradaQueUltrapassaCapacidadeDoRecipiente() {
        produto.setUnidadeMedida(UnidadeMedida.ML);
        estoque.setQuantidadeAtual(BigDecimal.valueOf(480));

        Lote lote = criarLote(72L, "AJUSTE-CAPACIDADE", 480, null, LocalDate.now().minusDays(1));
        lote.setTipoEmbalagem(TipoEmbalagem.FRASCO);
        lote.setConteudoPorApresentacao(BigDecimal.valueOf(500));
        lote.setFracionavel(true);

        RecipienteEstoque recipiente =
                criarRecipiente(lote, 1, 500, 480, EstadoRecipienteEstoque.ABERTO);

        AjusteEstoqueRequestDTO dto = novoAjusteBase(
                lote,
                TipoAjusteEstoque.ENTRADA,
                BigDecimal.valueOf(50)
        );
        dto.setDestinoEntrada(DestinoAjusteEntrada.RECIPIENTE_EXISTENTE);
        dto.setRecipienteId(recipiente.getPublicId());

        prepararAjuste(estoque, lote, List.of(recipiente));
        when(recipienteEstoqueRepository.findByPublicId(recipiente.getPublicId()))
                .thenReturn(Optional.of(recipiente));
        when(recipienteEstoqueRepository.buscarPorIdComBloqueio(recipiente.getId()))
                .thenReturn(Optional.of(recipiente));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.ajustarEstoque(ESTOQUE_PUBLIC_ID, dto, usuario)
        );

        assertEquals(
                "O ajuste ultrapassaria a capacidade inicial do recipiente. Utilize NOVO_RECIPIENTE.",
                exception.getMessage()
        );

        assertEquals(0, BigDecimal.valueOf(480).compareTo(recipiente.getQuantidadeDisponivel()));
        verify(movimentacaoRepository, never()).save(any());
    }

    @Test
    void deveRegistrarAjusteSaidaNoRecipienteEspecifico() {
        produto.setUnidadeMedida(UnidadeMedida.ML);
        estoque.setQuantidadeAtual(BigDecimal.valueOf(300));

        Lote lote = criarLote(73L, "AJUSTE-SAIDA", 300, null, LocalDate.now().minusDays(1));
        lote.setTipoEmbalagem(TipoEmbalagem.FRASCO);
        lote.setConteudoPorApresentacao(BigDecimal.valueOf(500));
        lote.setFracionavel(true);

        RecipienteEstoque recipiente =
                criarRecipiente(lote, 1, 500, 300, EstadoRecipienteEstoque.ABERTO);

        AjusteEstoqueRequestDTO dto = novoAjusteBase(
                lote,
                TipoAjusteEstoque.SAIDA,
                BigDecimal.valueOf(100)
        );
        dto.setRecipienteId(recipiente.getPublicId());

        prepararAjuste(estoque, lote, List.of(recipiente));
        when(recipienteEstoqueRepository.findByPublicId(recipiente.getPublicId()))
                .thenReturn(Optional.of(recipiente));
        when(recipienteEstoqueRepository.buscarPorIdComBloqueio(recipiente.getId()))
                .thenReturn(Optional.of(recipiente));

        service.ajustarEstoque(ESTOQUE_PUBLIC_ID, dto, usuario);

        assertEquals(0, BigDecimal.valueOf(200).compareTo(recipiente.getQuantidadeDisponivel()));
        assertEquals(EstadoRecipienteEstoque.ABERTO, recipiente.getEstado());
        assertEquals(0, BigDecimal.valueOf(200).compareTo(lote.getQuantidadeDisponivel()));
        assertEquals(0, BigDecimal.valueOf(200).compareTo(estoque.getQuantidadeAtual()));

        ArgumentCaptor<MovimentacaoEstoque> movimentoCaptor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoRepository).save(movimentoCaptor.capture());

        assertEquals(
                TipoMovimentacao.AJUSTE_SAIDA,
                movimentoCaptor.getValue().getTipoMovimentacao()
        );
        assertEquals(
                OrigemMovimentacao.AJUSTE,
                movimentoCaptor.getValue().getOrigem()
        );
    }

    @Test
    void deveImpedirAjustePorPerfilSemPermissao() {
        usuario.setPerfil(Perfil.TECNICO);

        AjusteEstoqueRequestDTO dto = new AjusteEstoqueRequestDTO();
        dto.setTipoAjuste(TipoAjusteEstoque.SAIDA);
        dto.setQuantidade(BigDecimal.ONE);
        dto.setUnidadeMedida(UnidadeMedida.UNIDADE);
        dto.setJustificativa("Conferência física");

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.ajustarEstoque(ESTOQUE_PUBLIC_ID, dto, usuario)
        );

        assertEquals(
                "Somente Gestor ou Administrador pode realizar ajuste de estoque.",
                exception.getMessage()
        );
        verify(estoqueCentralRepository, never()).findByPublicId(any());
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

        RecipienteEstoque recipiente1 =
                criarRecipiente(
                        vencido1,
                        1,
                        2,
                        2,
                        EstadoRecipienteEstoque.FECHADO
                );
        RecipienteEstoque recipiente2 =
                criarRecipiente(
                        vencido2,
                        1,
                        4,
                        4,
                        EstadoRecipienteEstoque.FECHADO
                );

        when(estoqueCentralRepository.findByPublicId(ESTOQUE_PUBLIC_ID))
                .thenReturn(Optional.of(estoque));
        when(estoqueCentralRepository.buscarPorIdComBloqueio(3L))
                .thenReturn(Optional.of(estoque));
        when(loteRepository.buscarVencidosComBloqueio(any(), any(LocalDate.class)))
                .thenReturn(List.of(vencido1, vencido2));
        when(recipienteEstoqueRepository.buscarDisponiveisPorLoteComBloqueio(40L))
                .thenReturn(List.of(recipiente1));
        when(recipienteEstoqueRepository.buscarDisponiveisPorLoteComBloqueio(41L))
                .thenReturn(List.of(recipiente2));

        service.registrarDescarteVencimento(
                ESTOQUE_PUBLIC_ID,
                BigDecimal.valueOf(5),
                "Vencidos",
                usuario
        );

        assertEquals(0, BigDecimal.ZERO.compareTo(vencido1.getQuantidadeDisponivel()));
        assertEquals(0, BigDecimal.ONE.compareTo(vencido2.getQuantidadeDisponivel()));
        assertEquals(0, BigDecimal.valueOf(5).compareTo(estoque.getQuantidadeAtual()));

        assertEquals(EstadoRecipienteEstoque.ESGOTADO, recipiente1.getEstado());
        assertEquals(0, BigDecimal.ZERO.compareTo(recipiente1.getQuantidadeDisponivel()));

        assertEquals(EstadoRecipienteEstoque.ABERTO, recipiente2.getEstado());
        assertEquals(0, BigDecimal.ONE.compareTo(recipiente2.getQuantidadeDisponivel()));
        assertEquals(true, recipiente2.getDataAbertura() != null);

        verify(movimentacaoRecipienteRepository, org.mockito.Mockito.times(2))
                .saveAll(any());
    }


    @Test
    void deveRestaurarRecipienteFechadoEsgotadoNoCancelamento() {
        Lote lote = criarLote(
                50L,
                "RET-FECHADO",
                0,
                null,
                LocalDate.now().minusDays(10)
        );
        lote.setQuantidadeInicial(BigDecimal.valueOf(5));

        estoque.setQuantidadeAtual(BigDecimal.ZERO);

        RecipienteEstoque recipiente =
                criarRecipiente(
                        lote,
                        1,
                        5,
                        0,
                        EstadoRecipienteEstoque.ESGOTADO
                );

        com.sgl.model.Pedido pedido =
                com.sgl.model.Pedido.builder()
                        .id(60L)
                        .build();

        MovimentacaoEstoque saida =
                MovimentacaoEstoque.builder()
                        .id(70L)
                        .estoqueCentral(estoque)
                        .lote(lote)
                        .pedido(pedido)
                        .usuario(usuario)
                        .quantidadeMovimentada(BigDecimal.valueOf(5))
                        .tipoMovimentacao(TipoMovimentacao.SAIDA)
                        .build();

        MovimentacaoRecipiente detalhe =
                MovimentacaoRecipiente.builder()
                        .id(80L)
                        .movimentacaoEstoque(saida)
                        .recipienteEstoque(recipiente)
                        .quantidadeAnterior(BigDecimal.valueOf(5))
                        .quantidadeMovimentada(BigDecimal.valueOf(5))
                        .quantidadeAtual(BigDecimal.ZERO)
                        .estadoAnterior(EstadoRecipienteEstoque.FECHADO)
                        .estadoAtual(EstadoRecipienteEstoque.ESGOTADO)
                        .abriuRecipiente(false)
                        .esgotouRecipiente(true)
                        .build();

        prepararDevolucao(
                pedido,
                saida,
                lote,
                List.of(),
                List.of(detalhe)
        );
        when(recipienteEstoqueRepository.buscarPorIdComBloqueio(recipiente.getId()))
                .thenReturn(Optional.of(recipiente));

        service.devolverSaidasDoPedido(
                pedido,
                null,
                "Cancelamento"
        );

        assertEquals(0, BigDecimal.valueOf(5).compareTo(recipiente.getQuantidadeDisponivel()));
        assertEquals(EstadoRecipienteEstoque.FECHADO, recipiente.getEstado());
        assertEquals(null, recipiente.getDataAbertura());
        assertEquals(null, recipiente.getDataEsgotamento());

        assertEquals(0, BigDecimal.valueOf(5).compareTo(lote.getQuantidadeDisponivel()));
        assertEquals(0, BigDecimal.valueOf(5).compareTo(estoque.getQuantidadeAtual()));

        ArgumentCaptor<MovimentacaoEstoque> movimentoCaptor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoRepository).save(movimentoCaptor.capture());

        assertEquals(
                TipoMovimentacao.DEVOLUCAO,
                movimentoCaptor.getValue().getTipoMovimentacao()
        );

        List<MovimentacaoRecipiente> reversoes =
                capturarDetalhesDeRecipiente();

        assertEquals(1, reversoes.size());
        assertEquals(EstadoRecipienteEstoque.ESGOTADO, reversoes.get(0).getEstadoAnterior());
        assertEquals(EstadoRecipienteEstoque.FECHADO, reversoes.get(0).getEstadoAtual());
        assertEquals(0, BigDecimal.ZERO.compareTo(reversoes.get(0).getQuantidadeAnterior()));
        assertEquals(0, BigDecimal.valueOf(5).compareTo(reversoes.get(0).getQuantidadeAtual()));
    }

    @Test
    void deveRestaurarRecipienteAbertoEsgotadoNoCancelamento() {
        produto.setUnidadeMedida(UnidadeMedida.ML);

        Lote lote = criarLote(
                51L,
                "RET-ABERTO",
                0,
                null,
                LocalDate.now().minusDays(5)
        );
        lote.setQuantidadeInicial(BigDecimal.valueOf(500));
        lote.setTipoEmbalagem(TipoEmbalagem.FRASCO);
        lote.setConteudoPorApresentacao(BigDecimal.valueOf(500));
        lote.setFracionavel(true);

        estoque.setQuantidadeAtual(BigDecimal.ZERO);

        RecipienteEstoque recipiente =
                criarRecipiente(
                        lote,
                        1,
                        500,
                        0,
                        EstadoRecipienteEstoque.ESGOTADO
                );
        LocalDateTime aberturaOriginal =
                LocalDateTime.now().minusDays(2);
        recipiente.setDataAbertura(aberturaOriginal);

        com.sgl.model.Pedido pedido =
                com.sgl.model.Pedido.builder()
                        .id(61L)
                        .build();

        MovimentacaoEstoque saida =
                MovimentacaoEstoque.builder()
                        .id(71L)
                        .estoqueCentral(estoque)
                        .lote(lote)
                        .pedido(pedido)
                        .usuario(usuario)
                        .quantidadeMovimentada(BigDecimal.valueOf(300))
                        .tipoMovimentacao(TipoMovimentacao.SAIDA)
                        .build();

        MovimentacaoRecipiente detalhe =
                MovimentacaoRecipiente.builder()
                        .id(81L)
                        .movimentacaoEstoque(saida)
                        .recipienteEstoque(recipiente)
                        .quantidadeAnterior(BigDecimal.valueOf(300))
                        .quantidadeMovimentada(BigDecimal.valueOf(300))
                        .quantidadeAtual(BigDecimal.ZERO)
                        .estadoAnterior(EstadoRecipienteEstoque.ABERTO)
                        .estadoAtual(EstadoRecipienteEstoque.ESGOTADO)
                        .abriuRecipiente(false)
                        .esgotouRecipiente(true)
                        .build();

        prepararDevolucao(
                pedido,
                saida,
                lote,
                List.of(),
                List.of(detalhe)
        );
        when(recipienteEstoqueRepository.buscarPorIdComBloqueio(recipiente.getId()))
                .thenReturn(Optional.of(recipiente));

        service.devolverSaidasDoPedido(
                pedido,
                null,
                "Cancelamento"
        );

        assertEquals(0, BigDecimal.valueOf(300).compareTo(recipiente.getQuantidadeDisponivel()));
        assertEquals(EstadoRecipienteEstoque.ABERTO, recipiente.getEstado());
        assertEquals(aberturaOriginal, recipiente.getDataAbertura());
        assertEquals(null, recipiente.getDataEsgotamento());

        assertEquals(0, BigDecimal.valueOf(300).compareTo(lote.getQuantidadeDisponivel()));
        assertEquals(0, BigDecimal.valueOf(300).compareTo(estoque.getQuantidadeAtual()));
    }

    @Test
    void deveBloquearCancelamentoQuandoRecipienteFoiMovimentadoDepoisDaSaida() {
        produto.setUnidadeMedida(UnidadeMedida.ML);

        Lote lote = criarLote(
                52L,
                "RET-CONFLITO",
                100,
                null,
                LocalDate.now().minusDays(4)
        );
        lote.setQuantidadeInicial(BigDecimal.valueOf(500));
        lote.setTipoEmbalagem(TipoEmbalagem.FRASCO);
        lote.setConteudoPorApresentacao(BigDecimal.valueOf(500));
        lote.setFracionavel(true);

        estoque.setQuantidadeAtual(BigDecimal.valueOf(100));

        RecipienteEstoque recipiente =
                criarRecipiente(
                        lote,
                        1,
                        500,
                        100,
                        EstadoRecipienteEstoque.ABERTO
                );

        com.sgl.model.Pedido pedido =
                com.sgl.model.Pedido.builder()
                        .id(62L)
                        .build();

        MovimentacaoEstoque saida =
                MovimentacaoEstoque.builder()
                        .id(72L)
                        .estoqueCentral(estoque)
                        .lote(lote)
                        .pedido(pedido)
                        .usuario(usuario)
                        .quantidadeMovimentada(BigDecimal.valueOf(200))
                        .tipoMovimentacao(TipoMovimentacao.SAIDA)
                        .build();

        MovimentacaoRecipiente detalhe =
                MovimentacaoRecipiente.builder()
                        .id(82L)
                        .movimentacaoEstoque(saida)
                        .recipienteEstoque(recipiente)
                        .quantidadeAnterior(BigDecimal.valueOf(500))
                        .quantidadeMovimentada(BigDecimal.valueOf(200))
                        .quantidadeAtual(BigDecimal.valueOf(300))
                        .estadoAnterior(EstadoRecipienteEstoque.FECHADO)
                        .estadoAtual(EstadoRecipienteEstoque.ABERTO)
                        .abriuRecipiente(true)
                        .esgotouRecipiente(false)
                        .build();

        prepararDevolucao(
                pedido,
                saida,
                lote,
                List.of(recipiente),
                List.of(detalhe)
        );
        when(recipienteEstoqueRepository.buscarPorIdComBloqueio(recipiente.getId()))
                .thenReturn(Optional.of(recipiente));

        StockConflictException exception =
                assertThrows(
                        StockConflictException.class,
                        () -> service.devolverSaidasDoPedido(
                                pedido,
                                null,
                                "Cancelamento"
                        )
                );

        assertEquals(
                "O recipiente "
                        + recipiente.getCodigoInterno()
                        + " sofreu outra movimentação após a saída "
                        + "deste pedido e não pode ser restaurado automaticamente.",
                exception.getMessage()
        );

        assertEquals(0, BigDecimal.valueOf(100).compareTo(recipiente.getQuantidadeDisponivel()));
        assertEquals(0, BigDecimal.valueOf(100).compareTo(lote.getQuantidadeDisponivel()));
        assertEquals(0, BigDecimal.valueOf(100).compareTo(estoque.getQuantidadeAtual()));

        verify(movimentacaoRepository, never()).save(any());
        verify(movimentacaoRecipienteRepository, never()).saveAll(any());
    }


    private void prepararDevolucao(
            com.sgl.model.Pedido pedido,
            MovimentacaoEstoque saida,
            Lote lote,
            List<RecipienteEstoque> disponiveis,
            List<MovimentacaoRecipiente> detalhes) {

        when(movimentacaoRepository
                .findByPedidoIdAndTipoMovimentacaoOrderByIdAsc(
                        pedido.getId(),
                        TipoMovimentacao.SAIDA
                ))
                .thenReturn(List.of(saida));

        when(estoqueCentralRepository.buscarPorIdComBloqueio(estoque.getId()))
                .thenReturn(Optional.of(estoque));

        when(loteRepository.buscarPorIdComBloqueio(lote.getId()))
                .thenReturn(Optional.of(lote));

        when(recipienteEstoqueRepository
                .buscarDisponiveisPorLoteComBloqueio(lote.getId()))
                .thenReturn(disponiveis);

        when(movimentacaoRecipienteRepository
                .findByMovimentacaoEstoqueIdOrderByIdAsc(saida.getId()))
                .thenReturn(detalhes);
    }

    private AjusteEstoqueRequestDTO novoAjusteBase(
            Lote lote,
            TipoAjusteEstoque tipo,
            BigDecimal quantidade) {

        AjusteEstoqueRequestDTO dto = new AjusteEstoqueRequestDTO();
        dto.setTipoAjuste(tipo);
        dto.setLoteId(lote.getPublicId());
        dto.setQuantidade(quantidade);
        dto.setUnidadeMedida(produto.getUnidadeMedida());
        dto.setJustificativa("Conferência física de estoque");
        dto.setObservacao("Teste de ajuste");
        return dto;
    }

    private void prepararAjuste(
            EstoqueCentral estoque,
            Lote lote,
            List<RecipienteEstoque> recipientes) {

        when(estoqueCentralRepository.findByPublicId(ESTOQUE_PUBLIC_ID))
                .thenReturn(Optional.of(estoque));
        when(estoqueCentralRepository.buscarPorIdComBloqueio(estoque.getId()))
                .thenReturn(Optional.of(estoque));
        when(loteRepository.findByPublicId(lote.getPublicId()))
                .thenReturn(Optional.of(lote));
        when(loteRepository.buscarPorIdComBloqueio(lote.getId()))
                .thenReturn(Optional.of(lote));
        when(recipienteEstoqueRepository.buscarDisponiveisPorLoteComBloqueio(lote.getId()))
                .thenReturn(recipientes);
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
