package com.sgl.dto;

import com.sgl.model.EstoqueCentral;

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
public class EstoqueCentralDTO {

	private Long id;
	
	@NotNull(message = "Id do produto é obrigatorio")
	private Long produtoId;
	
	private String produtoNome;

	@NotNull(message = "quantidade atual é obrigatoria")
	@Min(value = 0, message = "Quantidade atual não pode ser negativa")
	private Integer quantidadeAtual;
	
	
	
	@NotNull(message = "quantidade atual é obrigatoria")
	@Min(value = 0, message = "quantidade mínima não pode ser negativa")
	private Integer quantidadeMinima;
	
	private Boolean ativo;
	
	
	public EstoqueCentralDTO(EstoqueCentral entity) {
		
		this.id = entity.getId();
		this.produtoId = entity.getProduto().getId();
		this.produtoNome = entity.getProduto().getNome();
		this.quantidadeAtual = entity.getQuantidadeAtual();
		this.quantidadeMinima = entity.getQuantidadeMinima();
		this.ativo = entity.getAtivo();
		
	}
	
}
