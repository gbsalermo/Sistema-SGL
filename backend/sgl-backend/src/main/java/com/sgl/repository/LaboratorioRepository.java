package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.Laboratorio;

@Repository
public interface LaboratorioRepository extends JpaRepository<Laboratorio, Long> {

    Optional<Laboratorio> findByPublicId(UUID publicId);
    Optional<Laboratorio> findByPublicIdAndUnidadePublicId(UUID publicId, UUID unidadePublicId);

    List<Laboratorio> findByUnidadeId(Long unidadeId);
    List<Laboratorio> findByUnidadePublicId(UUID unidadePublicId);
}
