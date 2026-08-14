package com.sgl.dto;

import java.time.LocalDate;
import java.util.UUID;

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

	private UUID id;
	
	@NotNull(message = "Id do laboratório é obrigatorio")
	private UUID laboratorioId;
	
	private String laboratorioNome;
	
	@NotBlank(message = "Nome é obrigatório")
	private String nome;
	
	private String descricao;
	
	private LocalDate dataInicio;
	
	private LocalDate dataFim;
	
	private String responsavel;
	
	private Boolean ativo;
	
	public ProjetoDTO(Projeto entity) {
		
		this.id = entity.getPublicId();
		this.laboratorioId = entity.getLaboratorio().getPublicId();
		this.laboratorioNome = entity.getLaboratorio().getNome();
		this.nome = entity.getNome();
		this.descricao = entity.getDescricao();
		this.dataInicio = entity.getDataInicio();
		this.dataFim = entity.getDataFim();
		this.responsavel = entity.getResponsavel();
		this.ativo = entity.getAtivo();
	}

}
