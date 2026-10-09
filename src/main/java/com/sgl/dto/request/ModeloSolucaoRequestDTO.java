package com.sgl.dto.request;

import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ModeloSolucaoRequestDTO {
    @NotNull private UUID unidadeId;
    @NotBlank @Size(max=150) private String nome;
    @Size(max=1000) private String descricao;
    @Size(max=2000) private String instrucoesPreparo;
    @Valid @NotEmpty private List<ComponenteModeloSolucaoRequestDTO> componentes;
    private Boolean ativo;
}

