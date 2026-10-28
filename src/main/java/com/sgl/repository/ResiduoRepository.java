package com.sgl.repository;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sgl.model.Residuo;
import com.sgl.model.enums.StatusResiduo;
import com.sgl.model.enums.SituacaoUsuario;

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

    /**
     * Resíduos ainda em fluxo (status fora de {@code finais}) da unidade cujo gestor
     * recebedor inicial não pode mais exercer o cargo (inativo, não ATIVO no SGL ou de outra unidade).
     */
    @Query("""
            SELECT residuo
            FROM Residuo residuo
            JOIN FETCH residuo.gestorRecebedorInicial gestor
            WHERE residuo.laboratorio.unidade.publicId = :unidadeId
              AND residuo.status NOT IN :finais
              AND (gestor.ativo = false
                   OR gestor.situacao <> :liberado
                   OR gestor.unidade IS NULL
                   OR gestor.unidade <> residuo.laboratorio.unidade)
            ORDER BY residuo.dataInformacao DESC
            """)
    List<Residuo> findEmFluxoComGestorSemCondicoes(@Param("unidadeId") UUID unidadeId,
            @Param("finais") Collection<StatusResiduo> finais,
            @Param("liberado") SituacaoUsuario liberado);
}
