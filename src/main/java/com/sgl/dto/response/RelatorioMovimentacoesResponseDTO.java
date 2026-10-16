package com.sgl.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "Relatório consolidado de movimentações de estoque.")
@Getter
@AllArgsConstructor
public class RelatorioMovimentacoesResponseDTO {

    private LocalDateTime geradoEm;
    private Integer totalMovimentacoes;
    private BigDecimal quantidadeEntradas;
    private BigDecimal quantidadeSaidas;
    private BigDecimal quantidadeAjustes;
    private BigDecimal quantidadeDevolucoes;
    private BigDecimal quantidadeDescartes;
    private List<MovimentacaoEstoqueResponseDTO> itens;
}
