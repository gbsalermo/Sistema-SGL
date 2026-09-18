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

import com.sgl.dto.response.RelatorioMovimentacoesResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.Laboratorio;
import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.Produto;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.repository.MovimentacaoEstoqueRepository;

/**
 * Testes unitários de {@link RelatorioMovimentacoesService}.
 */
@ExtendWith(MockitoExtension.class)
class RelatorioMovimentacoesServiceTest {

    private static final UUID PRODUTO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private MovimentacaoEstoqueRepository movimentacaoRepository;

    @InjectMocks
    private RelatorioMovimentacoesService relatorioMovimentacoesService;

    private Produto produto;
    private MovimentacaoEstoque entrada;
    private MovimentacaoEstoque saida;
    private MovimentacaoEstoque descarte;

    @BeforeEach
    void setUp() {
        Unidade unidade = new Unidade();
        unidade.setId(1L);
        unidade.setPublicId(UUID.randomUUID());

        Laboratorio laboratorio = Laboratorio.builder()
                .id(10L)
                .publicId(UUID.randomUUID())
                .unidade(unidade)
                .nome("Laboratório de Química")
                .ativo(true)
                .build();

        produto = new Produto();
        produto.setId(20L);
        produto.setPublicId(PRODUTO_PUBLIC_ID);
        produto.setNome("Ágar Nutriente");
        produto.setUnidadeMedida(UnidadeMedida.KG);

        Usuario usuario = new Usuario();
        usuario.setId(30L);
        usuario.setPublicId(UUID.randomUUID());
        usuario.setNome("Usuário de Teste");

        EstoqueCentral estoqueCentral = EstoqueCentral.builder()
                .id(40L)
                .publicId(UUID.randomUUID())
                .unidade(unidade)
                .produto(produto)
                .quantidadeAtual(4)
                .quantidadeMinima(1)
                .ativo(true)
                .build();

        entrada = MovimentacaoEstoque.builder()
                .id(100L)
                .publicId(UUID.randomUUID())
                .produto(produto)
                .laboratorio(laboratorio)
                .usuario(usuario)
                .estoqueCentral(estoqueCentral)
                .tipoMovimentacao(TipoMovimentacao.ENTRADA)
                .origem(OrigemMovimentacao.COMPRA)
                .quantidadeMovimentada(10)
                .quantidadeAnterior(0)
                .quantidadeAtual(10)
                .dataMovimentacao(LocalDateTime.of(2026, 1, 10, 8, 0))
                .build();

        saida = MovimentacaoEstoque.builder()
                .id(101L)
                .publicId(UUID.randomUUID())
                .produto(produto)
                .laboratorio(laboratorio)
                .usuario(usuario)
                .estoqueCentral(estoqueCentral)
                .tipoMovimentacao(TipoMovimentacao.SAIDA)
                .origem(OrigemMovimentacao.PEDIDO)
                .quantidadeMovimentada(4)
                .quantidadeAnterior(10)
                .quantidadeAtual(6)
                .dataMovimentacao(LocalDateTime.of(2026, 2, 5, 10, 0))
                .build();

        descarte = MovimentacaoEstoque.builder()
                .id(102L)
                .publicId(UUID.randomUUID())
                .produto(produto)
                .laboratorio(laboratorio)
                .usuario(usuario)
                .estoqueCentral(estoqueCentral)
                .tipoMovimentacao(TipoMovimentacao.DESCARTE_VENCIMENTO)
                .origem(OrigemMovimentacao.DESCARTE)
                .quantidadeMovimentada(2)
                .quantidadeAnterior(6)
                .quantidadeAtual(4)
                .dataMovimentacao(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();
    }

    @Test
    void deveGerarRelatorioDeMovimentacoesComSucesso() {
        when(movimentacaoRepository.findAll()).thenReturn(List.of(entrada, saida, descarte));

        RelatorioMovimentacoesResponseDTO resultado = relatorioMovimentacoesService.gerar(
                null, null, null, null, null, null, null, null);

        assertEquals(3, resultado.getTotalMovimentacoes());
        assertEquals(10, resultado.getQuantidadeEntradas());
        assertEquals(4, resultado.getQuantidadeSaidas());
        assertEquals(0, resultado.getQuantidadeAjustes());
        assertEquals(0, resultado.getQuantidadeDevolucoes());
        assertEquals(2, resultado.getQuantidadeDescartes());
        // Ordenado da mais recente para a mais antiga.
        assertEquals(descarte.getPublicId(), resultado.getItens().get(0).getId());
        assertEquals(entrada.getPublicId(), resultado.getItens().get(2).getId());
    }

    @Test
    void deveFiltrarPorTipoDeMovimentacao() {
        when(movimentacaoRepository.findAll()).thenReturn(List.of(entrada, saida, descarte));

        RelatorioMovimentacoesResponseDTO resultado = relatorioMovimentacoesService.gerar(
                TipoMovimentacao.SAIDA, null, null, null, null, null, null, null);

        assertEquals(1, resultado.getTotalMovimentacoes());
        assertEquals(saida.getPublicId(), resultado.getItens().get(0).getId());
    }

    @Test
    void deveFiltrarPorProdutoEPeriodo() {
        when(movimentacaoRepository.findAll()).thenReturn(List.of(entrada, saida, descarte));

        RelatorioMovimentacoesResponseDTO resultado = relatorioMovimentacoesService.gerar(
                null, null, PRODUTO_PUBLIC_ID, null, null, null,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));

        assertEquals(1, resultado.getTotalMovimentacoes());
        assertEquals(saida.getPublicId(), resultado.getItens().get(0).getId());
    }

    @Test
    void deveRejeitarQuandoFiltroDePeriodoIncompleto() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> relatorioMovimentacoesService.gerar(
                        null, null, null, null, null, null,
                        LocalDate.of(2026, 1, 1), null));

        assertEquals("Para filtrar por período, informe dataInicio e dataFim.", ex.getMessage());
    }

    @Test
    void deveRejeitarQuandoDataInicialPosteriorADataFinal() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> relatorioMovimentacoesService.gerar(
                        null, null, null, null, null, null,
                        LocalDate.of(2026, 3, 1), LocalDate.of(2026, 1, 1)));

        assertEquals("A data inicial não pode ser posterior à data final.", ex.getMessage());
    }
}
