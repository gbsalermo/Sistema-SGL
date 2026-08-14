package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sgl.model.EstoqueCentral;

import jakarta.persistence.LockModeType;

@Repository
public interface EstoqueCentralRepository extends JpaRepository<EstoqueCentral, Long> {
	
	
	Optional<EstoqueCentral> findByPublicId(UUID publicId);
	
	
	  /*
     * Busca comum, sem bloqueio.
     *
     * Deve continuar sendo utilizada em operações apenas de consulta,
     * nas quais quantidadeAtual não será modificada.
     */
	Optional<EstoqueCentral> findByUnidadeIdAndProdutoId(Long unidadeId, Long produtoId);

	boolean existsByUnidadeIdAndProdutoId(
	        Long unidadeId,
	        Long produtoId
	);

	 /*
     * Busca o estoque pelo ID aplicando um bloqueio pessimista de escrita.
     *
     * Enquanto a transação que chamou este método não for finalizada,
     * outra transação que tentar bloquear o mesmo estoque deverá aguardar.
     *
     * Esse método deve ser usado apenas quando quantidadeAtual será alterada.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT estoque
            FROM EstoqueCentral estoque
            WHERE estoque.id = :id
            """)
    Optional<EstoqueCentral> buscarPorIdComBloqueio(
            @Param("id") Long id
    );
    
    
    /*
     * Busca o estoque pela combinação Unidade + Produto aplicando bloqueio
     * pessimista de escrita.
     *
     * Esse método será usado principalmente durante aprovação e cancelamento
     * de pedidos.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT estoque
            FROM EstoqueCentral estoque
            WHERE estoque.unidade.id = :unidadeId
              AND estoque.produto.id = :produtoId
            """)
    Optional<EstoqueCentral> buscarPorUnidadeEProdutoComBloqueio(
            @Param("unidadeId") Long unidadeId,
            @Param("produtoId") Long produtoId
    );
    
    
	List<EstoqueCentral> findByUnidadeId(Long unidadeId);

	List<EstoqueCentral> findByUnidadeIdAndAtivoTrue(Long unidadeId);
	
	List<EstoqueCentral> findByAtivoTrue();
	
}
