package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.VinculoEstagioAtividadeCultura;

@Repository
public interface VinculoEstagioAtividadeCulturaRepository extends JpaRepository<VinculoEstagioAtividadeCultura, Long> {

	Optional<VinculoEstagioAtividadeCultura> findByPublicIdAndParticipacaoVinculoEstagioEstagiarioUnidadePublicId(
			UUID publicId, UUID unidadePublicId);

	List<VinculoEstagioAtividadeCultura> findByParticipacaoPublicIdAndParticipacaoVinculoEstagioEstagiarioUnidadePublicIdOrderByCulturaNomeAsc(
			UUID participacaoPublicId, UUID unidadePublicId);

	boolean existsByParticipacaoIdAndCulturaId(Long participacaoId, Long culturaId);
}