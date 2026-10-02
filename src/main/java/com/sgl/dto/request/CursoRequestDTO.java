package com.sgl.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CursoRequestDTO {

    @NotNull(message = "Unidade é obrigatória")
    private UUID unidadeId;

    @NotBlank(message = "Nome do curso é obrigatório")
    private String nome;

    private Boolean ativo;
}