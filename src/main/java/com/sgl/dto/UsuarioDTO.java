package com.sgl.dto;

import java.io.Serializable;

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

	@NotBlank(message = "senha é obrigatória")
	private String senha;

	@NotNull(message = "perfil é obrigatório")
	private Perfil perfil;

	private Long laboratorioId;

	private Boolean ativo = true;

	public UsuarioDTO(Usuario entity) {
		this.id = entity.getId();
		this.nome = entity.getNome();
		this.email = entity.getEmail();
		this.perfil = entity.getPerfil();
		this.laboratorioId = entity.getLaboratorio() != null ? entity.getLaboratorio().getId() : null;
		this.ativo = entity.getAtivo();
	}
}
