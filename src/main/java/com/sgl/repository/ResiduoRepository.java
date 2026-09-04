package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sgl.model.Residuo;
import com.sgl.model.enums.StatusResiduo;

@Repository
public interface ResiduoRepository extends JpaRepository<Residuo, Long> {

    Optional<Residuo> findByPublicId(UUID publicId);
    Optional<Residuo> findByPublicIdAndLaboratorioUnidadePublicId(UUID publicId, UUID unidadePublicId);

    @Query("""
            SELECT residuo
            FROM Residuo residuo
            WHERE (:#{@tenantProvider.unidadeId} IS NULL
               OR residuo.laboratorio.unidade.publicId = :#{@tenantProvider.unidadeId})
            ORDER BY residuo.dataInformacao DESC
            """)
    List<Residuo> findAllByOrderByDataInformacaoDesc();

    List<Residuo> findByLaboratorioUnidadePublicIdOrderByDataInformacaoDesc(UUID unidadePublicId);

    @Query("""
            SELECT residuo
            FROM Residuo residuo
            WHERE residuo.status = :status
              AND (:#{@tenantProvider.unidadeId} IS NULL
               OR residuo.laboratorio.unidade.publicId = :#{@tenantProvider.unidadeId})
            ORDER BY residuo.dataInformacao DESC
            """)
    List<Residuo> findByStatusOrderByDataInformacaoDesc(@Param("status") StatusResiduo status);

    List<Residuo> findByLaboratorioUnidadePublicIdAndStatusOrderByDataInformacaoDesc(
            UUID unidadePublicId,
            StatusResiduo status
    );

    List<Residuo> findByLaboratorioPublicIdOrderByDataInformacaoDesc(UUID laboratorioPublicId);

    List<Residuo> findByGeradorPublicIdOrderByDataInformacaoDesc(UUID geradorPublicId);
}
