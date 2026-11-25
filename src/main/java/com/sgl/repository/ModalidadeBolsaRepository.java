package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.ModalidadeBolsa;

@Repository
public interface ModalidadeBolsaRepository extends JpaRepository<ModalidadeBolsa, Long> {

    Optional<ModalidadeBolsa> findByPublicIdAndUnidadePublicId(UUID id, UUID unidadeId);

    List<ModalidadeBolsa> findByUnidadePublicIdOrderByNomeAsc(UUID unidadeId);

    List<ModalidadeBolsa> findByUnidadePublicIdAndAtivoTrueOrderByNomeAsc(UUID unidadeId);

    boolean existsByUnidadeIdAndNomeIgnoreCase(Long unidadeId, String nome);

    boolean existsByUnidadeIdAndNomeIgnoreCaseAndIdNot(Long unidadeId, String nome, Long id);
}
