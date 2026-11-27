package com.sgl.dto.request;

import java.util.List;

import com.sgl.corporate.TipoPessoaCorporativa;
import com.sgl.model.enums.CategoriaCargo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Pessoas da base da Embrapa a cadastrar de uma vez na equipe do laboratório.")
public record ImportarLoteRequestDTO(
        @Schema(description = "Até 50 pessoas por envio.")
        @NotEmpty(message = "Informe ao menos uma pessoa")
        @Size(max = 50, message = "Envie no máximo 50 pessoas por vez")
        List<@Valid @NotNull ItemImportacao> itens) {

    @Schema(description = "Uma pessoa a cadastrar. O CPF nunca trafega: a pessoa é buscada de novo pela referência.")
    public record ItemImportacao(
            @NotNull(message = "Tipo é obrigatório") TipoPessoaCorporativa tipo,
            @NotBlank(message = "Referência é obrigatória") String referencia,
            @Schema(description = "Categoria escolhida; nula usa a sugerida pelo cargo.") CategoriaCargo categoriaCargo) {
    }
}
