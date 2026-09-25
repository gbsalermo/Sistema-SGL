package com.sgl.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Dados necessários para corrigir administrativamente um Código SEG.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CorrecaoCodigoSegRequestDTO {

	@Schema(
			description = "UUID do usuário responsável pela correção. Campo temporário enquanto o backend ainda não possui autenticação real.",
			example = "550e8400-e29b-41d4-a716-446655440000",
			requiredMode = Schema.RequiredMode.REQUIRED
	)
	@NotNull(message = "O usuário responsável pela correção é obrigatório")
	private UUID usuarioId;

	@Schema(
			description = "Novo Código SEG institucional.",
			example = "10.25.00.085.00.01",
			requiredMode = Schema.RequiredMode.REQUIRED
	)
	@NotBlank(message = "O novo Código SEG é obrigatório")
	@Size(max = 22, message = "O Código SEG deve possuir no máximo 22 caracteres")
	private String novoCodigoSeg;

	@Schema(
			description = "Justificativa obrigatória para a correção administrativa.",
			example = "Correção de erro de digitação identificado no cadastro institucional.",
			requiredMode = Schema.RequiredMode.REQUIRED
	)
	@NotBlank(message = "A justificativa da correção é obrigatória")
	@Size(max = 1000, message = "A justificativa deve possuir no máximo 1000 caracteres")
	private String justificativa;
}
