package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.response.RelatorioProdutosResponseDTO;
import com.sgl.model.Produto;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.OrgaoFiscalizador;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.repository.ProdutoRepository;

/**
 * Testes unitários de {@link RelatorioProdutosService}.
 *
 * Serviço puramente agregador: não há {@code throw} no fonte (linhas 21–66),
 * apenas filtros em memória sobre {@code produtoRepository.findAll()} — por
 * isso não existem testes de {@code BusinessRuleException} aqui, ao
 * contrário dos demais relatórios deste batch.
 */
@ExtendWith(MockitoExtension.class)
class RelatorioProdutosServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @InjectMocks
    private RelatorioProdutosService relatorioProdutosService;

    private Produto ativoFiscalizadoAltoRisco;
    private Produto inativoNaoFiscalizado;

    @BeforeEach
    void setUp() {
        ativoFiscalizadoAltoRisco = new Produto();
        ativoFiscalizadoAltoRisco.setId(1L);
        ativoFiscalizadoAltoRisco.setNome("Ácido Sulfúrico");
        ativoFiscalizadoAltoRisco.setUnidadeMedida(UnidadeMedida.L);
        ativoFiscalizadoAltoRisco.setRisco(NivelRisco.ALTO);
        ativoFiscalizadoAltoRisco.setPerecivel(false);
        ativoFiscalizadoAltoRisco.setFiscalizado(true);
        ativoFiscalizadoAltoRisco.setOrgaosFiscalizadores(Set.of(OrgaoFiscalizador.POLICIA_FEDERAL));
        ativoFiscalizadoAltoRisco.setAtivo(true);

        inativoNaoFiscalizado = new Produto();
        inativoNaoFiscalizado.setId(2L);
        inativoNaoFiscalizado.setNome("Álcool 70%");
        inativoNaoFiscalizado.setUnidadeMedida(UnidadeMedida.L);
        inativoNaoFiscalizado.setRisco(NivelRisco.NENHUM);
        inativoNaoFiscalizado.setPerecivel(true);
        inativoNaoFiscalizado.setFiscalizado(false);
        inativoNaoFiscalizado.setAtivo(false);
    }

    @Test
    void deveGerarRelatorioDeProdutosComSucesso() {
        when(produtoRepository.findAll())
                .thenReturn(List.of(inativoNaoFiscalizado, ativoFiscalizadoAltoRisco));

        RelatorioProdutosResponseDTO resultado = relatorioProdutosService.gerar(
                null, null, null, null, null);

        assertEquals(2, resultado.getTotal());
        assertEquals(1, resultado.getAtivos());
        assertEquals(1, resultado.getInativos());
        assertEquals(1, resultado.getFiscalizados());
        assertEquals(1, resultado.getPereciveis());
        assertEquals(1, resultado.getComRisco());
        // Ordenados por nome (case-insensitive): "Ácido..." antes de "Álcool...".
        assertEquals("Ácido Sulfúrico", resultado.getItens().get(0).getNome());
    }

    @Test
    void deveGerarRelatorioVazioQuandoNaoHaProdutos() {
        when(produtoRepository.findAll()).thenReturn(List.of());

        RelatorioProdutosResponseDTO resultado = relatorioProdutosService.gerar(
                null, null, null, null, null);

        assertEquals(0, resultado.getTotal());
        assertEquals(0, resultado.getAtivos());
        assertEquals(0, resultado.getInativos());
        assertEquals(0, resultado.getFiscalizados());
        assertEquals(0, resultado.getPereciveis());
        assertEquals(0, resultado.getComRisco());
        assertTrue(resultado.getItens().isEmpty());
    }

    @Test
    void deveFiltrarPorFiscalizadoEOrgaoFiscalizador() {
        when(produtoRepository.findAll())
                .thenReturn(List.of(inativoNaoFiscalizado, ativoFiscalizadoAltoRisco));

        RelatorioProdutosResponseDTO resultado = relatorioProdutosService.gerar(
                null, true, null, null, OrgaoFiscalizador.POLICIA_FEDERAL);

        assertEquals(1, resultado.getTotal());
        assertEquals("Ácido Sulfúrico", resultado.getItens().get(0).getNome());
    }
}
