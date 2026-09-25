package com.sgl.dto.response;

import java.time.LocalDate;
import java.util.UUID;

import com.sgl.model.Atividade;
import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "Representação de uma Atividade retornada pela API.")
@Getter
public class AtividadeResponseDTO {

	@Schema(description = "Identificador público UUID da Atividade.")
	private final UUID id;

	@Schema(description = "Identificador público UUID do SCI ao qual a Atividade pertence.")
	private final UUID sciId;

	@Schema(description = "Nome do SCI ao qual a Atividade pertence.")
	private final String sciNome;

	@Schema(description = "Código SEG do SCI pai.")
	private final String sciCodigoSeg;

	@Schema(description = "Identificador público UUID do Projeto ao qual a Atividade pertence.")
	private final UUID projetoId;

	@Schema(description = "Nome do Projeto ao qual a Atividade pertence.")
	private final String projetoNome;

	@Schema(description = "Código SEG do Projeto.")
	private final String projetoCodigoSeg;

	@Schema(description = "Código SEG institucional da Atividade.", example = "10.25.00.085.00.01.001")
	private final String codigoSeg;

	@Schema(description = "Título da Atividade.")
	private final String nome;

	@Schema(description = "Nome do líder ou responsável pela Atividade.")
	private final String responsavel;

	@Schema(description = "Data de início da Atividade.")
	private final LocalDate dataInicio;

	@Schema(description = "Data de término da Atividade, quando definida.")
	private final LocalDate dataFim;

	@Schema(description = "Status do ciclo de vida da Atividade.")
	private final StatusProjeto status;

	@Schema(description = "Situação de execução da Atividade.")
	private final SituacaoExecucaoProjeto situacaoExecucao;

	@Schema(description = "Indica se a Atividade está ativa tecnicamente.")
	private final Boolean ativo;

	public AtividadeResponseDTO(Atividade entity) {

		this.id = entity.getPublicId();

		this.sciId = entity.getSci() != null ? entity.getSci().getPublicId() : null;

		this.sciNome = entity.getSci() != null ? entity.getSci().getNome() : null;

		this.sciCodigoSeg = entity.getSci() != null ? entity.getSci().getCodigoSeg() : null;

		this.projetoId = entity.getSci() != null && entity.getSci().getProjeto() != null
				? entity.getSci().getProjeto().getPublicId()
				: null;

		this.projetoNome = entity.getSci() != null && entity.getSci().getProjeto() != null
				? entity.getSci().getProjeto().getNome()
				: null;

		this.projetoCodigoSeg = entity.getSci() != null && entity.getSci().getProjeto() != null
				? entity.getSci().getProjeto().getCodigoSeg()
				: null;

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