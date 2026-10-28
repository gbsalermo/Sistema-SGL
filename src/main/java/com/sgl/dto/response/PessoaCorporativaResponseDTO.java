package com.sgl.dto.response;

import com.sgl.corporate.TipoPessoaCorporativa;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;

import io.swagger.v3.oas.annotations.media.Schema;

/** A search row. Deliberately has no CPF field: personal identifiers never reach the browser. */
@Schema(description = "Pessoa encontrada nas bases corporativas.")
public record PessoaCorporativaResponseDTO(
        @Schema(description = "Referência opaca para importar: matrícula (empregado) ou ColaboradorId (colaborador).")
        String referencia,
        TipoPessoaCorporativa tipo,
        String nome,
        @Schema(description = "Somente para empregados.")
        String matricula,
        String cargo,
        String sublotacao,
        String unidadeNome,
        @Schema(description = "E-mail corporativo; nulo para colaboradores, que não o têm na base.")
        String email,
        @Schema(description = "Indica se a pessoa já consta no SGL (por matrícula ou CPF).")
        boolean jaCadastrado,
        @Schema(description = "Situação do cadastro; nulo se não cadastrada.")
        SituacaoUsuario situacao,
        @Schema(description = "Perfil sugerido a partir do cargo; nulo se não reconhecido.")
        Perfil perfilSugerido) {
}
