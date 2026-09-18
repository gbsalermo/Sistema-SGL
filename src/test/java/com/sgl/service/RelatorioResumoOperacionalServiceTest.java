package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.response.RelatorioResumoOperacionalResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.Lote;
import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.Produto;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.TipoEmbalagem;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.repository.MovimentacaoEstoqueRepository;

/**
 * Testes unitários de {@link RelatorioResumoOperacionalService}.
 *
 * O relatório monta três rankings em memória (produtos mais recebidos,
 * produtos mais retirados e lotes mais movimentados) a partir da lista
 * "achatada" de {@link MovimentacaoEstoque}. O setUp usa dois produtos e um
 * único lote (para que {@code lotesMaisMovimentados} acumule mais de uma
 * movimentação no mesmo lote).
 */
@ExtendWith(MockitoExtension.class)
class RelatorioResumoOperacionalServiceTest {

    private static final UUID PRODUTO_A_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private MovimentacaoEstoqueRepository movimentacaoRepository;

    @InjectMocks
    private RelatorioResumoOperacionalService relatorioResumoOperacionalService;

    private MovimentacaoEstoque entradaProdutoA;
    private MovimentacaoEstoque saidaProdutoA;
    private MovimentacaoEstoque entradaProdutoB;
    private MovimentacaoEstoque descarteProdutoA;

    @BeforeEach
    void setUp() {
        Unidade unidade = new Unidade();
        unidade.setId(1L);
        unidade.setPublicId(UUID.randomUUID());

        Produto produtoA = new Produto();
        produtoA.setId(10L);
        produtoA.setPublicId(PRODUTO_A_PUBLIC_ID);
        produtoA.setNome("Ágar Nutriente");
        produtoA.setUnidadeMedida(UnidadeMedida.KG);

        Produto produtoB = new Produto();
        produtoB.setId(11L);
        produtoB.setPublicId(UUID.randomUUID());
        produtoB.setNome("Etanol 70%");
        produtoB.setUnidadeMedida(UnidadeMedida.L);

        EstoqueCentral estoqueA = EstoqueCentral.builder()
                .id(20L)
                .publicId(UUID.randomUUID())
                .unidade(unidade)
                .produto(produtoA)
                .quantidadeAtual(6)
                .quantidadeMinima(1)
                .ativo(true)
                .build();

        Lote loteA = new Lote();
        loteA.setId(30L);
        loteA.setPublicId(UUID.randomUUID());
        loteA.setEstoqueCentral(estoqueA);
        loteA.definirCodigoInterno("LOT-AGAR-001", 1);
        loteA.setNumeroLote("FAB-2026-001");
        loteA.setTipoEmbalagem(TipoEmbalagem.UNITARIO);
        loteA.setFracionavel(true);
        loteA.setQuantidadeInicial(20);
        loteA.setQuantidadeDisponivel(6);
        loteA.setDataEntrada(LocalDate.of(2026, 1, 1));
        loteA.setDataValidade(LocalDate.of(2027, 1, 1));
        loteA.setAtivo(true);

        Usuario usuario = new Usuario();
        usuario.setId(40L);
        usuario.setPublicId(UUID.randomUUID());
        usuario.setNome("Usuário de Teste");

        entradaProdutoA = MovimentacaoEstoque.builder()
                .id(100L)
                .publicId(UUID.randomUUID())
                .produto(produtoA)
                .usuario(usuario)
                .estoqueCentral(estoqueA)
                .lote(loteA)
                .tipoMovimentacao(TipoMovimentacao.ENTRADA)
                .origem(OrigemMovimentacao.COMPRA)
                .quantidadeMovimentada(10)
                .dataMovimentacao(LocalDateTime.of(2026, 1, 10, 8, 0))
                .build();

        saidaProdutoA = MovimentacaoEstoque.builder()
                .id(101L)
                .publicId(UUID.randomUUID())
                .produto(produtoA)
                .usuario(usuario)
                .estoqueCentral(estoqueA)
                .lote(loteA)
                .tipoMovimentacao(TipoMovimentacao.SAIDA)
                .origem(OrigemMovimentacao.PEDIDO)
                .quantidadeMovimentada(4)
                .dataMovimentacao(LocalDateTime.of(2026, 1, 15, 8, 0))
                .build();

        descarteProdutoA = MovimentacaoEstoque.builder()
                .id(102L)
                .publicId(UUID.randomUUID())
                .produto(produtoA)
                .usuario(usuario)
                .estoqueCentral(estoqueA)
                .lote(loteA)
                .tipoMovimentacao(TipoMovimentacao.DESCARTE_VENCIMENTO)
                .origem(OrigemMovimentacao.DESCARTE)
                .quantidadeMovimentada(1)
                .dataMovimentacao(LocalDateTime.of(2026, 1, 20, 8, 0))
                .build();

        entradaProdutoB = MovimentacaoEstoque.builder()
                .id(103L)
                .publicId(UUID.randomUUID())
                .produto(produtoB)
                .usuario(usuario)
                .estoqueCentral(estoqueA)
                .tipoMovimentacao(TipoMovimentacao.ENTRADA)
                .origem(OrigemMovimentacao.COMPRA)
                .quantidadeMovimentada(6)
                .dataMovimentacao(LocalDateTime.of(2026, 1, 12, 8, 0))
                .build();
    }

    @Test
    void deveGerarResumoOperacionalComSucesso() {
        when(movimentacaoRepository.findAll()).thenReturn(
                List.of(entradaProdutoA, saidaProdutoA, descarteProdutoA, entradaProdutoB));

        RelatorioResumoOperacionalResponseDTO resultado = relatorioResumoOperacionalService.gerar(
                null, null, null, null);

        assertEquals(4, resultado.getTotalMovimentacoes());
        assertEquals(16, resultado.getQuantidadeEntradas());
        assertEquals(4, resultado.getQuantidadeSaidas());
        assertEquals(1, resultado.getQuantidadeDescartes());
        assertEquals(2, resultado.getProdutosMovimentados());
        assertEquals(1, resultado.getLotesMovimentados());

        // Ranking de entradas: produto A (10) na frente do produto B (6).
        assertEquals(2, resultado.getPrincipaisEntradas().size());
        assertEquals(PRODUTO_A_PUBLIC_ID, resultado.getPrincipaisEntradas().get(0).getProdutoId());
        assertEquals(10, resultado.getPrincipaisEntradas().get(0).getQuantidade());

        assertEquals(1, resultado.getPrincipaisSaidas().size());
        assertEquals(4, resultado.getPrincipaisSaidas().get(0).getQuantidade());

        // O único lote acumula as 3 movimentações do produto A.
        assertEquals(1, resultado.getLotesMaisMovimentados().size());
        RelatorioResumoOperacionalResponseDTO.LoteRanking loteRanking = resultado.getLotesMaisMovimentados().get(0);
        assertEquals(15, loteRanking.getQuantidadeMovimentada());
        assertEquals(3, loteRanking.getMovimentacoes());
        assertEquals(10, loteRanking.getQuantidadeEntradas());
        assertEquals(4, loteRanking.getQuantidadeSaidas());
    }

    @Test
    void deveAplicarLimiteDoRanking() {
        when(movimentacaoRepository.findAll()).thenReturn(
                List.of(entradaProdutoA, saidaProdutoA, descarteProdutoA, entradaProdutoB));

        RelatorioResumoOperacionalResponseDTO resultado = relatorioResumoOperacionalService.gerar(
                null, null, null, 1);

        assertEquals(1, resultado.getPrincipaisEntradas().size());
        assertEquals(PRODUTO_A_PUBLIC_ID, resultado.getPrincipaisEntradas().get(0).getProdutoId());
    }

    @Test
    void deveRejeitarQuandoLimiteDeRankingForaDoIntervaloValido() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> relatorioResumoOperacionalService.gerar(null, null, null, 51));

        assertEquals("O limite do ranking deve estar entre 1 e 50.", ex.getMessage());
    }

    @Test
    void deveRejeitarQuandoFiltroDePeriodoIncompleto() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> relatorioResumoOperacionalService.gerar(
                        null, LocalDate.of(2026, 1, 1), null, null));

        assertEquals("Para filtrar por período, informe dataInicio e dataFim.", ex.getMessage());
    }

    @Test
    void deveRejeitarQuandoDataInicialPosteriorADataFinal() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> relatorioResumoOperacionalService.gerar(
                        null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 1, 1), null));

        assertEquals("A data inicial não pode ser posterior à data final.", ex.getMessage());
    }
}
