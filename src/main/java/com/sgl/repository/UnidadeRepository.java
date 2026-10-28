package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sgl.model.Unidade;

@Repository
public interface UnidadeRepository extends JpaRepository<Unidade, Long> {

    Optional<Unidade> findByPublicId(UUID publicId);

    @Override
    @Query("""
            SELECT unidade
            FROM Unidade unidade
            WHERE (:#{@tenantProvider.unidadeId} IS NULL
               OR unidade.publicId = :#{@tenantProvider.unidadeId})
            """)
    List<Unidade> findAll();

    /**
     * Every Unidade, bypassing the tenant filter of {@link #findAll()}. Only for the general
     * administrator, who operates on all of them (selector and enablement screen).
     */
    @Query("SELECT unidade FROM Unidade unidade ORDER BY unidade.sigla")
    List<Unidade> findAllSemFiltroDeTenant();

    Optional<Unidade> findByCodigoCorporativo(Integer codigoCorporativo);

    boolean existsBySigla(String sigla);

    boolean existsBySiglaAndIdNot(String sigla, Long id);
}
