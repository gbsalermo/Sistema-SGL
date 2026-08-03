package com.sgl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.Estagiario;

@Repository
public interface EstagiarioRepository extends JpaRepository<Estagiario, Long> {

	List<Estagiario> findByLaboratorioId(Long laboratorioId);

	List<Estagiario> findByAtivoTrue();

	boolean existsByUsuarioId(Long usuarioId);

	boolean existsByUsuarioIdAndIdNot(Long usuarioId, Long id);
}
