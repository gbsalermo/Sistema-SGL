package com.sgl.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import com.sgl.model.ModeloSolucao;

public interface ModeloSolucaoRepository extends JpaRepository<ModeloSolucao, Long> {
    @EntityGraph(attributePaths = {"unidade","componentes","componentes.produto"})
    Optional<ModeloSolucao> findByPublicIdAndUnidadePublicId(UUID id, UUID unidadeId);

    @EntityGraph(attributePaths = {"unidade","componentes","componentes.produto"})
    List<ModeloSolucao> findDistinctByUnidadePublicIdOrderByNomeAsc(UUID unidadeId);

    @EntityGraph(attributePaths = {"unidade","componentes","componentes.produto"})
    List<ModeloSolucao> findDistinctByUnidadePublicIdAndAtivoTrueOrderByNomeAsc(UUID unidadeId);

    boolean existsByUnidadeIdAndNomeIgnoreCase(Long unidadeId, String nome);
    boolean existsByUnidadeIdAndNomeIgnoreCaseAndIdNot(Long unidadeId, String nome, Long id);
}

