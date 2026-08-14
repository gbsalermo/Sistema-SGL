package com.sgl.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import com.sgl.model.Estagiario;
import com.sgl.model.enums.TipoBolsa;

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

	private UUID id;

	@NotNull(message = "Id do usuário é obrigatório")
	private UUID usuarioId;

	private String usuarioNome;

	@NotNull(message = "Id do laboratório é obrigatório")
	private UUID laboratorioId;

	private String laboratorioNome;

	@NotNull(message = "Data de início do estágio é obrigatória")
	private LocalDate dataInicioEstagio;

	private LocalDate dataFimEstagio;

	@NotNull(message = "Tipo de bolsa é obrigatório")
	private TipoBolsa tipoBolsa;

	private String observacao;

	private Boolean ativo = true;

	public EstagiarioDTO(Estagiario entity) {
		this.id = entity.getPublicId();
		this.usuarioId = entity.getPublicId();
		this.usuarioNome = entity.getNome();
		this.laboratorioId = entity.getLaboratorio() != null ? entity.getLaboratorio().getPublicId() : null;
		this.laboratorioNome = entity.getLaboratorio() != null ? entity.getLaboratorio().getNome() : null;
		this.dataInicioEstagio = entity.getDataInicioEstagio();
		this.dataFimEstagio = entity.getDataFimEstagio();
		this.tipoBolsa = entity.getTipoBolsa();
		this.observacao = entity.getObservacao();
		this.ativo = entity.getAtivo();
	}
}
