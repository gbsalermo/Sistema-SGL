
package com.sgl.dto.response;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;

public record RelatorioProjetosResponseDTO(LocalDateTime geradoEm, long totalProjetos, long totalScis,
		long totalAtividades, long totalParticipacoes, long totalEstagiariosDistintos, List<ProjetoItem> projetos) {

	public record ProjetoItem(UUID id, String codigoSeg, String nome, UUID laboratorioId, String laboratorioNome,
			String responsavel, LocalDate dataInicio, LocalDate dataFim, StatusProjeto status,
			SituacaoExecucaoProjeto situacaoExecucao, Boolean ativo, Boolean possuiRecursoExterno,
			String empresaRecursoExterno, long totalScis, long totalAtividades, long totalParticipacoes,
			long totalEstagiarios) {
	}
}
