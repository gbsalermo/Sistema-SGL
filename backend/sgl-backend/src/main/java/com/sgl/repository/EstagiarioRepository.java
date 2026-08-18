package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.Estagiario;
import com.sgl.model.Usuario;

@Repository
public interface EstagiarioRepository extends JpaRepository<Estagiario, Long> {

	List<Estagiario> findByLaboratorioId(Long laboratorioId);

	List<Estagiario> findByAtivoTrue();
	
	boolean existsByIdAndAtivoTrue(Long usuarioId);

	Optional<Estagiario> findById(UUID id);
	
	Optional<Estagiario> findByPublicId(UUID publicId);
	

}
