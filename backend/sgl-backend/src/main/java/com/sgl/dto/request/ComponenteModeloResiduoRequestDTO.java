package com.sgl.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComponenteModeloResiduoRequestDTO {

    private UUID produtoId;

    @Size(
            max = 255,
            message = "O nome do componente deve possuir no máximo 255 caracteres"
    )
    private String nomeComponente;

    private Boolean principal;

    @Size(
            max = 100,
            message = "A concentração ou quantidade deve possuir no máximo 100 caracteres"
    )
    private String concentracaoOuQuantidade;

    @Size(
            max = 500,
            message = "A observação do componente deve possuir no máximo 500 caracteres"
    )
    private String observacao;

    @AssertTrue(
            message = "Informe produtoId ou nomeComponente para identificar o componente"
    )
    public boolean isIdentificado() {

        return produtoId != null
                || (nomeComponente != null
                && !nomeComponente.isBlank());
    }
}