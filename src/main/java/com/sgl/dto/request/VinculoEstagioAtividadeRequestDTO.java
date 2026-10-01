package com.sgl.dto.request;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VinculoEstagioAtividadeRequestDTO {

    @NotNull(message = "Atividade é obrigatória")
    private UUID atividadeId;

    @NotNull(message = "Data de início da participação é obrigatória")
    private LocalDate dataInicioParticipacao;

    private String observacao;
}