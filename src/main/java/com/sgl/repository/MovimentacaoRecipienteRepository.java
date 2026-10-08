package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sgl.model.MovimentacaoRecipiente;

@Repository
public interface MovimentacaoRecipienteRepository extends JpaRepository<MovimentacaoRecipiente, Long> {

	Optional<MovimentacaoRecipiente> findByPublicId(UUID publicId);

	Optional<MovimentacaoRecipiente> findByPublicIdAndMovimentacaoEstoqueEstoqueCentralUnidadePublicId(UUID publicId,
			UUID unidadePublicId);

	@Override
	@Query("""
			SELECT detalhe
			FROM MovimentacaoRecipiente detalhe
			WHERE (:#{@tenantProvider.unidadeId} IS NULL
			   OR detalhe.movimentacaoEstoque.estoqueCentral.unidade.publicId =
			      :#{@tenantProvider.unidadeId})
			ORDER BY detalhe.movimentacaoEstoque.dataMovimentacao DESC,
			         detalhe.id DESC
			""")
	List<MovimentacaoRecipiente> findAll();

	List<MovimentacaoRecipiente> findByMovimentacaoEstoqueIdOrderByIdAsc(Long movimentacaoEstoqueId);

	@Query("""
			SELECT detalhe
			FROM MovimentacaoRecipiente detalhe
			WHERE detalhe.recipienteEstoque.id = :recipienteId
			ORDER BY detalhe.movimentacaoEstoque.dataMovimentacao DESC,
			         detalhe.id DESC
			""")
	List<MovimentacaoRecipiente> buscarHistoricoDoRecipiente(@Param("recipienteId") Long recipienteId);

	boolean existsByMovimentacaoEstoqueIdAndRecipienteEstoqueId(Long movimentacaoEstoqueId, Long recipienteEstoqueId);
}