package com.sgl.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
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
public class PedidoRequestDTO {

    @NotNull(message = "Id do usuário é obrigatório")
    private UUID usuarioId;

    @NotNull(message = "Id do laboratório é obrigatório")
    private UUID laboratorioId;

    private UUID projetoId;
    private String observacao;
    private String arquivoDocumento;

    @Valid
    @NotEmpty(message = "Pedido deve ter pelo menos 1 item")
    private List<ItemPedidoRequestDTO> itens;
}
