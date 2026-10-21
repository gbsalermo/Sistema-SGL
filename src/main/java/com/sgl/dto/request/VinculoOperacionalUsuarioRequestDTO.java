package com.sgl.dto.request;

import java.time.LocalDate;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class VinculoOperacionalUsuarioRequestDTO {
    @NotNull private UUID usuarioId;
    @NotNull private UUID laboratorioId;
    private UUID projetoId;
    private UUID sciId;
    private UUID atividadeId;
    @NotNull private LocalDate dataInicio;
    private LocalDate dataFim;
    private String observacao;
}
