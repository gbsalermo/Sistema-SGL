package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.Atividade;

@Repository
public interface AtividadeRepository extends JpaRepository<Atividade, Long> {

	Optional<Atividade> findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(UUID publicId, UUID unidadePublicId);

	List<Atividade> findBySciProjetoLaboratorioUnidadePublicId(UUID unidadePublicId);

	List<Atividade> findBySciProjetoLaboratorioUnidadePublicIdAndAtivoTrue(UUID unidadePublicId);

	List<Atividade> findBySciPublicIdAndSciProjetoLaboratorioUnidadePublicId(UUID sciPublicId, UUID unidadePublicId);

	List<Atividade> findBySciPublicIdAndSciProjetoLaboratorioUnidadePublicIdAndAtivoTrue(UUID sciPublicId,
			UUID unidadePublicId);

	List<Atividade> findBySciProjetoPublicIdAndSciProjetoLaboratorioUnidadePublicId(UUID projetoPublicId,
			UUID unidadePublicId);
}