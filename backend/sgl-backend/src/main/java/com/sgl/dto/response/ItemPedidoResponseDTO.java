package com.sgl.dto.response;

import java.util.UUID;

import com.sgl.model.ItemPedido;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.TipoPerecivel;
import com.sgl.model.enums.TipoRisco;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Item de pedido retornado pela API.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedidoResponseDTO {

    @Schema(description = "Identificador público UUID do item do pedido.", example = "550e8400-e29b-41d4-a716-446655440005")
    private UUID id;
    @Schema(description = "Identificador público UUID do produto.", example = "550e8400-e29b-41d4-a716-446655440004")
    private UUID produtoId;
    @Schema(description = "Nome do produto.", example = "Extrato de DNA Plant Wizard")
    private String produtoNome;
    @Schema(description = "Unidade de armazenamento do produto.", example = "kit com 50 reações")
    private String produtoUnidadeArmazenamento;
    @Schema(description = "Nível de risco associado ao produto.", example = "BAIXO")
    private NivelRisco produtoRisco;
    @Schema(description = "Tipo específico de risco do produto, quando aplicável.", example = "QUIMICO")
    private TipoRisco produtoTipoRisco;
    @Schema(description = "Descrição complementar de risco do produto.", example = "Evitar contato direto com pele e olhos.")
    private String produtoDescricaoRisco;
    @Schema(description = "Indica se o produto é perecível.", example = "true")
    private Boolean produtoPerecivel;
    @Schema(description = "Tipo de perecibilidade do produto, quando aplicável.", example = "VALIDADE")
    private TipoPerecivel produtoTipoPerecivel;
    @Schema(description = "Condições de armazenamento recomendadas para o produto.", example = "Manter entre 2°C e 8°C.")
    private String produtoCondicoesArmazenamento;
    @Schema(description = "Quantidade originalmente solicitada.", example = "10")
    private Integer quantidadeSolicitada;
    @Schema(description = "Quantidade aprovada para atendimento, quando definida.", example = "8")
    private Integer quantidadeAprovada;

    public ItemPedidoResponseDTO(ItemPedido entity) {
        this.id = entity.getPublicId();
        this.produtoId = entity.getProduto().getPublicId();
        this.produtoNome = entity.getProduto().getNome();
        this.produtoUnidadeArmazenamento = entity.getProduto().getUnidadeArmazenamento();
        this.produtoRisco = entity.getProduto().getRisco();
        this.produtoTipoRisco = entity.getProduto().getTipoRisco();
        this.produtoDescricaoRisco = entity.getProduto().getDescricaoRisco();
        this.produtoPerecivel = entity.getProduto().getPerecivel();
        this.produtoTipoPerecivel = entity.getProduto().getTipoPerecivel();
        this.produtoCondicoesArmazenamento = entity.getProduto().getCondicoesArmazenamento();
        this.quantidadeSolicitada = entity.getQuantidadeSolicitada();
        this.quantidadeAprovada = entity.getQuantidadeAprovada();
    }
}
