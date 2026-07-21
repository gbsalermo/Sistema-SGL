package com.sgl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.Produto;
import com.sgl.model.enums.NivelRisco;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long>{
	
	List<Produto> findByRisco(NivelRisco risco);
	
	List<Produto> findByPerecivelTrue();
	
	List<Produto> findByNomeContainingIgnoreCase(String nome);

}
