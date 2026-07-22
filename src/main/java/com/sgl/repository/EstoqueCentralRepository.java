package com.sgl.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.EstoqueCentral;

@Repository
public interface EstoqueCentralRepository extends JpaRepository<EstoqueCentral, Long> {
	
	Optional<EstoqueCentral> findByProdutoId(Long produtoId);
	
	List<EstoqueCentral> findByQuantidadeAtualLessThanEqual( Integer quantidade);
	
	List<EstoqueCentral> findByAtivoTrue();
	

}
