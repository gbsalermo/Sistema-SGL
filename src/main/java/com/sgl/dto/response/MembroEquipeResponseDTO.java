package com.sgl.dto.response;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import com.sgl.model.enums.CategoriaCargo;
import com.sgl.model.enums.PapelSistema;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pessoa da equipe de um laboratório.")
public record MembroEquipeResponseDTO(
        UUID usuarioId,
        String nome,
        String email,
        Set<PapelSistema> papeis,
        CategoriaCargo categoriaCargo,
        String cargo,
        @Schema(description = "O laboratório está na lista da pessoa (acesso sem prazo).") boolean permanente,
        @Schema(description = "Fim do acesso temporário mais longo e vigente; nulo se não houver ou se não tiver fim.") LocalDate acessoTemporarioAte,
        @Schema(description = "A pessoa é a responsável pelo laboratório.") boolean responsavel) {
}
