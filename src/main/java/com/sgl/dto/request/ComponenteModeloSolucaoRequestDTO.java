package com.sgl.dto.request;

import java.math.BigDecimal;
import java.util.UUID;
import com.sgl.model.enums.UnidadeMedida;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ComponenteModeloSolucaoRequestDTO {
    @NotNull private UUID produtoId;
    @NotNull @DecimalMin(value="0.0", inclusive=false) @Digits(integer=13, fraction=6)
    private BigDecimal quantidade;
    @NotNull private UnidadeMedida unidadeMedida;
}

