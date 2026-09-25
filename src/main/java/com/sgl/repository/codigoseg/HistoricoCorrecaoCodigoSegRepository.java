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
			findByProjetoPublicIdAndProjetoLaboratorioUnidadePublicIdOrderByDataHoraAsc(
					UUID projetoPublicId,
					UUID unidadePublicId
			);

	List<HistoricoCorrecaoCodigoSeg>
			findBySciPublicIdAndSciProjetoLaboratorioUnidadePublicIdOrderByDataHoraAsc(
					UUID sciPublicId,
					UUID unidadePublicId
			);

	List<HistoricoCorrecaoCodigoSeg>
			findByAtividadePublicIdAndAtividadeSciProjetoLaboratorioUnidadePublicIdOrderByDataHoraAsc(
					UUID atividadePublicId,
					UUID unidadePublicId
			);
}
