package com.sgl.dto.request;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.sgl.model.enums.FormacaoEstagiario;
import com.sgl.model.enums.TipoBolsa;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Dados necessários para cadastrar ou atualizar um estagiário.")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstagiarioRequestDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "Identificador público UUID do usuário associado ao estágio.", example = "550e8400-e29b-41d4-a716-446655440001", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Id do usuário é obrigatório")
	private UUID usuarioId;

	@Schema(description = "Identificador público UUID do laboratório do estágio.", example = "550e8400-e29b-41d4-a716-446655440002", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Id do laboratório é obrigatório")
	private UUID laboratorioId;

	@Schema(description = "Data de início do estágio.", example = "2026-08-01", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Data de início do estágio é obrigatória")
	private LocalDate dataInicioEstagio;

	@Schema(description = "Data final prevista do estágio.", example = "2027-01-31", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Data final prevista do estágio é obrigatória")
	private LocalDate dataFimEstagio;

	@Schema(description = "Tipo de vínculo do estágio. O nome técnico do campo permanece tipoBolsa por compatibilidade.", example = "CONTRATUAL", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Tipo de vínculo é obrigatório")
	private TipoBolsa tipoBolsa;

	@Schema(description = "Observação opcional sobre o estágio.", example = "Estágio vinculado ao projeto de síntese.")
	private String observacao;

	@Schema(description = "Indica se o vínculo de estágio está ativo.", example = "true")
	private Boolean ativo;

	@Schema(description = "UUID do usuário orientador do estagiário.", example = "550e8400-e29b-41d4-a716-446655440010", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Orientador é obrigatório")
	private UUID orientadorId;

	@Schema(description = "UUID da Atividade inicial do primeiro vínculo de estágio.", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Atividade inicial é obrigatória")
	private UUID atividadeId;

	@Schema(description = "Formação/categoria institucional do primeiro vínculo.", example = "GRADUACAO", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Formação é obrigatória")
	private FormacaoEstagiario formacao;

	@Schema(description = "Descrição obrigatória quando a formação for OUTRO.")
	private String formacaoOutro;

	@Schema(description = "UUID do curso vinculado ao estágio, quando aplicável.")
	private UUID cursoId;

	@Schema(description = "UUIDs das Culturas associadas à participação inicial em Atividade.")
	private Set<UUID> culturaIds = new LinkedHashSet<>();
}
