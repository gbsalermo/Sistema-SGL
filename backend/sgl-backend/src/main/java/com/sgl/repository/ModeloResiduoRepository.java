package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.ModeloResiduo;

@Repository
public interface ModeloResiduoRepository extends JpaRepository<ModeloResiduo, Long> {

	Optional<ModeloResiduo> findByPublicId(UUID publicId);

	Optional<ModeloResiduo> findByPublicIdAndUnidadePublicId(UUID publicId, UUID unidadePublicId);

	List<ModeloResiduo> findByUnidadePublicIdOrderByNomeAsc(UUID unidadePublicId);

	List<ModeloResiduo> findByUnidadePublicIdAndAtivoTrueOrderByNomeAsc(UUID unidadePublicId);

	List<ModeloResiduo> findByAtivoTrueOrderByNomeAsc();

	boolean existsByUnidadeIdAndNomeIgnoreCase(Long unidadeId, String nome);

	boolean existsByUnidadeIdAndNomeIgnoreCaseAndIdNot(Long unidadeId, String nome, Long id);
}