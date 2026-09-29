package com.sgl.dto.response;

import java.time.LocalDate;
import java.util.UUID;

import com.sgl.model.Sci;
import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "Representação de um SCI retornado pela API.")
@Getter
public class SciResponseDTO {

	@Schema(description = "Identificador público UUID do SCI.", example = "550e8400-e29b-41d4-a716-446655440004")
	private final UUID id;

	@Schema(description = "Identificador público UUID do projeto ao qual o SCI pertence.")
	private final UUID projetoId;

	@Schema(description = "Nome do projeto ao qual o SCI pertence.")
	private final String projetoNome;

	@Schema(description = "Código SEG do projeto pai.")
	private final String projetoCodigoSeg;

	@Schema(description = "Código SEG institucional do SCI.", example = "10.25.00.085.00.01")
	private final String codigoSeg;

	@Schema(description = "Título do SCI.")
	private final String nome;

	@Schema(description = "Nome do líder ou responsável pelo SCI.")
	private final String responsavel;

	@Schema(description = "Data de início do SCI.")
	private final LocalDate dataInicio;

	@Schema(description = "Data de término do SCI, quando definida.")
	private final LocalDate dataFim;

	@Schema(description = "Status do ciclo de vida do SCI.")
	private final StatusProjeto status;

	@Schema(description = "Situação de execução do SCI.")
	private final SituacaoExecucaoProjeto situacaoExecucao;

	@Schema(description = "Indica se o SCI está ativo tecnicamente.")
	private final Boolean ativo;

	public SciResponseDTO(Sci entity) {

		this.id = entity.getPublicId();

		this.projetoId = entity.getProjeto() != null ? entity.getProjeto().getPublicId() : null;

		this.projetoNome = entity.getProjeto() != null ? entity.getProjeto().getNome() : null;

		this.projetoCodigoSeg = entity.getProjeto() != null ? entity.getProjeto().getCodigoSeg() : null;

		this.codigoSeg = entity.getCodigoSeg();
		this.nome = entity.getNome();
		this.responsavel = entity.getResponsavel();
		this.dataInicio = entity.getDataInicio();
		this.dataFim = entity.getDataFim();
		this.status = entity.getStatus();
		this.situacaoExecucao = entity.getSituacaoExecucao();
		this.ativo = entity.getAtivo();
	}
}