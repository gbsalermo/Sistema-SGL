package com.sgl.dto.response;

import java.util.UUID;

import com.sgl.model.ItemPedido;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedidoResponseDTO {

    private UUID id;
    private UUID produtoId;
    private String produtoNome;
    private String produtoUnidadeArmazenamento;
    private Integer quantidadeSolicitada;
    private Integer quantidadeAprovada;

    public ItemPedidoResponseDTO(ItemPedido entity) {
        this.id = entity.getPublicId();
        this.produtoId = entity.getProduto().getPublicId();
        this.produtoNome = entity.getProduto().getNome();
        this.produtoUnidadeArmazenamento = entity.getProduto().getUnidadeArmazenamento();
        this.quantidadeSolicitada = entity.getQuantidadeSolicitada();
        this.quantidadeAprovada = entity.getQuantidadeAprovada();
    }
}
