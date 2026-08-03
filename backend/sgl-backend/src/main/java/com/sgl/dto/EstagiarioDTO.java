package com.sgl.dto;

import java.io.Serializable;
import java.time.LocalDate;

import com.sgl.model.Estagiario;
import com.sgl.model.enums.TipoBolsa;

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
public class EstagiarioDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long id;

	@NotNull(message = "Id do usuário é obrigatório")
	private Long usuarioId;

	private String usuarioNome;

	@NotNull(message = "Id do laboratório é obrigatório")
	private Long laboratorioId;

	private String laboratorioNome;

	@NotNull(message = "Data de início do estágio é obrigatória")
	private LocalDate dataInicioEstagio;

	private LocalDate dataFimEstagio;

	@NotNull(message = "Tipo de bolsa é obrigatório")
	private TipoBolsa tipoBolsa;

	@NotBlank(message = "Função é obrigatória")
	private String funcao;

	private String observacao;

	private Boolean ativo = true;

	public EstagiarioDTO(Estagiario entity) {
		this.id = entity.getId();
		this.usuarioId = entity.getUsuario().getId();
		this.usuarioNome = entity.getUsuario().getNome();
		this.laboratorioId = entity.getLaboratorio().getId();
		this.laboratorioNome = entity.getLaboratorio().getNome();
		this.dataInicioEstagio = entity.getDataInicioEstagio();
		this.dataFimEstagio = entity.getDataFimEstagio();
		this.tipoBolsa = entity.getTipoBolsa();
		this.funcao = entity.getFuncao();
		this.observacao = entity.getObservacao();
		this.ativo = entity.getAtivo();
	}
}
