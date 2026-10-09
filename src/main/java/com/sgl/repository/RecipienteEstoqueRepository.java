package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sgl.model.RecipienteEstoque;

import jakarta.persistence.LockModeType;

@Repository
public interface RecipienteEstoqueRepository extends JpaRepository<RecipienteEstoque, Long> {

	Optional<RecipienteEstoque> findByPublicId(UUID publicId);

	Optional<RecipienteEstoque> findByPublicIdAndLoteEstoqueCentralUnidadePublicId(UUID publicId, UUID unidadePublicId);

	@Override
	@Query("""
			SELECT recipiente
			FROM RecipienteEstoque recipiente
			WHERE (:#{@tenantProvider.unidadeId} IS NULL
			   OR recipiente.lote.estoqueCentral.unidade.publicId =
			      :#{@tenantProvider.unidadeId})
			ORDER BY recipiente.lote.id ASC,
			         recipiente.sequencial ASC
			""")
	List<RecipienteEstoque> findAll();

	List<RecipienteEstoque> findByLoteIdOrderBySequencialAsc(Long loteId);

	List<RecipienteEstoque> findByLoteEstoqueCentralIdOrderByLoteIdAscSequencialAsc(Long estoqueId);

	boolean existsByCodigoInterno(String codigoInterno);

	boolean existsByLoteIdAndSequencial(Long loteId, Integer sequencial);

	@Query("""
			SELECT COALESCE(MAX(recipiente.sequencial), 0)
			FROM RecipienteEstoque recipiente
			WHERE recipiente.lote.id = :loteId
			""")
	Integer buscarMaiorSequencialPorLote(@Param("loteId") Long loteId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT recipiente
			FROM RecipienteEstoque recipiente
			WHERE recipiente.id = :id
			""")
	Optional<RecipienteEstoque> buscarPorIdComBloqueio(@Param("id") Long id);
	
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
	        SELECT recipiente
	        FROM RecipienteEstoque recipiente
	        WHERE recipiente.lote.id = :loteId
	          AND recipiente.quantidadeDisponivel > 0
	        ORDER BY recipiente.sequencial ASC,
	                 recipiente.id ASC
	        """)
	List<RecipienteEstoque> buscarDisponiveisPorLoteComBloqueio(
	        @Param("loteId") Long loteId
	);
}