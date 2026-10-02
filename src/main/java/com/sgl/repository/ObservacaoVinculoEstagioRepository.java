package com.sgl.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.ObservacaoVinculoEstagio;

@Repository
public interface ObservacaoVinculoEstagioRepository extends JpaRepository<ObservacaoVinculoEstagio, Long> {

    List<ObservacaoVinculoEstagio>
            findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataHoraDesc(
                    UUID vinculoId,
                    UUID unidadeId);
}
