package com.sgl.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import com.sgl.model.enums.TipoEmbalagem;
import com.sgl.model.enums.UnidadeMedida;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Item solicitado em um pedido de materiais.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedidoRequestDTO {

	@Schema(description = "Identificador público UUID do produto solicitado.", example = "550e8400-e29b-41d4-a716-446655440004", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Id do produto é obrigatório")
	private UUID produtoId;

	@Schema(description = "Quantidade solicitada: na unidade informada para quantidade física ou Soluções; na unidade canônica para retirada por apresentação.", example = "50", minimum = "0.000001", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Quantidade solicitada é obrigatória")
	@DecimalMin(value = "0.0", inclusive = false, message = "Quantidade solicitada deve ser maior que zero")
	private BigDecimal quantidadeSolicitada;

	@Schema(description = "Forma física de retirada. Obrigatória somente em Pedidos de produtos.", example = "KIT")
	// Obrigatorio apenas para Pedido de PRODUTOS. O pedido-pai valida
	// a forma de retirada conforme o tipo; Solucao utiliza unidade fisica.
	private TipoEmbalagem tipoEmbalagemSolicitada;

	@Schema(description = "Quantidade de embalagens solicitadas. Obrigatória somente em Pedidos de produtos.", example = "1", minimum = "1")
	@Min(value = 1, message = "Quantidade da forma de retirada deve ser no mínimo 1")
	private Integer quantidadeEmbalagensSolicitada;

	@Schema(description = "Multiplicador de unidades por embalagem. Obrigatório somente em Pedidos de produtos.", example = "50", minimum = "0.000001")
	@DecimalMin(value = "0.0", inclusive = false, message = "Multiplicador deve ser maior que zero")
	private BigDecimal multiplicadorSolicitado;

    @Schema(description = "Unidade física informada para Soluções ou Pedidos convencionais por quantidade física. Não combinar com campos de apresentação.")
    private UnidadeMedida unidadeMedidaSolicitada;

    // Construtor legado: pedidos comuns usam embalagem e multiplicador.
    public ItemPedidoRequestDTO(UUID produtoId, BigDecimal quantidadeSolicitada,
            TipoEmbalagem tipoEmbalagemSolicitada, Integer quantidadeEmbalagensSolicitada,
            BigDecimal multiplicadorSolicitado) {
        this.produtoId = produtoId;
        this.quantidadeSolicitada = quantidadeSolicitada;
        this.tipoEmbalagemSolicitada = tipoEmbalagemSolicitada;
        this.quantidadeEmbalagensSolicitada = quantidadeEmbalagensSolicitada;
        this.multiplicadorSolicitado = multiplicadorSolicitado;
    }
}
