package com.sgl.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sgl.model.codigoseg.HistoricoCorrecaoCodigoSeg;
import com.sgl.model.enums.TipoAlvoCodigoSeg;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "Registro histórico de uma correção administrativa de Código SEG.")
@Getter
public class HistoricoCorrecaoCodigoSegResponseDTO {

	@Schema(description = "UUID público do registro de histórico.")
	private final UUID id;

	@Schema(description = "Tipo de entidade cujo Código SEG foi corrigido.")
	private final TipoAlvoCodigoSeg tipoAlvo;

	@Schema(description = "UUID público da entidade corrigida.")
	private final UUID alvoId;

	@Schema(description = "UUID do usuário responsável pela correção.")
	private final UUID usuarioId;

	@Schema(description = "Nome do usuário responsável pela correção.")
	private final String usuarioNome;

	@Schema(description = "Código SEG existente antes da correção.")
	private final String codigoAnterior;

	@Schema(description = "Novo Código SEG aplicado.")
	private final String codigoNovo;

	@Schema(description = "Justificativa registrada para a correção.")
	private final String justificativa;

	@Schema(description = "Data e hora em que a correção foi registrada.")
	private final LocalDateTime dataHora;

	public HistoricoCorrecaoCodigoSegResponseDTO(HistoricoCorrecaoCodigoSeg entity) {

		this.id = entity.getPublicId();
		this.tipoAlvo = entity.getTipoAlvo();
		this.alvoId = resolverAlvoId(entity);
		this.usuarioId = entity.getUsuario() != null ? entity.getUsuario().getPublicId() : null;
		this.usuarioNome = entity.getUsuario() != null ? entity.getUsuario().getNome() : null;
		this.codigoAnterior = entity.getCodigoAnterior();
		this.codigoNovo = entity.getCodigoNovo();
		this.justificativa = entity.getJustificativa();
		this.dataHora = entity.getDataHora();
	}

	private UUID resolverAlvoId(HistoricoCorrecaoCodigoSeg entity) {

		return switch (entity.getTipoAlvo()) {
			case PROJETO -> entity.getProjeto() != null ? entity.getProjeto().getPublicId() : null;
			case SCI -> entity.getSci() != null ? entity.getSci().getPublicId() : null;
			case ATIVIDADE -> entity.getAtividade() != null ? entity.getAtividade().getPublicId() : null;
		};
	}
}
