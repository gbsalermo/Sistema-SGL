package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.Residuo;
import com.sgl.model.enums.StatusResiduo;

@Repository
public interface ResiduoRepository extends JpaRepository<Residuo, Long> {

    Optional<Residuo> findByPublicId(UUID publicId);

    List<Residuo> findAllByOrderByDataInformacaoDesc();

    List<Residuo> findByStatusOrderByDataInformacaoDesc(StatusResiduo status);

    List<Residuo> findByLaboratorioPublicIdOrderByDataInformacaoDesc(UUID laboratorioPublicId);
}
