package com.sgl.dto.request;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.sgl.model.enums.FormacaoEstagiario;
import com.sgl.model.enums.OrigemSincronizacaoVinculoEstagio;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.model.enums.TipoBolsa;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Estado institucional recebido para sincronização de um vínculo de estágio já existente.")
public class SincronizacaoVinculoEstagioRequestDTO {

	@NotNull(message = "A origem da sincronização é obrigatória")
	private OrigemSincronizacaoVinculoEstagio origem;

	@Size(max = 120)
	@Schema(description = "Identificador do evento na fonte externa, quando disponível.")
	private String referenciaEvento;

	@NotNull(message = "A situação institucional é obrigatória")
	private SituacaoEstagio situacao;

	@Schema(description = "Data de início institucional, quando a fonte externa a informar.")
	private LocalDate dataInicio;

	@NotNull(message = "A data final prevista é obrigatória")
	private LocalDate dataFimPrevista;

	@Schema(description = "Modalidade/bolsa institucional, quando a fonte externa a informar.")
	private TipoBolsa tipoBolsa;

	@Schema(description = "Formação institucional, quando a fonte externa a informar.")
	private FormacaoEstagiario formacao;

	@Schema(description = "Descrição usada quando a formação institucional for OUTRO.")
	private String formacaoOutro;

	@Schema(description = "UUID do Curso já conhecido pelo SGL, quando informado pela integração.")
	private UUID cursoId;

	@Schema(description = "UUID do Orientador já conhecido pelo SGL, quando informado pela integração.")
	private UUID orientadorId;

	@Schema(description = "Data efetiva do encerramento. Obrigatória quando a situação for FINALIZADO.")
	private LocalDate dataFimEfetiva;

	@Schema(description = "Data e hora do evento na fonte institucional, quando disponível.")
	private LocalDateTime dataHoraOrigem;

	@Size(max = 120)
	@Schema(description = "Referência do vínculo/bolsa na fonte institucional, quando disponível.")
	private String referenciaInstitucional;
}