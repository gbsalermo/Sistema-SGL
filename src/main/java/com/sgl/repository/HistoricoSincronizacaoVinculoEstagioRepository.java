package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.HistoricoSincronizacaoVinculoEstagio;
import com.sgl.model.enums.OrigemSincronizacaoVinculoEstagio;

@Repository
public interface HistoricoSincronizacaoVinculoEstagioRepository
		extends JpaRepository<HistoricoSincronizacaoVinculoEstagio, Long> {

	List<HistoricoSincronizacaoVinculoEstagio> findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataHoraSincronizacaoAsc(
			UUID vinculoId, UUID unidadeId);

	boolean existsByOrigemAndReferenciaEvento(OrigemSincronizacaoVinculoEstagio origem, String referenciaEvento);

	Optional<HistoricoSincronizacaoVinculoEstagio> findByOrigemAndReferenciaEvento(
			OrigemSincronizacaoVinculoEstagio origem, String referenciaEvento);

	/**
	 * Esse método é útil para verificar se determinado evento de sincronização já
	 * foi processado dentro da unidade correta, evitando duplicidade ou associação
	 * de um histórico a outra unidade.
	 */
	Optional<HistoricoSincronizacaoVinculoEstagio> findByOrigemAndReferenciaEventoAndVinculoEstagioEstagiarioUnidadePublicId(
			OrigemSincronizacaoVinculoEstagio origem, String referenciaEvento, UUID unidadeId);
}