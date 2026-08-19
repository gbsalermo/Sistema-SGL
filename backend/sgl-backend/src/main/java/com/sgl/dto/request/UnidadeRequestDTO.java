package com.sgl.dto.request;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnidadeRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotBlank(message = "Sigla é obrigatória")
    private String sigla;
}
