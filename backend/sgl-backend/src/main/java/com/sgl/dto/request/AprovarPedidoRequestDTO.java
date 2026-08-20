package com.sgl.dto.request;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Dados necessários para aprovar um pedido pendente.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AprovarPedidoRequestDTO {

    @Schema(description = "Observação opcional registrada durante a aprovação.", example = "Aprovado para atendimento.")
    private String observacao;

    @Schema(description = "Itens do pedido e respectivas quantidades aprovadas.", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotEmpty(message = "Lista de itens aprovados deve possuir pelo menos um item")
    private List<ItemAprovacaoDTO> itens;

    @Schema(description = "Identificador público UUID do usuário responsável pela aprovação.", example = "550e8400-e29b-41d4-a716-446655440001", requiredMode = Schema.RequiredMode.REQUIRED)
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

    @Schema(description = "Item individual informado na aprovação do pedido.")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemAprovacaoDTO {

        @Schema(description = "Identificador público UUID do item do pedido.", example = "550e8400-e29b-41d4-a716-446655440005", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Id do item é obrigatório")
        private UUID itemId;

        @Schema(description = "Quantidade aprovada para o item.", example = "8", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Quantidade aprovada é obrigatória")
        @Min(value = 1, message = "Quantidade aprovada deve ser no mínimo 1")
        private Integer quantidadeAprovada;
    }
}
