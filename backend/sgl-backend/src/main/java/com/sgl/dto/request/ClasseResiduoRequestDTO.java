package com.sgl.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClasseResiduoRequestDTO {

    @NotNull(message = "A unidade é obrigatória")
    private UUID unidadeId;

    @NotBlank(message = "O código da classe é obrigatório")
    private String codigo;

    @NotBlank(message = "A descrição da classe é obrigatória")
    private String descricao;

    private Boolean ativo;
}