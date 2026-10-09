package com.sgl.dto.request;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sgl.model.enums.UnidadeMedida;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SolucaoRequestDTO {

    @NotNull(message = "Unidade é obrigatória")
    private UUID unidadeId;

    @NotBlank(message = "Nome da solução é obrigatório")
    @Size(max = 160, message = "Nome da solução deve possuir no máximo 160 caracteres")
    private String nome;

    @Size(max = 1000, message = "Descrição deve possuir no máximo 1000 caracteres")
    private String descricao;

    @NotNull(message = "Rendimento da solução é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "Rendimento deve ser maior que zero")
    @Digits(integer = 13, fraction = 6, message = "Rendimento deve possuir no máximo 6 casas decimais")
    private BigDecimal rendimentoQuantidade;

    @NotNull(message = "Unidade do rendimento é obrigatória")
    private UnidadeMedida rendimentoUnidade;

    @Size(max = 160, message = "Concentração deve possuir no máximo 160 caracteres")
    private String concentracao;

    @Size(max = 4000, message = "Instruções de preparo devem possuir no máximo 4000 caracteres")
    private String instrucoesPreparo;

    private Boolean ativo;

    @Valid
    @NotEmpty(message = "Informe ao menos um componente da solução")
    private List<SolucaoComponenteRequestDTO> componentes = new ArrayList<>();
}
