package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.Cultura;

@Repository
public interface CulturaRepository extends JpaRepository<Cultura, Long> {

	Optional<Cultura> findByPublicIdAndUnidadePublicId(UUID publicId, UUID unidadePublicId);

	List<Cultura> findByUnidadePublicIdOrderByNomeAsc(UUID unidadePublicId);

	List<Cultura> findByUnidadePublicIdAndAtivoTrueOrderByNomeAsc(UUID unidadePublicId);

	List<Cultura> findByPublicIdInAndUnidadePublicId(Set<UUID> ids, UUID unidadePublicId);

	boolean existsByUnidadeIdAndNomeIgnoreCase(Long unidadeId, String nome);

	boolean existsByUnidadeIdAndNomeIgnoreCaseAndIdNot(Long unidadeId, String nome, Long id);
}