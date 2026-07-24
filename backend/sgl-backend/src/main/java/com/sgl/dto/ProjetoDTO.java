package com.sgl.dto;

import java.time.LocalDate;

import com.sgl.model.Projeto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjetoDTO {

	private Long id;
	
	@NotNull(message = "Id do laboratório é obrigatorio")
	private Long laboratorioId;
	
	private String laboratorioNome;
	
	@NotBlank(message = "Nome é obrigatório")
	private String nome;
	
	private String descricao;
	
	private LocalDate DataInicio;
	
	private LocalDate dataFim;
	
	private String responsavel;
	
	private Boolean ativo;
	
	public ProjetoDTO(Projeto entity) {
		
		this.id = entity.getId();
		this.laboratorioId = entity.getLaboratorio().getId();
		this.laboratorioNome = entity.getLaboratorio().getNome();
		this.nome = entity.getNome();
		this.descricao = entity.getDescricao();
		this.DataInicio = entity.getDataInicio();
		this.dataFim = entity.getDataFim();
		this.responsavel = entity.getResponsavel();
		this.ativo = entity.getAtivo();
	}

}
