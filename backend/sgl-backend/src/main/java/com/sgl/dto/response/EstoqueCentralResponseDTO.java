package com.sgl.dto.response;

import java.util.UUID;

import com.sgl.model.EstoqueCentral;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Representação de um item do estoque central retornado pela API.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstoqueCentralResponseDTO {

    @Schema(description = "Identificador público UUID do registro de estoque.", example = "550e8400-e29b-41d4-a716-446655440012")
    private UUID id;
    @Schema(description = "Identificador público UUID da unidade.", example = "550e8400-e29b-41d4-a716-446655440002")
    private UUID unidadeId;
    @Schema(description = "Nome da unidade.", example = "Instituto de Química")
    private String unidadeNome;
    @Schema(description = "Sigla da unidade.", example = "IQ")
    private String unidadeSigla;
    @Schema(description = "Identificador público UUID do produto.", example = "550e8400-e29b-41d4-a716-446655440004")
    private UUID produtoId;
    @Schema(description = "Nome do produto.", example = "Extrato de DNA Plant Wizard")
    private String produtoNome;
    @Schema(description = "Código de referência do produto.", example = "DNA-PW-50")
    private String produtoCodigoReferencia;
    @Schema(description = "Localização física do produto na unidade.", example = "AMX2 - Prateleira 3")
    private String produtoLocalizacaoFisica;
    @Schema(description = "Apresentação ou forma de acondicionamento do produto.", example = "kit com 50 reações")
    private String produtoUnidadeArmazenamento;
    @Schema(description = "Quantidade total atual consolidada a partir dos lotes.", example = "20")
    private Integer quantidadeAtual;
    @Schema(description = "Quantidade mínima configurada para alerta de estoque baixo.", example = "3")
    private Integer quantidadeMinima;
    @Schema(description = "Indica se o registro de estoque está ativo.", example = "true")
    private Boolean ativo;

    public EstoqueCentralResponseDTO(EstoqueCentral entity) {
        this.id = entity.getPublicId();
        this.unidadeId = entity.getUnidade().getPublicId();
        this.unidadeNome = entity.getUnidade().getNome();
        this.unidadeSigla = entity.getUnidade().getSigla();
        this.produtoId = entity.getProduto().getPublicId();
        this.produtoNome = entity.getProduto().getNome();
        this.produtoCodigoReferencia = entity.getProduto().getCodigoReferencia();
        this.produtoLocalizacaoFisica = entity.getProduto().getLocalizacaoFisica();
        this.produtoUnidadeArmazenamento = entity.getProduto().getUnidadeArmazenamento();
        this.quantidadeAtual = entity.getQuantidadeAtual();
        this.quantidadeMinima = entity.getQuantidadeMinima();
        this.ativo = entity.getAtivo();
    }
}
