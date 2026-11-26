package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.Bolsa;

@Repository
public interface BolsaRepository extends JpaRepository<Bolsa, Long> {

    Optional<Bolsa> findByPublicIdAndUnidadePublicId(UUID id, UUID unidadeId);

    List<Bolsa> findByUnidadePublicIdOrderByNomeAsc(UUID unidadeId);

    List<Bolsa> findByUnidadePublicIdAndAtivoTrueOrderByNomeAsc(UUID unidadeId);

    boolean existsByUnidadeIdAndNomeIgnoreCase(Long unidadeId, String nome);

    boolean existsByUnidadeIdAndNomeIgnoreCaseAndIdNot(Long unidadeId, String nome, Long id);
}
