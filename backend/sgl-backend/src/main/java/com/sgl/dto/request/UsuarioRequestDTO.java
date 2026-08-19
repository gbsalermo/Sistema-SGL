package com.sgl.dto.request;

import java.io.Serializable;
import java.util.UUID;

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
public class UsuarioRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "nome é obrigatório")
    private String nome;

    @NotBlank(message = "email é obrigatório")
    @Email(message = "email inválido")
    private String email;

    private String senha;

    @NotNull(message = "perfil é obrigatório")
    private Perfil perfil;

    @NotNull(message = "Id da unidade é obrigatório")
    private UUID unidadeId;

    private UUID laboratorioId;

    private Boolean ativo;
}
