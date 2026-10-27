package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sgl.model.Atividade;

@Repository
public interface AtividadeRepository extends JpaRepository<Atividade, Long> {

	Optional<Atividade> findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(UUID publicId, UUID unidadePublicId);

	List<Atividade> findBySciProjetoLaboratorioUnidadePublicId(UUID unidadePublicId);

	List<Atividade> findBySciProjetoLaboratorioUnidadePublicIdAndAtivoTrue(UUID unidadePublicId);

	List<Atividade> findBySciPublicIdAndSciProjetoLaboratorioUnidadePublicId(UUID sciPublicId, UUID unidadePublicId);

	List<Atividade> findBySciPublicIdAndSciProjetoLaboratorioUnidadePublicIdAndAtivoTrue(UUID sciPublicId,
			UUID unidadePublicId);

	List<Atividade> findBySciProjetoPublicIdAndSciProjetoLaboratorioUnidadePublicId(UUID projetoPublicId,
			UUID unidadePublicId);

	boolean existsByCodigoSeg(String codigoSeg);

	boolean existsByCodigoSegAndPublicIdNot(String codigoSeg, UUID publicId);
	
	interface TotalAtividadesPorProjeto {
	    UUID getProjetoId();
	    Long getTotalAtividades();
	}
	
	@Query("""
		    SELECT
		        a.sci.projeto.publicId AS projetoId,
		        COUNT(a.id) AS totalAtividades
		    FROM Atividade a
		    WHERE a.sci.projeto.laboratorio.unidade.publicId = :unidadeId
		      AND a.sci.projeto.publicId IN :projetoIds
		    GROUP BY a.sci.projeto.publicId
		    """)
		List<TotalAtividadesPorProjeto> contarAtividadesPorProjetos(
		        @Param("unidadeId") UUID unidadeId,
		        @Param("projetoIds") List<UUID> projetoIds
		);
	
}