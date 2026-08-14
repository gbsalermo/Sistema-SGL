package com.sgl.dto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AprovarPedidoDTO {

    private String observacao;

    @Valid
    @NotEmpty(message = "Lista de itens aprovados deve possuir pelo menos um item")
    private List<ItemAprovacaoDTO> itens;

    @NotNull(message = "Id do usuário que aprovou é obrigatório")
    private UUID usuarioAprovadorId;

    @JsonIgnore
    @AssertTrue(message = "O mesmo item do pedido não pode ser informado mais de uma vez")
    public boolean isItensSemDuplicidade() {
        if (itens == null) {
            return true;
        }

        Set<UUID> ids = new HashSet<>();
        return itens.stream()
                .map(ItemAprovacaoDTO::getItemId)
                .filter(id -> id != null)
                .allMatch(ids::add);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemAprovacaoDTO {

        @NotNull(message = "Id do item é obrigatório")
        private UUID itemId;

        @NotNull(message = "Quantidade aprovada é obrigatória")
        @Min(value = 1, message = "Quantidade aprovada deve ser no mínimo 1")
        private Integer quantidadeAprovada;
    }
}
