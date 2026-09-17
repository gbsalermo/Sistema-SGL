package com.sgl.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "Relatório operacional de resíduos laboratoriais.")
@Getter
@AllArgsConstructor
public class RelatorioResiduosResponseDTO {

    private LocalDateTime geradoEm;
    private Integer total;
    private Integer informados;
    private Integer emAnalise;
    private Integer liberados;
    private Integer armazenados;
    private Integer despachados;
    private Integer altoRisco;
    private List<ResiduoResponseDTO> itens;
}
