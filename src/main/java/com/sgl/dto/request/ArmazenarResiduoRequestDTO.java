package com.sgl.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Confirmação do armazenamento temporário do resíduo já conferido e rotulado.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArmazenarResiduoRequestDTO {

	@NotNull(message = "O usuário gestor é obrigatório")
	private UUID usuarioGestorId;

	@Schema(description = "UUID de um local cadastrado caso o local físico seja corrigido")
	private UUID localArmazenamentoResiduoId;

	@Size(max = 150, message = "O complemento do local deve possuir no máximo 150 caracteres")
	@Schema(description = "Complemento do local cadastrado.", example = "Estante A1")
	private String complementoLocalArmazenamento;

	@Size(max = 255, message = "O local de armazenamento manual deve possuir no máximo 255 caracteres")
	@Schema(description = "Local manual caso seja necessária uma correção física")
	private String localArmazenamentoTemporario;	
}
