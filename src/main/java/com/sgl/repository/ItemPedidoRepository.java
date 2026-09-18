package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import com.sgl.model.ItemPedido;

@Repository
public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {
	
	Optional<ItemPedido> findByPublicId(UUID publicId);
	
	List<ItemPedido> findByPedidoId(Long pedidoId);
	
	List<ItemPedido> findByProdutoId(Long produtoId);

}
