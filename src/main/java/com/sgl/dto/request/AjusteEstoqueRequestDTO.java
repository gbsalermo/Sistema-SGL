package com.sgl.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import com.sgl.model.enums.DestinoAjusteEntrada;
import com.sgl.model.enums.TipoAjusteEstoque;
import com.sgl.model.enums.TipoEmbalagem;
import com.sgl.model.enums.UnidadeMedida;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AjusteEstoqueRequestDTO {

	@NotNull(message = "Tipo do ajuste é obrigatório")
	private TipoAjusteEstoque tipoAjuste;

	@NotNull(message = "Lote é obrigatório")
	private UUID loteId;

	private UUID recipienteId;

	private DestinoAjusteEntrada destinoEntrada;

	private TipoEmbalagem tipoEmbalagem;

	@NotNull(message = "Quantidade do ajuste é obrigatória")
	@DecimalMin(value = "0.0", inclusive = false, message = "Quantidade do ajuste deve ser maior que zero")
	private BigDecimal quantidade;

	@NotNull(message = "Unidade de medida é obrigatória")
	private UnidadeMedida unidadeMedida;

	@NotBlank(message = "Justificativa do ajuste é obrigatória")
	@Size(max = 500, message = "Justificativa deve possuir no máximo 500 caracteres")
	private String justificativa;

	@Size(max = 500, message = "Observação deve possuir no máximo 500 caracteres")
	private String observacao;
}