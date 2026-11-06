package com.sgl.repository.prorrogacao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.prorrogacao.HistoricoProrrogacaoSci;

@Repository
public interface HistoricoProrrogacaoSciRepository
        extends JpaRepository<HistoricoProrrogacaoSci, Long> {

    Optional<HistoricoProrrogacaoSci>
            findByPublicIdAndSciProjetoUnidadePublicId(
                    UUID publicId,
                    UUID unidadePublicId
            );

    List<HistoricoProrrogacaoSci>
            findBySciPublicIdAndSciProjetoUnidadePublicIdOrderByDataHoraAsc(
                    UUID sciPublicId,
                    UUID unidadePublicId
            );
}