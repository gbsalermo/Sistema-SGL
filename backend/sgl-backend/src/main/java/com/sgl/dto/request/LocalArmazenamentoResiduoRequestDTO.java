package com.sgl.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocalArmazenamentoResiduoRequestDTO {
	
	@NotNull(message = "A unidade é obrigatória")
	private UUID unidadeId;
	
	@NotBlank(message = "O nome do local de armazenamento é obrigatório")
	@Size(max = 150, message = "O nome do local de armazenamento deve possuir no máximo 150 caracteres")
	private String nome;
	
	private Boolean ativo;

}
