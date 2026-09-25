package com.sgl.dto.request;

import java.time.LocalDate;
import java.util.UUID;

import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Dados necessários para cadastrar ou atualizar um SCI.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SciRequestDTO {

	@Schema(description = "Identificador público UUID do projeto ao qual o SCI pertence.", example = "550e8400-e29b-41d4-a716-446655440003", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "O projeto é obrigatório")
	private UUID projetoId;

	@Schema(description = "Código SEG institucional do SCI.", example = "10.25.00.085.00.01", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "O código SEG do SCI é obrigatório")
	private String codigoSeg;

	@Schema(description = "Título do SCI.", example = "Desenvolvimento de solução para controle biológico", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "O nome do SCI é obrigatório")
	private String nome;

	@Schema(description = "Nome do líder ou responsável pelo SCI.", example = "Maria Oliveira")
	private String responsavel;

	@Schema(description = "Data de início do SCI.", example = "2026-03-01", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "A data de início do SCI é obrigatória")
	private LocalDate dataInicio;

	@Schema(description = "Data de término do SCI, quando definida.", example = "2026-08-31")
	private LocalDate dataFim;

	@Schema(description = "Status do ciclo de vida do SCI.", example = "ATIVO")
	private StatusProjeto status;

	@Schema(description = "Situação de execução do SCI.", example = "NAO_INFORMADO")
	private SituacaoExecucaoProjeto situacaoExecucao;

	@Schema(description = "Indica se o SCI está ativo tecnicamente.", example = "true")
	private Boolean ativo;
}