package com.sgl.dto.response;

import java.math.BigDecimal;
import java.util.UUID;
import com.sgl.model.ComponenteModeloSolucao;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.model.medida.ConversorUnidadeMedida;
import lombok.*;

@Getter @NoArgsConstructor
public class ComponenteModeloSolucaoResponseDTO {
    private UUID id;
    private UUID produtoId;
    private String produtoNome;
    private Integer ordem;
    private BigDecimal quantidade;
    private UnidadeMedida unidadeMedida;
    private BigDecimal quantidadeCanonica;
    private UnidadeMedida unidadeCanonica;

    public ComponenteModeloSolucaoResponseDTO(ComponenteModeloSolucao c) {
        id = c.getPublicId();
        produtoId = c.getProduto().getPublicId();
        produtoNome = c.getProduto().getNome();
        ordem = c.getOrdem();
        quantidade = c.getQuantidade();
        unidadeMedida = c.getUnidadeMedida();
        unidadeCanonica = c.getProduto().getUnidadeMedida();
        quantidadeCanonica = ConversorUnidadeMedida.converterParaCanonica(
             quantidade, unidadeMedida, unidadeCanonica);
    }
}

