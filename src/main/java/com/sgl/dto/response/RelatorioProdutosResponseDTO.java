package com.sgl.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "Relatório cadastral e gerencial de produtos.")
@Getter
@AllArgsConstructor
public class RelatorioProdutosResponseDTO {

    private LocalDateTime geradoEm;
    private Integer total;
    private Integer ativos;
    private Integer inativos;
    private Integer fiscalizados;
    private Integer pereciveis;
    private Integer comRisco;
    private List<ProdutoResponseDTO> itens;
}
