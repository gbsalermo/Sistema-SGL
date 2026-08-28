package com.sgl.dto.response;

public record ArquivoRelatorioDTO(
        byte[] conteudo,
        String nomeArquivo,
        String contentType
) {
}
