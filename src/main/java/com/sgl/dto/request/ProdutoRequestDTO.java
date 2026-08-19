package com.sgl.dto.request;

import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.TipoPerecivel;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;

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
public class ProdutoRequestDTO {

    @NotBlank(message = "nome é obrigatório")
    private String nome;

    private String descricao;

    @NotBlank(message = "Codigo de referência é obrigatório")
    private String codigoReferencia;

    @NotNull(message = "Informe a Unidade de medida")
    private UnidadeMedida unidadeMedida;

    private String localizacaoFisica;

    @NotNull(message = "risco é obrigatório")
    private NivelRisco risco;

    private TipoRisco tipoRisco;
    private String descricaoRisco;

    @NotNull(message = "Precisa confirmar se é perecivel")
    private Boolean perecivel;

    private TipoPerecivel tipoPerecivel;
    private String condicoesArmazenamento;
    private String unidadeArmazenamento;
    private Boolean ativo;
}
