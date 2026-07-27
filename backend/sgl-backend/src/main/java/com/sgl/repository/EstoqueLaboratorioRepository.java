package com.sgl.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sgl.model.EstoqueLaboratorio;

@Repository
public interface EstoqueLaboratorioRepository extends JpaRepository<EstoqueLaboratorio, Long>{
	
	List<EstoqueLaboratorio> findByLaboratorioid(Long laboratorioId);
	
	List<EstoqueLaboratorio> findByProdutoId(Long produtoId);
	
	List<EstoqueLaboratorio> findByPedidoid(Long pedidoId);
	
	Optional<EstoqueLaboratorio> findByLaboratorioidAndProdutoid(Long laboratoriId, Long produtoId);
	
	// Retorna os itens recebidos por um laboratório dentro de um intervalo de datas (usado em relatórios de período)
	@Query("SELECT el FROM EstoqueLaboratorio el WHERE el.laboratorio.id = :laboratorioId AND el.dataRecebimento BETWEEN :dataInicio AND :dataFim")
	List<EstoqueLaboratorio> findByLaboratorioIdAndPeriodo(
			@Param("laboratorioId") Long laboratorioId,
			@Param("dataInicio") LocalDate dataInicio,
			@Param("dataFim") LocalDate dataFim);
	
	

}
