package com.sgl.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.HistoricoResiduo;

@Repository
public interface HistoricoResiduoRepository extends JpaRepository<HistoricoResiduo, Long> {

    List<HistoricoResiduo> findByResiduoIdOrderByDataHoraAsc(Long residuoId);

    List<HistoricoResiduo> findByResiduoGeradorPublicIdAndResiduoLaboratorioUnidadePublicIdOrderByDataHoraDesc(
            UUID geradorPublicId,
            UUID unidadePublicId
    );
}
