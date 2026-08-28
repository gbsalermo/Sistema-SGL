package com.sgl.dto.response;

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
    private Integer quantidadeEntradas;
    private Integer quantidadeSaidas;
    private Integer quantidadeAjustes;
    private Integer quantidadeDevolucoes;
    private Integer quantidadeDescartes;
    private List<MovimentacaoEstoqueResponseDTO> itens;
}
