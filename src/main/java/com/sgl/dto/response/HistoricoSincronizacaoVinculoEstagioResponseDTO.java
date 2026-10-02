package com.sgl.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.sgl.model.HistoricoSincronizacaoVinculoEstagio;
import com.sgl.model.enums.OrigemSincronizacaoVinculoEstagio;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.model.enums.TipoEventoSincronizacaoVinculoEstagio;

import lombok.Getter;

@Getter
public class HistoricoSincronizacaoVinculoEstagioResponseDTO {

	private final UUID id;

	private final UUID vinculoId;

	private final TipoEventoSincronizacaoVinculoEstagio tipoEvento;

	private final OrigemSincronizacaoVinculoEstagio origem;

	private final String referenciaEvento;

	private final SituacaoEstagio situacaoAnterior;
	private final SituacaoEstagio situacaoNova;

	private final LocalDate dataFimPrevistaAnterior;
	private final LocalDate dataFimPrevistaNova;

	private final LocalDate dataFimEfetivaAnterior;
	private final LocalDate dataFimEfetivaNova;

	private final LocalDateTime dataHoraOrigem;
	private final LocalDateTime dataHoraSincronizacao;

	public HistoricoSincronizacaoVinculoEstagioResponseDTO(HistoricoSincronizacaoVinculoEstagio entity) {

		this.id = entity.getPublicId();

		this.vinculoId = entity.getVinculoEstagio().getPublicId();

		this.tipoEvento = entity.getTipoEvento();

		this.origem = entity.getOrigem();

		this.referenciaEvento = entity.getReferenciaEvento();

		this.situacaoAnterior = entity.getSituacaoAnterior();

		this.situacaoNova = entity.getSituacaoNova();

		this.dataFimPrevistaAnterior = entity.getDataFimPrevistaAnterior();

		this.dataFimPrevistaNova = entity.getDataFimPrevistaNova();

		this.dataFimEfetivaAnterior = entity.getDataFimEfetivaAnterior();

		this.dataFimEfetivaNova = entity.getDataFimEfetivaNova();

		this.dataHoraOrigem = entity.getDataHoraOrigem();

		this.dataHoraSincronizacao = entity.getDataHoraSincronizacao();
	}
}