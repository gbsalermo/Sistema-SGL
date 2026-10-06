package com.sgl.model.medida;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.sgl.exception.BusinessRuleException;
import com.sgl.model.enums.DimensaoMedida;
import com.sgl.model.enums.GrupoConversaoMedida;
import com.sgl.model.enums.UnidadeMedida;

class ConversorUnidadeMedidaTest {

    @Test
    void deveConverterLitrosParaMililitros() {
        BigDecimal convertido = ConversorUnidadeMedida.converter(
                new BigDecimal("1.5"),
                UnidadeMedida.L,
                UnidadeMedida.ML
        );

        assertEquals(0, new BigDecimal("1500").compareTo(convertido));
    }

    @Test
    void deveConverterQuilogramasParaGramas() {
        BigDecimal convertido = ConversorUnidadeMedida.converter(
                new BigDecimal("2.75"),
                UnidadeMedida.KG,
                UnidadeMedida.G
        );

        assertEquals(0, new BigDecimal("2750").compareTo(convertido));
    }

    @Test
    void deveConverterGramasParaMiligramas() {
        BigDecimal convertido = ConversorUnidadeMedida.converter(
                new BigDecimal("0.125"),
                UnidadeMedida.G,
                UnidadeMedida.MG
        );

        assertEquals(0, new BigDecimal("125").compareTo(convertido));
    }

    @Test
    void deveManterQuantidadeQuandoUnidadeForIgual() {
        BigDecimal quantidade = new BigDecimal("125.500");

        BigDecimal convertido = ConversorUnidadeMedida.converter(
                quantidade,
                UnidadeMedida.ML,
                UnidadeMedida.ML
        );

        assertEquals(quantidade, convertido);
    }

    @Test
    void naoDeveConverterMassaParaVolume() {
        assertThrows(
                BusinessRuleException.class,
                () -> ConversorUnidadeMedida.converter(
                        BigDecimal.ONE,
                        UnidadeMedida.G,
                        UnidadeMedida.ML
                )
        );
    }

    @Test
    void naoDeveConverterUnidadeParaReacaoMesmoCompartilhandoDimensaoContagem() {
        assertEquals(DimensaoMedida.CONTAGEM, UnidadeMedida.UNIDADE.getDimensao());
        assertEquals(DimensaoMedida.CONTAGEM, UnidadeMedida.REACAO.getDimensao());
        assertEquals(GrupoConversaoMedida.UNIDADE, UnidadeMedida.UNIDADE.getGrupoConversao());
        assertEquals(GrupoConversaoMedida.REACAO, UnidadeMedida.REACAO.getGrupoConversao());

        assertThrows(
                BusinessRuleException.class,
                () -> ConversorUnidadeMedida.converter(
                        BigDecimal.ONE,
                        UnidadeMedida.UNIDADE,
                        UnidadeMedida.REACAO
                )
        );
    }
}
