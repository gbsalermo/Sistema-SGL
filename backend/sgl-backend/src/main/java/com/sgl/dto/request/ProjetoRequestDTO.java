package com.sgl.dto.request;

import java.time.LocalDate;
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
public class ProjetoRequestDTO {

    @NotNull(message = "Id do laboratório é obrigatorio")
    private UUID laboratorioId;

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    private String descricao;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String responsavel;
    private Boolean ativo;
}
