package com.sgl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.Unidade;




@Repository //Classe responsavel pela camada de persistÃªncia
public interface  UnidadeRepository extends JpaRepository<Unidade, Long> {
}

