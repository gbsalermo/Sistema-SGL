package com.sgl.dto.request;

import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Dados necessários para registrar uma prorrogação.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProrrogacaoRequestDTO {

    @Schema(
            description = "UUID do usuário responsável pela operação. Campo temporário enquanto o backend ainda não possui autenticação real.",
            example = "550e8400-e29b-41d4-a716-446655440000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "O usuário responsável pela prorrogação é obrigatório")
    private UUID usuarioId;

    @Schema(
            description = "Nova data final solicitada.",
            example = "2027-03-31",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "A nova data de fim é obrigatória")
    private LocalDate novaDataFim;

    @Schema(
            description = "Justificativa institucional para a prorrogação.",
            example = "Necessidade de ampliar o período para conclusão das análises.",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "A justificativa da prorrogação é obrigatória")
    @Size(max = 1000, message = "A justificativa deve possuir no máximo 1000 caracteres")
    private String justificativa;
}