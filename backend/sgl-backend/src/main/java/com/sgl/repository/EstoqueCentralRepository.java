package com.sgl.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.EstoqueCentral;

@Repository
public interface EstoqueCentralRepository extends JpaRepository<EstoqueCentral, Long> {
	
	Optional<EstoqueCentral> findByUnidadeIdAndProdutoId(
	        Long unidadeId,
	        Long produtoId
	);

	boolean existsByUnidadeIdAndProdutoId(
	        Long unidadeId,
	        Long produtoId
	);

	List<EstoqueCentral> findByUnidadeId(Long unidadeId);

	List<EstoqueCentral> findByUnidadeIdAndAtivoTrue(Long unidadeId);
	
	List<EstoqueCentral> findByAtivoTrue();
	
}
