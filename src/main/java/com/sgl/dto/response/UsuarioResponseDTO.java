package com.sgl.dto.response;

import java.io.Serializable;
import java.util.UUID;

import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Representação de um usuário retornado pela API. A senha nunca é exposta.")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Identificador público UUID do usuário.", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID id;
    @Schema(description = "Nome completo do usuário.", example = "Maria Oliveira")
    private String nome;
    @Schema(description = "E-mail do usuário.", example = "maria.oliveira@ufrb.edu.br")
    private String email;
    @Schema(description = "Perfil de acesso do usuário.", example = "RESPONSAVEL")
    private Perfil perfil;
    @Schema(description = "Identificador público UUID da unidade vinculada.", example = "550e8400-e29b-41d4-a716-446655440002")
    private UUID unidadeId;
    @Schema(description = "Nome da unidade vinculada.", example = "Instituto de Química")
    private String unidadeNome;
    @Schema(description = "Sigla da unidade vinculada.", example = "IB")
    private String unidadeSigla;
    @Schema(description = "Identificador público UUID do laboratório vinculado, quando aplicável.", example = "550e8400-e29b-41d4-a716-446655440003")
    private UUID laboratorioId;
    @Schema(description = "Nome do laboratório vinculado, quando aplicável.", example = "Laboratório de Química Orgânica")
    private String laboratorioNome;
    @Schema(description = "Indica se o usuário está ativo.", example = "true")
    private Boolean ativo;

    public UsuarioResponseDTO(Usuario entity) {
        this.id = entity.getPublicId();
        this.nome = entity.getNome();
        this.email = entity.getEmail();
        this.perfil = entity.getPerfil();
        this.unidadeId = entity.getUnidade() != null ? entity.getUnidade().getPublicId() : null;
        this.unidadeNome = entity.getUnidade() != null ? entity.getUnidade().getNome() : null;
        this.unidadeSigla = entity.getUnidade() != null ? entity.getUnidade().getSigla() : null;
        this.laboratorioId = entity.getLaboratorio() != null ? entity.getLaboratorio().getPublicId() : null;
        this.laboratorioNome = entity.getLaboratorio() != null ? entity.getLaboratorio().getNome() : null;
        this.ativo = entity.getAtivo();
    }
}
