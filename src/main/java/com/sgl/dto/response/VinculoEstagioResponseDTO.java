package com.sgl.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.sgl.model.HistoricoSincronizacaoVinculoEstagio;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.VinculoEstagioAtividade;
import com.sgl.model.VinculoEstagioAtividadeCultura;
import com.sgl.model.enums.FormacaoEstagiario;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.model.enums.TipoBolsa;
import com.sgl.model.enums.TipoEventoSincronizacaoVinculoEstagio;

import lombok.Getter;

@Getter
public class VinculoEstagioResponseDTO {

	private final UUID id;

	private final UUID orientadorId;
	private final String orientadorNome;

	private final LocalDate dataInicio;
	private final LocalDate dataFimPrevista;
	private final LocalDate dataFimPrevistaOriginal;
	private final LocalDate dataFimEfetiva;

	private final TipoBolsa tipoBolsa;
	private final SituacaoEstagio situacao;
	private final List<VinculoEstagioAtividadeResponseDTO> participacoesAtividade;
	private final FormacaoEstagiario formacao;
	private final String formacaoOutro;

	private final UUID cursoId;
	private final String cursoNome;

	private final Boolean treinamentoSegurancaConcluido;

	private final String observacao;
	private final String referenciaInstitucional;

	public VinculoEstagioResponseDTO(VinculoEstagio entity) {

		this(entity, List.of());
	}

	public VinculoEstagioResponseDTO(VinculoEstagio entity, List<VinculoEstagioAtividade> participacoes) {

		this(entity, participacoes, Map.of());
	}

	public VinculoEstagioResponseDTO(VinculoEstagio entity, List<VinculoEstagioAtividade> participacoes,
			Map<UUID, List<VinculoEstagioAtividadeCultura>> culturasPorParticipacao) {

		this(entity, participacoes, culturasPorParticipacao, List.of());
	}

	public VinculoEstagioResponseDTO(VinculoEstagio entity, List<VinculoEstagioAtividade> participacoes,
			Map<UUID, List<VinculoEstagioAtividadeCultura>> culturasPorParticipacao,
			List<HistoricoSincronizacaoVinculoEstagio> historicosSincronizacao) {

		this.id = entity.getPublicId();

		this.orientadorId = entity.getOrientador() != null ? entity.getOrientador().getPublicId() : null;

		this.orientadorNome = entity.getOrientador() != null ? entity.getOrientador().getNome() : null;

		this.dataInicio = entity.getDataInicio();
		this.dataFimPrevista = entity.getDataFimPrevista();

		this.dataFimPrevistaOriginal = historicosSincronizacao.stream()
				.filter(historico -> historico.getTipoEvento() == TipoEventoSincronizacaoVinculoEstagio.PRORROGACAO)
				.map(HistoricoSincronizacaoVinculoEstagio::getDataFimPrevistaAnterior)
				.filter(data -> data != null)
				.findFirst()
				.orElse(null);

		this.dataFimEfetiva = entity.getDataFimEfetiva();

		this.tipoBolsa = entity.getTipoBolsa();
		this.situacao = entity.getSituacao();
		this.observacao = entity.getObservacao();

		this.participacoesAtividade = participacoes.stream()
				.map(participacao -> new VinculoEstagioAtividadeResponseDTO(participacao,
						culturasPorParticipacao.getOrDefault(participacao.getPublicId(), List.of())))
				.toList();

		this.formacao = entity.getFormacao();
		this.formacaoOutro = entity.getFormacaoOutro();

		this.cursoId = entity.getCurso() != null ? entity.getCurso().getPublicId() : null;

		this.cursoNome = entity.getCurso() != null ? entity.getCurso().getNome() : null;

		this.treinamentoSegurancaConcluido = entity.getTreinamentoSegurancaConcluido();
		this.referenciaInstitucional = entity.getReferenciaInstitucional();
	}
}