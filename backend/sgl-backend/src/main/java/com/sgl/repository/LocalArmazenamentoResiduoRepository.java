package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.LocalArmazenamentoResiduo;

@Repository
public interface LocalArmazenamentoResiduoRepository extends JpaRepository<LocalArmazenamentoResiduo, Long>{
	
	
	Optional<LocalArmazenamentoResiduo> findByPublicId(UUID publicId);
	
	Optional<LocalArmazenamentoResiduo> findByPublicIdAndUnidadePublicId(UUID publicId, UUID unidadePublicId);
	
	List<LocalArmazenamentoResiduo> findByUnidadePublicIdOrderByNomeAsc(UUID unidadePublicId);
	
	List<LocalArmazenamentoResiduo> findByUnidadePublicIdAndAtivoTrueOrderByNomeAsc(UUID unidadePublicId);
	
	List <LocalArmazenamentoResiduo> findByAtivoTrueOrderByNomeAsc();
	
	boolean existsByUnidadeIdAndNomeIgnoreCase(Long unidadeId, String nome);
	
	boolean existsByUnidadeIdAndNomeIgnoreCaseAndIdNot(Long unidadeId, String nome, Long id);

	
}
