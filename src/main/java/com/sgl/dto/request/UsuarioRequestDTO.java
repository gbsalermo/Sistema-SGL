package com.sgl.dto.request;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sgl.model.enums.CategoriaCargo;
import com.sgl.model.enums.PapelSistema;
import com.sgl.model.enums.Perfil;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Dados necessários para cadastrar ou atualizar um usuário.")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Nome completo do usuário.", example = "Maria Oliveira", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "nome é obrigatório")
    private String nome;

    @Schema(description = "E-mail do usuário.", example = "maria.oliveira@ufrb.edu.br", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "email é obrigatório")
    @Email(message = "email inválido")
    private String email;

    @Schema(description = "Senha usada apenas na autenticação local enquanto esse mecanismo estiver ativo.", example = "SenhaForte123!", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String senha;

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

    @Schema(description = "Identificador público UUID da unidade vinculada ao usuário.", example = "550e8400-e29b-41d4-a716-446655440002", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Id da unidade é obrigatório")
    private UUID unidadeId;

    @Schema(description = "Identificadores públicos UUID dos laboratórios da pessoa. Obrigatório para quem não tem papel de gestão.")
    private List<@NotNull(message = "Laboratório inválido na lista") UUID> laboratorioIds;

    @Schema(description = "Indica se o usuário está ativo.", example = "true")
    private Boolean ativo;
}
