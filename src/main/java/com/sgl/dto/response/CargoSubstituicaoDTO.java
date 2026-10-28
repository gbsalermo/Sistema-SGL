package com.sgl.dto.response;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/** Um cargo da unidade cujo ocupante não pode mais exercê-lo e precisa ser substituído. */
@Schema(description = "Cargo da unidade que precisa de substituição do ocupante.")
public record CargoSubstituicaoDTO(
        @Schema(description = "Tipo do cargo.") TipoCargo tipo,
        @Schema(description = "Identificador público do registro (laboratório, vínculo de estágio ou resíduo).") UUID registroId,
        @Schema(description = "Descrição legível do registro.", example = "Laboratório de Química") String descricao,
        @Schema(description = "Identificador público do ocupante atual.") UUID ocupanteId,
        @Schema(description = "Nome do ocupante atual.") String ocupanteNome) {

    public enum TipoCargo {
        RESPONSAVEL_LABORATORIO,
        ORIENTADOR_ESTAGIO,
        GESTOR_RECEBEDOR_RESIDUO
    }
}
