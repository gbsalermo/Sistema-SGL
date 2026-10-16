package com.sgl.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CancelarSolucaoRequestDTO {
    private Boolean preparada;
    @NotBlank private String justificativa;
}
