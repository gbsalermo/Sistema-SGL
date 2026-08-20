package com.sgl.dto.request;

import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.TipoPerecivel;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Dados necessários para cadastrar ou atualizar um produto do catálogo.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoRequestDTO {

    @Schema(description = "Nome do produto.", example = "Extrato de DNA Plant Wizard", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "nome é obrigatório")
    private String nome;

    @Schema(description = "Descrição do produto.", example = "Kit para extração de DNA vegetal.")
    private String descricao;

    @Schema(description = "Código de referência do produto.", example = "DNA-PW-50", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Codigo de referência é obrigatório")
    private String codigoReferencia;

    @Schema(description = "Unidade de medida adotada para o produto.", example = "UNIDADE", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Informe a Unidade de medida")
    private UnidadeMedida unidadeMedida;

    @Schema(description = "Localização física do produto no armazenamento.", example = "Armário A - Prateleira 2")
    private String localizacaoFisica;

    @Schema(description = "Nível de risco associado ao produto.", example = "BAIXO", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "risco é obrigatório")
    private NivelRisco risco;

    @Schema(description = "Tipo específico de risco, quando aplicável.", example = "QUIMICO")
    private TipoRisco tipoRisco;

    @Schema(description = "Descrição complementar dos riscos do produto.", example = "Evitar contato direto com pele e olhos.")
    private String descricaoRisco;

    @Schema(description = "Indica se o produto é perecível.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Precisa confirmar se é perecivel")
    private Boolean perecivel;

    @Schema(description = "Tipo de perecibilidade do produto, quando aplicável.", example = "VALIDADE")
    private TipoPerecivel tipoPerecivel;

    @Schema(description = "Condições recomendadas de armazenamento.", example = "Manter entre 2°C e 8°C.")
    private String condicoesArmazenamento;

    @Schema(description = "Forma de armazenamento apresentada ao usuário.", example = "kit com 50 reações")
    private String unidadeArmazenamento;

    @Schema(description = "Indica se o produto está ativo no catálogo.", example = "true")
    private Boolean ativo;
}
