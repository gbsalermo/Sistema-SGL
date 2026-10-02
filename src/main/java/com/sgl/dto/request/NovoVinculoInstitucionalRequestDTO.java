package com.sgl.dto.request;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.sgl.model.enums.FormacaoEstagiario;
import com.sgl.model.enums.OrigemSincronizacaoVinculoEstagio;
import com.sgl.model.enums.TipoBolsa;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NovoVinculoInstitucionalRequestDTO {

    @NotNull
    private UUID orientadorId;

    @NotNull
    private LocalDate dataInicio;

    private LocalDate dataFimPrevista;

    @NotNull
    private TipoBolsa tipoBolsa;

    @NotNull
    private FormacaoEstagiario formacao;

    private String formacaoOutro;

    private UUID cursoId;

    private String observacao;

    @NotNull
    private OrigemSincronizacaoVinculoEstagio origem;

    @Size(max = 120)
    private String referenciaEvento;

    @Size(max = 120)
    private String referenciaInstitucional;

    private LocalDateTime dataHoraOrigem;
}