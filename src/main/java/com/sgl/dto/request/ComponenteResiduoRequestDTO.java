package com.sgl.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Componente informado na composição de um resíduo. O vínculo com Produto é opcional e nunca movimenta estoque.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComponenteResiduoRequestDTO {

    @Schema(description = "UUID de um produto do catálogo, quando existir correspondência. É apenas referência de composição.")
    private UUID produtoId;

    @Schema(description = "Nome livre do componente. Pode ser usado quando o material não existir no catálogo.", example = "Acetona")
    private String nomeComponente;

    @Schema(description = "Indica se o componente deve aparecer como principal no rótulo.", example = "true")
    private Boolean principal;

    @Schema(description = "Concentração ou quantidade conhecida, em formato livre.", example = "aprox. 70%")
    private String concentracaoOuQuantidade;

    @Schema(description = "Observação complementar sobre o componente.")
    private String observacao;

    @AssertTrue(message = "Informe produtoId ou nomeComponente para identificar o componente do resíduo")
    @Schema(hidden = true)
    public boolean isIdentificado() {
        return produtoId != null || (nomeComponente != null && !nomeComponente.isBlank());
    }
}
