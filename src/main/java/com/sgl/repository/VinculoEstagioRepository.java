package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sgl.model.VinculoEstagio;
import com.sgl.model.enums.SituacaoEstagio;

import jakarta.persistence.LockModeType;

@Repository
public interface VinculoEstagioRepository extends JpaRepository<VinculoEstagio, Long> {

	Optional<VinculoEstagio> findByPublicIdAndEstagiarioUnidadePublicId(UUID publicId, UUID unidadePublicId);

	List<VinculoEstagio> findByEstagiarioPublicIdAndEstagiarioUnidadePublicIdOrderByDataInicioDesc(
			UUID estagiarioPublicId, UUID unidadePublicId);

	boolean existsByEstagiarioIdAndSituacaoNot(Long estagiarioId, SituacaoEstagio situacao);

	//Bloqueia que duas sincronizações do mesmo vinculo altere datas/situação ao mesmo tempo
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT v
			FROM VinculoEstagio v
			WHERE v.publicId = :vinculoId
			  AND v.estagiario.unidade.publicId = :unidadeId
			""")
	Optional<VinculoEstagio> buscarPorPublicIdETenantComBloqueio(@Param("vinculoId") UUID vinculoId,
			@Param("unidadeId") UUID unidadeId);
}