package com.sgl.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.enums.TipoMovimentacao;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovimentacaoEstoqueDTO {

	private Long id;
	
	@NotNull(message = "Id do produto é obrigatorio")
	private Long produtoId;
	
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private String produtoNome;
	
	
	private Long laboratorioId;
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private String laboratorioNome;
	
	
	@NotNull(message = "Id do usuario é obrigatorio")
	private Long usuarioId;
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private String usuarioNome;
	
	@NotNull(message = "Id do estoque é obrigatório")
	private Long estoqueCentralId;

	
	private Long pedidoId;
	
	@NotNull(message = "Tipo da movimentação é obrigatório")
	private TipoMovimentacao tipoMovimentacao;
	
	
	@NotNull(message = "Quantidade movimentada é obrigatória")
	private Integer quantidadeMovimentada;
	
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private Integer quantidadeAnterior;
	
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private Integer quantidadeAtual;
	
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private LocalDateTime dataMovimentacao;
	
	private String observacao;
	
	public MovimentacaoEstoqueDTO(MovimentacaoEstoque entity) {
		
		this.id = entity.getId();
		this.produtoId = entity.getProduto().getId();
		this.produtoNome = entity.getProduto().getNome();
		this.laboratorioId = entity.getLaboratorio() != null
	            ? entity.getLaboratorio().getId()
	            : null;
		this.laboratorioNome = entity.getLaboratorio().getNome();
		this.usuarioId = entity.getUsuario().getId();
		this.usuarioNome = entity.getUsuario().getNome();
		this.estoqueCentralId = entity.getEstoqueCentral().getId();
		  this.pedidoId = entity.getPedido() != null
		            ? entity.getPedido().getId()
		            : null;
		this.tipoMovimentacao = entity.getTipoMovimentacao();
		this.quantidadeMovimentada = entity.getQuantidadeMovimentada();
		this.quantidadeAnterior = entity.getQuantidadeAnterior();
		this.quantidadeAtual = entity.getQuantidadeAtual();
		this.dataMovimentacao = entity.getDataMovimentacao();
		this.observacao = entity.getObservacao();
	}
}
