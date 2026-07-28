package com.sgl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.Pedido;
import com.sgl.model.enums.StatusPedido;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
	
	List<Pedido> findByUsuarioId(Long usuarioId);
	
	List<Pedido> findByLaboratorioId(Long laboratorioId);
	
	List<Pedido> findByStatus(StatusPedido status);
	
	List<Pedido> findByLaboratorioIdAndStatus(Long laboratorioId, StatusPedido status);

}
