package com.sgl.repository.prorrogacao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.prorrogacao.HistoricoProrrogacaoAtividade;

@Repository
public interface HistoricoProrrogacaoAtividadeRepository
        extends JpaRepository<HistoricoProrrogacaoAtividade, Long> {

    Optional<HistoricoProrrogacaoAtividade>
            findByPublicIdAndAtividadeSciProjetoLaboratorioUnidadePublicId(
                    UUID publicId,
                    UUID unidadePublicId
            );

    List<HistoricoProrrogacaoAtividade>
            findByAtividadePublicIdAndAtividadeSciProjetoLaboratorioUnidadePublicIdOrderByDataHoraAsc(
                    UUID atividadePublicId,
                    UUID unidadePublicId
            );
}