package com.sgl.dto.response;

import java.util.UUID;

import com.sgl.model.EstoqueCentral;
import com.sgl.model.enums.UnidadeMedida;

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

    private UUID id;
    private UUID unidadeId;
    private String unidadeNome;
    private String unidadeSigla;
    private UUID produtoId;
    private String produtoNome;
    private String produtoCodigoReferencia;
    private String produtoLocalizacaoFisica;
    @Schema(description = "Apresentação padrão descritiva do produto.", example = "kit com 50 reações")
    private String produtoUnidadeArmazenamento;
    @Schema(description = "Unidade-base usada para consolidar saldo entre lotes.", example = "UNIDADE")
    private UnidadeMedida produtoUnidadeMedida;
    private Integer quantidadeAtual;
    private Integer quantidadeMinima;
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
        this.produtoUnidadeMedida = entity.getProduto().getUnidadeMedida();
        this.quantidadeAtual = entity.getQuantidadeAtual();
        this.quantidadeMinima = entity.getQuantidadeMinima();
        this.ativo = entity.getAtivo();
    }
}
