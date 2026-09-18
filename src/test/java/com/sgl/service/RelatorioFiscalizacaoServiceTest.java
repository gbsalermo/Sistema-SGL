package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.response.RelatorioFiscalizacaoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.Lote;
import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.Produto;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.OrgaoFiscalizador;
import com.sgl.model.enums.TipoEmbalagem;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.LoteRepository;
import com.sgl.repository.MovimentacaoEstoqueRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.UnidadeRepository;

/**
 * Testes unitários de {@link RelatorioFiscalizacaoService}.
 *
 * É o relatório com mais dependências (5 repositórios) e a validação mais
 * particular do batch: {@code produtoId} pode falhar de duas formas
 * distintas — produto inexistente (404) ou produto existente mas não
 * marcado como fiscalizado (regra de negócio, 422/400) — cobertas em testes
 * separados.
 */
@ExtendWith(MockitoExtension.class)
class RelatorioFiscalizacaoServiceTest {

    private static final UUID UNIDADE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PRODUTO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private EstoqueCentralRepository estoqueCentralRepository;

    @Mock
    private LoteRepository loteRepository;

    @Mock
    private MovimentacaoEstoqueRepository movimentacaoRepository;

    @Mock
    private UnidadeRepository unidadeRepository;

    @InjectMocks
    private RelatorioFiscalizacaoService relatorioFiscalizacaoService;

    private Unidade unidade;
    private Produto produtoFiscalizado;
    private Produto produtoNaoFiscalizado;
    private EstoqueCentral estoque;
    private Lote loteVencido;
    private Lote loteProximoVencimento;
    private MovimentacaoEstoque entrada;
    private MovimentacaoEstoque saida;

    @BeforeEach
    void setUp() {
        LocalDate hoje = LocalDate.now();

        unidade = new Unidade();
        unidade.setId(1L);
        unidade.setPublicId(UNIDADE_PUBLIC_ID);
        unidade.setNome("Embrapa Mandioca e Fruticultura");
        unidade.setSigla("CNPMF");

        produtoFiscalizado = new Produto();
        produtoFiscalizado.setId(10L);
        produtoFiscalizado.setPublicId(PRODUTO_PUBLIC_ID);
        produtoFiscalizado.setNome("Éter Etílico");
        produtoFiscalizado.setCodigoReferencia("ETER-01");
        produtoFiscalizado.setUnidadeMedida(UnidadeMedida.L);
        produtoFiscalizado.setFiscalizado(true);
        produtoFiscalizado.setOrgaosFiscalizadores(Set.of(OrgaoFiscalizador.EXERCITO));

        produtoNaoFiscalizado = new Produto();
        produtoNaoFiscalizado.setId(11L);
        produtoNaoFiscalizado.setPublicId(UUID.fromString("00000000-0000-0000-0000-000000000003"));
        produtoNaoFiscalizado.setNome("Água destilada");
        produtoNaoFiscalizado.setUnidadeMedida(UnidadeMedida.L);
        produtoNaoFiscalizado.setFiscalizado(false);

        estoque = EstoqueCentral.builder()
                .id(20L)
                .publicId(UUID.randomUUID())
                .unidade(unidade)
                .produto(produtoFiscalizado)
                .quantidadeAtual(8)
                .quantidadeMinima(2)
                .ativo(true)
                .build();

        loteVencido = criarLote(1L, "L-1", 3, hoje.minusDays(2));
        loteProximoVencimento = criarLote(2L, "L-2", 2, hoje.plusDays(10));

        Usuario usuario = new Usuario();
        usuario.setId(30L);
        usuario.setPublicId(UUID.randomUUID());
        usuario.setNome("Usuário de Teste");

        entrada = MovimentacaoEstoque.builder()
                .id(200L)
                .publicId(UUID.randomUUID())
                .produto(produtoFiscalizado)
                .usuario(usuario)
                .estoqueCentral(estoque)
                .tipoMovimentacao(TipoMovimentacao.ENTRADA)
                .origem(OrigemMovimentacao.COMPRA)
                .quantidadeMovimentada(5)
                .quantidadeAnterior(0)
                .quantidadeAtual(5)
                .dataMovimentacao(LocalDateTime.of(2026, 1, 1, 8, 0))
                .build();

        saida = MovimentacaoEstoque.builder()
                .id(201L)
                .publicId(UUID.randomUUID())
                .produto(produtoFiscalizado)
                .usuario(usuario)
                .estoqueCentral(estoque)
                .tipoMovimentacao(TipoMovimentacao.SAIDA)
                .origem(OrigemMovimentacao.PEDIDO)
                .quantidadeMovimentada(2)
                .quantidadeAnterior(5)
                .quantidadeAtual(3)
                .dataMovimentacao(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();
    }

    private Lote criarLote(Long id, String codigoInterno, int quantidadeDisponivel, LocalDate dataValidade) {
        Lote lote = new Lote();
        lote.setId(id);
        lote.setPublicId(UUID.randomUUID());
        lote.setEstoqueCentral(estoque);
        lote.definirCodigoInterno(codigoInterno, id.intValue());
        lote.setNumeroLote("FAB-" + codigoInterno);
        lote.setTipoEmbalagem(TipoEmbalagem.UNITARIO);
        lote.setFracionavel(true);
        lote.setQuantidadeInicial(5);
        lote.setQuantidadeDisponivel(quantidadeDisponivel);
        lote.setDataEntrada(LocalDate.of(2026, 1, 1));
        lote.setDataValidade(dataValidade);
        lote.setAtivo(true);
        return lote;
    }

    @Test
    void deveGerarRelatorioDeFiscalizacaoComSucesso() {
        when(produtoRepository.findAll()).thenReturn(List.of(produtoFiscalizado, produtoNaoFiscalizado));
        when(estoqueCentralRepository.findAll()).thenReturn(List.of(estoque));
        when(loteRepository.findAll()).thenReturn(List.of(loteVencido, loteProximoVencimento));
        when(movimentacaoRepository.findAll()).thenReturn(List.of(entrada, saida));

        RelatorioFiscalizacaoResponseDTO resultado = relatorioFiscalizacaoService.gerar(
                null, null, null, null, null, null);

        assertEquals(1, resultado.getTotalProdutosFiscalizados());
        assertEquals(8, resultado.getSaldoAtualTotal());
        assertEquals(2, resultado.getLotesAtivos());
        assertEquals(1, resultado.getLotesVencidos());
        assertEquals(1, resultado.getLotesProximosVencimento());
        assertEquals(5, resultado.getQuantidadeEntradas());
        assertEquals(2, resultado.getQuantidadeSaidas());

        assertEquals(1, resultado.getProdutos().size());
        RelatorioFiscalizacaoResponseDTO.ProdutoFiscalizadoItem item = resultado.getProdutos().get(0);
        assertEquals(PRODUTO_PUBLIC_ID, item.getProdutoId());
        assertEquals(8, item.getSaldoAtual());
        assertEquals(LocalDate.now().plusDays(10), item.getProximoVencimento());

        // Trilha de movimentações ordenada da mais recente para a mais antiga.
        assertEquals(2, resultado.getMovimentacoes().size());
        assertEquals(saida.getPublicId(), resultado.getMovimentacoes().get(0).getMovimentacaoId());
    }

    @Test
    void deveFiltrarPorUnidadeValida() {
        when(unidadeRepository.findByPublicId(UNIDADE_PUBLIC_ID)).thenReturn(Optional.of(unidade));
        when(produtoRepository.findAll()).thenReturn(List.of(produtoFiscalizado));
        when(estoqueCentralRepository.findAll()).thenReturn(List.of(estoque));
        when(loteRepository.findAll()).thenReturn(List.of(loteVencido, loteProximoVencimento));
        when(movimentacaoRepository.findAll()).thenReturn(List.of(entrada, saida));

        RelatorioFiscalizacaoResponseDTO resultado = relatorioFiscalizacaoService.gerar(
                null, null, UNIDADE_PUBLIC_ID, null, null, null);

        assertEquals(1, resultado.getTotalProdutosFiscalizados());
    }

    @Test
    void deveLancarExcecaoQuandoUnidadeDoFiltroNaoEncontrada() {
        UUID idInexistente = UUID.randomUUID();
        when(unidadeRepository.findByPublicId(idInexistente)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> relatorioFiscalizacaoService.gerar(null, null, idInexistente, null, null, null));

        assertEquals("Unidade não encontrada.", ex.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoProdutoDoFiltroNaoEncontrado() {
        UUID idInexistente = UUID.randomUUID();
        when(produtoRepository.findAll()).thenReturn(List.of(produtoFiscalizado));
        when(produtoRepository.findByPublicId(idInexistente)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> relatorioFiscalizacaoService.gerar(idInexistente, null, null, null, null, null));

        assertEquals("Produto não encontrado.", ex.getMessage());
    }

    @Test
    void deveRejeitarQuandoProdutoNaoFiscalizado() {
        when(produtoRepository.findAll()).thenReturn(List.of(produtoFiscalizado, produtoNaoFiscalizado));
        when(produtoRepository.findByPublicId(produtoNaoFiscalizado.getPublicId()))
                .thenReturn(Optional.of(produtoNaoFiscalizado));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> relatorioFiscalizacaoService.gerar(
                        produtoNaoFiscalizado.getPublicId(), null, null, null, null, null));

        assertEquals("O produto informado não está classificado como fiscalizado.", ex.getMessage());
    }

    @Test
    void deveRejeitarQuandoJanelaDeVencimentoForaDoIntervaloValido() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> relatorioFiscalizacaoService.gerar(null, null, null, null, null, 0));

        assertEquals("A janela de vencimento deve estar entre 1 e 365 dias.", ex.getMessage());
    }

    @Test
    void deveRejeitarQuandoFiltroDePeriodoIncompleto() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> relatorioFiscalizacaoService.gerar(
                        null, null, null, LocalDate.of(2026, 1, 1), null, null));

        assertEquals("Para filtrar por período, informe dataInicio e dataFim.", ex.getMessage());
    }

    @Test
    void deveRejeitarQuandoDataInicialPosteriorADataFinal() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> relatorioFiscalizacaoService.gerar(
                        null, null, null,
                        LocalDate.of(2026, 3, 1), LocalDate.of(2026, 1, 1), null));

        assertEquals("A data inicial não pode ser posterior à data final.", ex.getMessage());
    }
}
