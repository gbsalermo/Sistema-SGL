package com.sgl.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sgl.model.Produto;
import com.sgl.model.enums.NivelRisco;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long>{
	
	List<Produto> findByRisco(NivelRisco risco);
	
	List<Produto> findByPerecivelTrue();
	
	List<Produto> findByNomeContainingIgnoreCase(String nome);
	
	//Buscar produtos perecíveis com validade próxima
	@Query("SELECT p FROM Produto p WHERE p.perecivel = true AND p.dataValidade BETWEEN :dataAtual And :dataLimite")
	List<Produto> findPereciveisComValidadeProxima(@Param("dataAtual")
	LocalDate dataAtual, @Param("dataLimite") LocalDate dataLimite);

	boolean existsByCodigoReferencia(String codigoReferencia);
	
	boolean existsByCodigoReferenciaAndIdNot(
	        String codigoReferencia,
	        Long id
	);
}
