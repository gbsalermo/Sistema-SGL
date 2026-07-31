package com.sgl.dto;

import java.time.LocalDate;

import com.sgl.model.HistoricoLaboratorio;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HistoricoLaboratorioDTO {
	
	private Long id;
	
	private Long laboratorioId;
	
	private String laboratorioNome;
	
	private Long produtoId;
	
	private String produtoNome;
	
	private String produtoUnidadeArmazenamento;
	
	private Integer quantidade;
	
	private LocalDate dataRecebimento;
	
	private Long pedidoId;
	
	private Boolean ativo;
	
	public HistoricoLaboratorioDTO(HistoricoLaboratorio entity) {
		this.id = entity.getId();
		this.laboratorioId = entity.getLaboratorio().getId();
		this.laboratorioNome = entity.getLaboratorio().getNome();
		this.produtoId = entity.getProduto().getId();
		this.produtoNome = entity.getProduto().getNome();
		this.produtoUnidadeArmazenamento = entity.getProduto().getUnidadeArmazenamento();
		this.quantidade = entity.getQuantidade();
		this.dataRecebimento = entity.getDataRecebimento();
		this.pedidoId = entity.getPedido() != null ? entity.getPedido().getId() : null;
		this.ativo = entity.getAtivo();
		
	}

}
