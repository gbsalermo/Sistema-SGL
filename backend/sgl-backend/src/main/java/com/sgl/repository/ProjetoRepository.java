package com.sgl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.Projeto;

@Repository
public interface ProjetoRepository extends JpaRepository<Projeto, Long> {
	
	
	List<Projeto> findByLaboratorioId(Long laboratorioId);
	
	List<Projeto> findByAtivoTrue();

}
