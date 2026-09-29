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

@Schema(description = "Dados necessários para cadastrar ou atualizar uma Atividade.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AtividadeRequestDTO {

	@Schema(description = "Identificador público UUID do SCI ao qual a Atividade pertence.", example = "550e8400-e29b-41d4-a716-446655440004", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "O SCI é obrigatório")
	private UUID sciId;

	@Schema(description = "Código SEG institucional da Atividade.", example = "10.25.00.085.00.01.001", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "O código SEG da Atividade é obrigatório")
	private String codigoSeg;

	@Schema(description = "Título da Atividade.", example = "Avaliação de isolados em condições controladas", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "O nome da Atividade é obrigatório")
	private String nome;

	@Schema(description = "Nome do líder ou responsável pela Atividade.", example = "Maria Oliveira")
	private String responsavel;

	@Schema(description = "Data de início da Atividade.", example = "2026-04-01", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "A data de início da Atividade é obrigatória")
	private LocalDate dataInicio;

	@Schema(description = "Data de término da Atividade, quando definida.", example = "2026-07-31")
	private LocalDate dataFim;

	@Schema(description = "Status do ciclo de vida da Atividade.", example = "ATIVO")
	private StatusProjeto status;

	@Schema(description = "Situação de execução da Atividade.", example = "NAO_INFORMADO")
	private SituacaoExecucaoProjeto situacaoExecucao;

	@Schema(description = "Indica se a Atividade está ativa tecnicamente.", example = "true")
	private Boolean ativo;
}