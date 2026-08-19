package com.sgl.dto.request;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import com.sgl.model.enums.TipoBolsa;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstagiarioRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Id do usuário é obrigatório")
    private UUID usuarioId;

    @NotNull(message = "Id do laboratório é obrigatório")
    private UUID laboratorioId;

    @NotNull(message = "Data de início do estágio é obrigatória")
    private LocalDate dataInicioEstagio;

    private LocalDate dataFimEstagio;

    @NotNull(message = "Tipo de bolsa é obrigatório")
    private TipoBolsa tipoBolsa;

    private String observacao;
    private Boolean ativo;
}
