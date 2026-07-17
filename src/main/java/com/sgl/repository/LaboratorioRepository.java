package com.sgl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.Laboratorio;

@Repository //Classe responsavel pela camada de persistencia
public interface LaboratorioRepository extends JpaRepository<Laboratorio, Long> {
	
	List<Laboratorio> findByUnidadeId(Long unidadeId);
}
