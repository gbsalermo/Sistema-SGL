package com.sgl.dto.request;

import java.io.Serializable;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LaboratorioRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "unidadeId é obrigatório")
    private UUID unidadeId;

    @NotBlank(message = "nome é obrigatório")
    private String nome;

    private String descricao;
    private UUID responsavelId;
    private Boolean ativo;
}
