package com.sgl.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AprovarPedidoDTO {

	private String observacao;
	
	@NotNull(message = "Lista de itens aprovados é obrigatoria")
	private List<ItemAprovacaoDTO> itens;
	
	private Boolean autorizarProdutoVencido;
	
	@NotNull(message = "Id do usuário que aprovou é obrigatório")
	private Long usuarioAprovadorId;
	
	
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ItemAprovacaoDTO{
		@NotNull(message = "Id do item é obrigatoria")
		private Long itemId;
		
		@NotNull(message = "Quantidade aprovada é obrigatoria")
		private Integer quantidadeAprovada;
	}
}
