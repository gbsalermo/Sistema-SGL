package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.sgl.model.VinculoOperacionalUsuario;

public interface VinculoOperacionalUsuarioRepository extends JpaRepository<VinculoOperacionalUsuario, Long> {
    List<VinculoOperacionalUsuario> findByUsuarioPublicIdAndUsuarioUnidadePublicId(UUID usuarioId, UUID unidadeId);
    Optional<VinculoOperacionalUsuario> findByPublicIdAndUsuarioUnidadePublicId(UUID id, UUID unidadeId);
}
