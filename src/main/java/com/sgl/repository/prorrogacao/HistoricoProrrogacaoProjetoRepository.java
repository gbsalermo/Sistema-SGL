package com.sgl.repository.prorrogacao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.prorrogacao.HistoricoProrrogacaoProjeto;

@Repository
public interface HistoricoProrrogacaoProjetoRepository
        extends JpaRepository<HistoricoProrrogacaoProjeto, Long> {

    Optional<HistoricoProrrogacaoProjeto>
            findByPublicIdAndProjetoLaboratorioUnidadePublicId(
                    UUID publicId,
                    UUID unidadePublicId
            );

    List<HistoricoProrrogacaoProjeto>
            findByProjetoPublicIdAndProjetoLaboratorioUnidadePublicIdOrderByDataHoraAsc(
                    UUID projetoPublicId,
                    UUID unidadePublicId
            );
}