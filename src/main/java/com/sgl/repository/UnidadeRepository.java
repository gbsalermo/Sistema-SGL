package com.sgl.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.Unidade;




@Repository //Classe responsavel pela camada de persistÃªncia
public interface  UnidadeRepository extends JpaRepository<Unidade, Long> {
	
	Optional<Unidade> findByPublicId(UUID publicId);
	
	boolean existsBySigla(String sigla);
	
	boolean existsBySiglaAndIdNot(String sigla, Long id);
}

