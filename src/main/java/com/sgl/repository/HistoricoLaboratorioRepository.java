package com.sgl.repository;

import java.time.LocalDate;
import java.util.List;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import com.sgl.model.HistoricoLaboratorio;

@Repository
public interface HistoricoLaboratorioRepository extends JpaRepository<HistoricoLaboratorio, Long>{
	
	List<HistoricoLaboratorio> findByLaboratorioId(Long laboratorioId);
	
	List<HistoricoLaboratorio> findByProdutoId(Long produtoId);
	
	List<HistoricoLaboratorio> findByPedidoId(Long pedidoId);
	
	
	// Retorna os itens recebidos por um laboratório dentro de um intervalo de datas (usado em relatórios de período)
	@Query("SELECT el FROM HistoricoLaboratorio el WHERE el.laboratorio.id = :laboratorioId AND el.dataRecebimento BETWEEN :dataInicio AND :dataFim")
	List<HistoricoLaboratorio> findByLaboratorioIdAndPeriodo(
			@Param("laboratorioId") Long laboratorioId,
			@Param("dataInicio") LocalDate dataInicio,
			@Param("dataFim") LocalDate dataFim);
	
	

}
