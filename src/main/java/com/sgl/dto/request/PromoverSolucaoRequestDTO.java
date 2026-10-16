package com.sgl.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PromoverSolucaoRequestDTO {
    @NotBlank @Size(max=150) private String nome;
    @Size(max=1000) private String descricao;
    @Size(max=2000) private String instrucoesPreparo;
}
