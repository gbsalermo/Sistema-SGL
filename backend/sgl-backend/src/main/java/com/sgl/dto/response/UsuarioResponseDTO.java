package com.sgl.dto.response;

import java.io.Serializable;
import java.util.UUID;

import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private String nome;
    private String email;
    private Perfil perfil;
    private UUID unidadeId;
    private String unidadeNome;
    private UUID laboratorioId;
    private String laboratorioNome;
    private Boolean ativo;

    public UsuarioResponseDTO(Usuario entity) {
        this.id = entity.getPublicId();
        this.nome = entity.getNome();
        this.email = entity.getEmail();
        this.perfil = entity.getPerfil();
        this.unidadeId = entity.getUnidade() != null ? entity.getUnidade().getPublicId() : null;
        this.unidadeNome = entity.getUnidade() != null ? entity.getUnidade().getNome() : null;
        this.laboratorioId = entity.getLaboratorio() != null ? entity.getLaboratorio().getPublicId() : null;
        this.laboratorioNome = entity.getLaboratorio() != null ? entity.getLaboratorio().getNome() : null;
        this.ativo = entity.getAtivo();
    }
}
