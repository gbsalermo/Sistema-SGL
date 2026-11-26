package com.sgl.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado do cadastro de uma pessoa do lote.")
public record ResultadoImportacaoDTO(String referencia, String nome, Resultado resultado, String mensagem) {

    public enum Resultado { CADASTRADA, JA_CADASTRADA, NAO_ENCONTRADA, ERRO }
}
