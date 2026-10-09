package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.Solucao;

@Repository
public interface SolucaoRepository extends JpaRepository<Solucao, Long> {

    @EntityGraph(attributePaths = {"unidade", "componentes", "componentes.produto"})
    Optional<Solucao> findByPublicIdAndUnidadePublicId(UUID publicId, UUID unidadePublicId);

    @EntityGraph(attributePaths = {"unidade", "componentes", "componentes.produto"})
    List<Solucao> findByUnidadePublicIdOrderByNomeAsc(UUID unidadePublicId);

    @EntityGraph(attributePaths = {"unidade", "componentes", "componentes.produto"})
    List<Solucao> findByUnidadePublicIdAndAtivoTrueOrderByNomeAsc(UUID unidadePublicId);

    boolean existsByUnidadeIdAndNomeIgnoreCase(Long unidadeId, String nome);

    boolean existsByUnidadeIdAndNomeIgnoreCaseAndIdNot(
            Long unidadeId,
            String nome,
            Long id
    );
}
