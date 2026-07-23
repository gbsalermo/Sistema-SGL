package com.sgl.dto;

import java.io.Serializable;
import java.time.LocalDate;

import com.sgl.model.Produto;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.TipoPerecivel;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoDTO implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private Long id;
	
	@NotBlank(message = "nome é obrigatório")
	private String nome;
	
	private String descricao;
	
	@NotBlank(message = "Codigo de referência é obrigatório")
	private String codigoReferencia;
	
	@NotNull(message = "Informe a Unidade de medida")
	private UnidadeMedida unidadeMedida;
	
	private String localizacaoFisica;
	
	@NotNull(message = "risco é obrigatório")
	private NivelRisco risco;
	
	private TipoRisco tipoRisco;
	
	private String descricaoRisco;
	
	@NotNull(message = "Precisa confirmar se é perecivel")
	private Boolean perecivel;
	
	private TipoPerecivel tipoPerecivel;
	
	private LocalDate dataValidade;
	
	private String condicoesArmazenamento;

	private String unidadeArmazenamento;

	private boolean ativo;
	
	public ProdutoDTO(Produto entity) {
		this.id = entity.getId();
		this.nome = entity.getNome();
		this.descricao = entity.getDescricao();
		this.codigoReferencia = entity.getCodigoReferencia();
		this.unidadeMedida = entity.getUnidadeMedida();
		this.localizacaoFisica = entity.getLocalizacaoFisica();
		this.risco = entity.getRisco();
		this.tipoRisco = entity.getTipoRisco();
		this.descricaoRisco = entity.getDescricaoRisco();
		this.perecivel = entity.getPerecivel();
		this.tipoPerecivel = entity.getTipoPerecivel();
		this.dataValidade = entity.getDataValidade();
		this.condicoesArmazenamento = entity.getCondicoesArmazenamento();
		this.unidadeArmazenamento = entity.getUnidadeArmazenamento();
		this.ativo = entity.getAtivo();
	}

}
