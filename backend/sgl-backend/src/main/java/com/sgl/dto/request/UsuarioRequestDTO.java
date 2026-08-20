package com.sgl.dto.request;

import java.io.Serializable;
import java.util.UUID;

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

    @Schema(description = "Perfil de acesso do usuário.", example = "RESPONSAVEL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "perfil é obrigatório")
    private Perfil perfil;

    @Schema(description = "Identificador público UUID da unidade vinculada ao usuário.", example = "550e8400-e29b-41d4-a716-446655440002", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Id da unidade é obrigatório")
    private UUID unidadeId;

    @Schema(description = "Identificador público UUID do laboratório vinculado ao usuário, quando aplicável.", example = "550e8400-e29b-41d4-a716-446655440003")
    private UUID laboratorioId;

    @Schema(description = "Indica se o usuário está ativo.", example = "true")
    private Boolean ativo;
}
