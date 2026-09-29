package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.Sci;

@Repository
public interface SciRepository extends JpaRepository<Sci, Long> {

	Optional<Sci> findByPublicIdAndProjetoLaboratorioUnidadePublicId(UUID publicId, UUID unidadePublicId);

	List<Sci> findByProjetoPublicIdAndProjetoLaboratorioUnidadePublicId(UUID projetoPublicId, UUID unidadePublicId);

	List<Sci> findByProjetoLaboratorioUnidadePublicId(UUID unidadePublicId);

	List<Sci> findByProjetoLaboratorioUnidadePublicIdAndAtivoTrue(UUID unidadePublicId);

	List<Sci> findByProjetoPublicIdAndProjetoLaboratorioUnidadePublicIdAndAtivoTrue(UUID projetoPublicId,
			UUID unidadePublicId);

	boolean existsByCodigoSeg(String codigoSeg);

	boolean existsByCodigoSegAndPublicIdNot(String codigoSeg, UUID publicId);
}