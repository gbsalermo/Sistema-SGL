package com.sgl.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import com.sgl.model.enums.UnidadeMedida;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SolucaoComponenteRequestDTO {

    @NotNull(message = "Produto do componente é obrigatório")
    private UUID produtoId;

    @NotNull(message = "Quantidade do componente é obrigatória")
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantidade do componente deve ser maior que zero")
    @Digits(integer = 13, fraction = 6, message = "Quantidade do componente deve possuir no máximo 6 casas decimais")
    private BigDecimal quantidade;

    @NotNull(message = "Unidade de medida do componente é obrigatória")
    private UnidadeMedida unidadeMedida;
}
