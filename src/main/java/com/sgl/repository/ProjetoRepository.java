package com.sgl.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sgl.model.Projeto;
import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;

@Repository
public interface ProjetoRepository extends JpaRepository<Projeto, Long> {

	Optional<Projeto> findByPublicId(UUID publicId);

	Optional<Projeto> findByPublicIdAndLaboratorioUnidadePublicId(UUID publicId, UUID unidadePublicId);

	@Override
	@Query("""
			SELECT projeto
			FROM Projeto projeto
			WHERE (:#{@tenantProvider.unidadeId} IS NULL
			   OR projeto.laboratorio.unidade.publicId = :#{@tenantProvider.unidadeId})
			""")
	List<Projeto> findAll();

	List<Projeto> findByLaboratorioId(Long laboratorioId);

	List<Projeto> findByLaboratorioUnidadePublicId(UUID unidadePublicId);

	List<Projeto> findByAtivoTrue();

	List<Projeto> findByLaboratorioUnidadePublicIdAndAtivoTrue(UUID unidadePublicId);

	boolean existsByCodigoSeg(String codigoSeg);

	boolean existsByCodigoSegAndPublicIdNot(String codigoSeg, UUID publicId);

	//Query de relatorio de projetos
	@Query("""
			SELECT p
			FROM Projeto p
			WHERE p.laboratorio.unidade.publicId = :unidadeId
			  AND (:laboratorioId IS NULL
			       OR p.laboratorio.publicId = :laboratorioId)
			  AND (:status IS NULL
			       OR p.status = :status)
			  AND (:situacaoExecucao IS NULL
			       OR p.situacaoExecucao = :situacaoExecucao)
			  AND (:ativo IS NULL
			       OR p.ativo = :ativo)
			  AND (:recursoExterno IS NULL
			       OR p.possuiRecursoExterno = :recursoExterno)
			  AND (:dataFim IS NULL
			       OR (p.dataInicio IS NOT NULL
			           AND p.dataInicio <= :dataFim))
			  AND (:dataInicio IS NULL
			       OR p.dataFim IS NULL
			       OR p.dataFim >= :dataInicio)
			ORDER BY p.nome ASC, p.id ASC
			""")
	List<Projeto> buscarParaRelatorio(@Param("unidadeId") UUID unidadeId, @Param("laboratorioId") UUID laboratorioId,
			@Param("status") StatusProjeto status, @Param("situacaoExecucao") SituacaoExecucaoProjeto situacaoExecucao,
			@Param("ativo") Boolean ativo, @Param("recursoExterno") Boolean recursoExterno,
			@Param("dataInicio") LocalDate dataInicio, @Param("dataFim") LocalDate dataFim);
}
