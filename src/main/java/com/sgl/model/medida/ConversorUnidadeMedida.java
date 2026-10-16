package com.sgl.model.medida;

import java.math.BigDecimal;
import java.math.MathContext;

import com.sgl.exception.BusinessRuleException;
import com.sgl.model.enums.UnidadeMedida;

/**
 * Converte quantidades apenas entre unidades pertencentes ao mesmo grupo
 * explícito de conversão.
 *
 * A unidade canônica é definida por Produto; esta classe não escolhe a
 * unidade do Produto, apenas executa a conversão solicitada.
 */
public final class ConversorUnidadeMedida {

    private static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;

    private ConversorUnidadeMedida() {
    }

    public static BigDecimal converter(
            BigDecimal quantidade,
            UnidadeMedida origem,
            UnidadeMedida destino) {

        if (quantidade == null) {
            throw new BusinessRuleException("A quantidade é obrigatória para conversão.");
        }
        if (origem == null || destino == null) {
            throw new BusinessRuleException("As unidades de origem e destino são obrigatórias.");
        }
        if (!origem.compativelCom(destino)) {
            throw new BusinessRuleException(
                    "Não é possível converter " + origem + " para " + destino
                            + ": unidades pertencem a grupos de conversão diferentes."
            );
        }
        if (origem == destino) {
            return quantidade;
        }

        BigDecimal quantidadeNaBase = quantidade.multiply(
                origem.getFatorParaUnidadeBase(),
                MATH_CONTEXT
        );

        return quantidadeNaBase.divide(
                destino.getFatorParaUnidadeBase(),
                MATH_CONTEXT
        );
    }

    public static BigDecimal converterParaCanonica(
            BigDecimal quantidade,
            UnidadeMedida unidadeInformada,
            UnidadeMedida unidadeCanonica) {
        return converter(quantidade, unidadeInformada, unidadeCanonica);
    }
}
