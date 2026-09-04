package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.Projeto;

@Repository
public interface ProjetoRepository extends JpaRepository<Projeto, Long> {

    Optional<Projeto> findByPublicId(UUID publicId);
    Optional<Projeto> findByPublicIdAndLaboratorioUnidadePublicId(UUID publicId, UUID unidadePublicId);

    List<Projeto> findByLaboratorioId(Long laboratorioId);
    List<Projeto> findByLaboratorioUnidadePublicId(UUID unidadePublicId);

    List<Projeto> findByAtivoTrue();
    List<Projeto> findByLaboratorioUnidadePublicIdAndAtivoTrue(UUID unidadePublicId);
}
