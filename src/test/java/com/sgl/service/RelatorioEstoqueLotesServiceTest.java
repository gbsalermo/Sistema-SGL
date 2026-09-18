package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.response.RelatorioEstoqueLotesResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.Lote;
import com.sgl.model.Produto;
import com.sgl.model.Unidade;
import com.sgl.model.enums.TipoEmbalagem;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.LoteRepository;

/**
 * Testes unitários de {@link RelatorioEstoqueLotesService}.
 *
 * O setUp monta um único estoque abaixo do mínimo com cinco lotes, um para
 * cada categoria possível de {@code classificarLote()}: VENCIDO,
 * PROXIMO_VENCIMENTO, VALIDO, ESGOTADO e INATIVO — assim o teste de
 * caminho feliz exercita as cinco contagens do relatório de uma vez.
 */
@ExtendWith(MockitoExtension.class)
class RelatorioEstoqueLotesServiceTest {

    @Mock
    private EstoqueCentralRepository estoqueCentralRepository;

    @Mock
    private LoteRepository loteRepository;

    @InjectMocks
    private RelatorioEstoqueLotesService relatorioEstoqueLotesService;

    private EstoqueCentral estoque;
    private Lote loteVencido;
    private Lote loteProximoVencimento;
    private Lote loteValido;
    private Lote loteEsgotado;
    private Lote loteInativo;

    @BeforeEach
    void setUp() {
        LocalDate hoje = LocalDate.now();

        Unidade unidade = new Unidade();
        unidade.setId(1L);
        unidade.setPublicId(UUID.randomUUID());
        unidade.setNome("Embrapa Mandioca e Fruticultura");
        unidade.setSigla("CNPMF");

        Produto produto = new Produto();
        produto.setId(10L);
        produto.setPublicId(UUID.randomUUID());
        produto.setNome("Ágar Nutriente");
        produto.setUnidadeMedida(UnidadeMedida.KG);
        produto.setCodigoReferencia("AGAR-01");

        // Abaixo do mínimo: quantidadeAtual (5) <= quantidadeMinima (10).
        estoque = EstoqueCentral.builder()
                .id(100L)
                .publicId(UUID.randomUUID())
                .unidade(unidade)
                .produto(produto)
                .quantidadeAtual(5)
                .quantidadeMinima(10)
                .ativo(true)
                .build();

        loteVencido = criarLote(1L, "L-1", true, 2, hoje.minusDays(5), estoque);
        loteProximoVencimento = criarLote(2L, "L-2", true, 2, hoje.plusDays(10), estoque);
        loteValido = criarLote(3L, "L-3", true, 2, hoje.plusDays(100), estoque);
        loteEsgotado = criarLote(4L, "L-4", true, 0, hoje.plusDays(50), estoque);
        loteInativo = criarLote(5L, "L-5", false, 5, hoje.plusDays(75), estoque);
    }

    private Lote criarLote(Long id, String codigoInterno, boolean ativo, int quantidadeDisponivel,
            LocalDate dataValidade, EstoqueCentral estoqueCentral) {
        Lote lote = new Lote();
        lote.setId(id);
        lote.setPublicId(UUID.randomUUID());
        lote.setEstoqueCentral(estoqueCentral);
        lote.definirCodigoInterno(codigoInterno, id.intValue());
        lote.setNumeroLote("FAB-" + codigoInterno);
        lote.setTipoEmbalagem(TipoEmbalagem.UNITARIO);
        lote.setFracionavel(true);
        lote.setQuantidadeInicial(5);
        lote.setQuantidadeDisponivel(quantidadeDisponivel);
        lote.setDataEntrada(LocalDate.of(2026, 1, 1));
        lote.setDataValidade(dataValidade);
        lote.setAtivo(ativo);
        return lote;
    }

    @Test
    void deveGerarRelatorioDeEstoqueLotesComSucesso() {
        when(estoqueCentralRepository.findAll()).thenReturn(List.of(estoque));
        when(loteRepository.findAll()).thenReturn(
                List.of(loteVencido, loteProximoVencimento, loteValido, loteEsgotado, loteInativo));

        RelatorioEstoqueLotesResponseDTO resultado = relatorioEstoqueLotesService.gerar(
                null, null, null, null, null, null, null);

        assertEquals(1, resultado.getTotalEstoques());
        assertEquals(1, resultado.getEstoquesAtivos());
        assertEquals(1, resultado.getEstoquesAbaixoMinimo());
        assertEquals(5L, resultado.getQuantidadeTotalEstoque());

        assertEquals(5, resultado.getTotalLotes());
        // Ativos e com saldo: vencido, próximo e válido (o esgotado tem
        // saldo zero, o inativo está com ativo=false).
        assertEquals(3, resultado.getLotesAtivos());
        assertEquals(1, resultado.getLotesVencidos());
        assertEquals(1, resultado.getLotesProximosVencimento());
        assertEquals(1, resultado.getLotesEsgotados());

        // Lotes ordenados por data de validade crescente.
        assertEquals("L-1", resultado.getLotes().get(0).getCodigoInterno());
        assertEquals("VENCIDO", resultado.getLotes().get(0).getSituacao());
        assertEquals("L-3", resultado.getLotes().get(4).getCodigoInterno());
        assertEquals("VALIDO", resultado.getLotes().get(4).getSituacao());
    }

    @Test
    void deveFiltrarPorSituacaoDeLoteValida() {
        when(estoqueCentralRepository.findAll()).thenReturn(List.of(estoque));
        when(loteRepository.findAll()).thenReturn(
                List.of(loteVencido, loteProximoVencimento, loteValido, loteEsgotado, loteInativo));

        RelatorioEstoqueLotesResponseDTO resultado = relatorioEstoqueLotesService.gerar(
                null, null, null, null, null, "vencido", null);

        assertEquals(1, resultado.getTotalLotes());
        assertEquals("L-1", resultado.getLotes().get(0).getCodigoInterno());
    }

    @Test
    void deveRejeitarQuandoPeriodoDeVencimentoForaDoIntervaloValido() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> relatorioEstoqueLotesService.gerar(null, null, null, null, null, null, 400));

        assertEquals("O período de vencimento deve estar entre 1 e 365 dias.", ex.getMessage());
    }

    @Test
    void deveRejeitarQuandoSituacaoDeLoteInvalida() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> relatorioEstoqueLotesService.gerar(
                        null, null, null, null, null, "SITUACAO_QUE_NAO_EXISTE", null));

        assertEquals("Situação de lote inválida para o relatório.", ex.getMessage());
    }
}
