package com.sgl.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Unidade da lista corporativa da Embrapa, indicando se já está habilitada no SGL.")
public record UnidadeCorporativaResponseDTO(
        @Schema(description = "unidadeId da Unidadeapi.", example = "7")
        Integer codigoCorporativo,
        String sigla,
        String nome,
        @Schema(description = "Indica se a Unidade já existe no SGL com este código.")
        boolean habilitada) {
}
