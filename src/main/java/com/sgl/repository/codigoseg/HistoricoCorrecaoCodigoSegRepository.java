package com.sgl.repository.codigoseg;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.codigoseg.HistoricoCorrecaoCodigoSeg;

@Repository
public interface HistoricoCorrecaoCodigoSegRepository
		extends JpaRepository<HistoricoCorrecaoCodigoSeg, Long> {

	List<HistoricoCorrecaoCodigoSeg>
			findByProjetoPublicIdAndProjetoUnidadePublicIdOrderByDataHoraAsc(
					UUID projetoPublicId,
					UUID unidadePublicId
			);

	List<HistoricoCorrecaoCodigoSeg>
			findBySciPublicIdAndSciProjetoUnidadePublicIdOrderByDataHoraAsc(
					UUID sciPublicId,
					UUID unidadePublicId
			);

	List<HistoricoCorrecaoCodigoSeg>
			findByAtividadePublicIdAndAtividadeSciProjetoUnidadePublicIdOrderByDataHoraAsc(
					UUID atividadePublicId,
					UUID unidadePublicId
			);
}
