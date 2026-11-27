package com.sgl.dto.request;

import java.util.List;
import java.util.UUID;

import com.sgl.corporate.TipoPessoaCorporativa;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sgl.model.enums.CategoriaCargo;
import com.sgl.model.enums.PapelSistema;
import com.sgl.model.enums.Perfil;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Pessoa corporativa a importar. O CPF nunca trafega: o servidor o busca de novo pela referência.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImportarPessoaRequestDTO {

    @Schema(description = "Base corporativa de origem.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Tipo é obrigatório")
    private TipoPessoaCorporativa tipo;

    @Schema(description = "Referência devolvida pela busca: matrícula (empregado) ou ColaboradorId (colaborador).",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Referência é obrigatória")
    private String referencia;

    @Schema(description = "Papéis do sistema (acumuláveis). Lista vazia: nenhum papel.", example = "[\"GESTOR\"]")
    private List<@NotNull(message = "Papel inválido na lista") PapelSistema> papeis;

    @Schema(description = "Categoria do cargo. Nula mantém a atual (ou a sugerida pelo cargo corporativo).", example = "PESQUISADOR")
    private CategoriaCargo categoriaCargo;

    /**
     * Fills roles and category from a former single profile, for tests written before roles.
     *
     * @deprecated set {@code papeis} and {@code categoriaCargo}.
     */
    @Deprecated
    @JsonIgnore
    public void setPerfil(Perfil perfil) {
        this.papeis = perfil.papel() == null ? List.of() : List.of(perfil.papel());
        this.categoriaCargo = perfil.categoria();
    }

    @Schema(description = "UUIDs dos laboratórios da pessoa, que devem pertencer à Unidade em operação. "
            + "Obrigatório para quem não tem papel de gestão.")
    private List<@NotNull(message = "Laboratório inválido na lista") UUID> laboratorioIds;
}
