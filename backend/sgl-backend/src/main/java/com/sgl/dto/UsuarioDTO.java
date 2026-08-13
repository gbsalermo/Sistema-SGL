package com.sgl.dto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "nome é obrigatório")
    private String nome;

    @NotBlank(message = "email é obrigatório")
    @Email(message = "email inválido")
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String senha;

    @NotNull(message = "perfil é obrigatório")
    private Perfil perfil;

    @NotNull(message = "Id da unidade é obrigatório")
    private Long unidadeId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String unidadeNome;

    private Long laboratorioId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String laboratorioNome;

    private Boolean ativo = true;

    public UsuarioDTO(Usuario entity) {
        this.id = entity.getId();
        this.nome = entity.getNome();
        this.email = entity.getEmail();
        this.perfil = entity.getPerfil();
        this.unidadeId = entity.getUnidade() != null ? entity.getUnidade().getId() : null;
        this.unidadeNome = entity.getUnidade() != null ? entity.getUnidade().getNome() : null;
        this.laboratorioId = entity.getLaboratorio() != null ? entity.getLaboratorio().getId() : null;
        this.laboratorioNome = entity.getLaboratorio() != null ? entity.getLaboratorio().getNome() : null;
        this.ativo = entity.getAtivo();
    }
}
