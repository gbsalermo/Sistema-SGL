package com.sgl.dto.response;

import java.util.UUID;

import com.sgl.model.ItemPedido;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.TipoEmbalagem;
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

    private UUID id;
    private UUID produtoId;
    private String produtoNome;
    private String produtoUnidadeArmazenamento;
    private NivelRisco produtoRisco;
    private TipoRisco produtoTipoRisco;
    private String produtoDescricaoRisco;
    private Boolean produtoPerecivel;
    private TipoPerecivel produtoTipoPerecivel;
    private String produtoCondicoesArmazenamento;
    private Integer quantidadeSolicitada;
    private Integer quantidadeAprovada;
    private TipoEmbalagem tipoEmbalagemSolicitada;
    private Integer quantidadeEmbalagensSolicitada;
    private Integer multiplicadorSolicitado;

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
        this.tipoEmbalagemSolicitada = entity.getTipoEmbalagemSolicitada();
        this.quantidadeEmbalagensSolicitada = entity.getQuantidadeEmbalagensSolicitada();
        this.multiplicadorSolicitado = entity.getMultiplicadorSolicitado();
    }
}
