package com.sgl.dto;

import java.util.UUID;

import com.sgl.model.ItemPedido;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedidoDTO {

	private UUID id;
	
	@NotNull(message = "Id do produto é obrigatório")
	private UUID produtoId;
	
	private String produtoNome;
	
	private String produtoUnidadeArmazenamento;
	
	@NotNull(message = "Quantidade solicitada é obrigatoria")
	@Min(value = 1, message = "Quantidade solicitada deve ser no mínimo 1")
	private Integer quantidadeSolicitada;
	
	private Integer quantidadeAprovada;
	
	public ItemPedidoDTO(ItemPedido entity) {
		this.id = entity.getPublicId();
		this.produtoId = entity.getProduto().getPublicId();
		this.produtoNome = entity.getProduto().getNome();
		this.produtoUnidadeArmazenamento = entity.getProduto().getUnidadeArmazenamento();
		this.quantidadeSolicitada = entity.getQuantidadeSolicitada();
		this.quantidadeAprovada = entity.getQuantidadeAprovada();
	}
}
