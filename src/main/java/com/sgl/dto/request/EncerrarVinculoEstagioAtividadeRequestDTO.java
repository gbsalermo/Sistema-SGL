package com.sgl.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EncerrarVinculoEstagioAtividadeRequestDTO {

    @NotNull(message = "Data de fim da participação é obrigatória")
    private LocalDate dataFimParticipacao;
}