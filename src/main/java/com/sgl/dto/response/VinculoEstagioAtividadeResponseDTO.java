package com.sgl.dto.response;

import java.time.LocalDate;
import java.util.UUID;

import com.sgl.model.Atividade;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.VinculoEstagioAtividade;

import lombok.Getter;

@Getter
public class VinculoEstagioAtividadeResponseDTO {

	private final UUID id;
	private final UUID vinculoEstagioId;

	private final UUID atividadeId;
	private final String atividadeNome;
	private final String atividadeCodigoSeg;

	private final UUID sciId;
	private final String sciNome;

	private final UUID projetoId;
	private final String projetoNome;

	private final UUID laboratorioId;
	private final String laboratorioNome;

	private final LocalDate dataInicioParticipacao;
	private final LocalDate dataFimParticipacao;

	private final String observacao;
	private final Boolean ativa;

	public VinculoEstagioAtividadeResponseDTO(VinculoEstagioAtividade entity) {

		Atividade atividade = entity.getAtividade();

		Sci sci = atividade != null ? atividade.getSci() : null;

		Projeto projeto = sci != null ? sci.getProjeto() : null;

		Laboratorio laboratorio = projeto != null ? projeto.getLaboratorio() : null;

		this.id = entity.getPublicId();

		this.vinculoEstagioId = entity.getVinculoEstagio() != null ? entity.getVinculoEstagio().getPublicId() : null;

		this.atividadeId = atividade != null ? atividade.getPublicId() : null;

		this.atividadeNome = atividade != null ? atividade.getNome() : null;

		this.atividadeCodigoSeg = atividade != null ? atividade.getCodigoSeg() : null;

		this.sciId = sci != null ? sci.getPublicId() : null;

		this.sciNome = sci != null ? sci.getNome() : null;

		this.projetoId = projeto != null ? projeto.getPublicId() : null;

		this.projetoNome = projeto != null ? projeto.getNome() : null;

		this.laboratorioId = laboratorio != null ? laboratorio.getPublicId() : null;

		this.laboratorioNome = laboratorio != null ? laboratorio.getNome() : null;

		this.dataInicioParticipacao = entity.getDataInicioParticipacao();

		this.dataFimParticipacao = entity.getDataFimParticipacao();

		this.observacao = entity.getObservacao();

		this.ativa = entity.getDataFimParticipacao() == null;
	}
}