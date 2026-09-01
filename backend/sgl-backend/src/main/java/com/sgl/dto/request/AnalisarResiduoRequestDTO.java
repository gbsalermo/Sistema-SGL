package com.sgl.dto.request;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.TipoRisco;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Conferência técnica do resíduo e liberação para rotulagem e armazenamento temporário.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnalisarResiduoRequestDTO {

    @NotNull(message = "O usuário gestor é obrigatório")
    private UUID usuarioGestorId;

    @NotNull(message = "O nível de risco confirmado é obrigatório")
    private NivelRisco nivelRiscoConfirmado;

    @NotNull(message = "Os riscos confirmados são obrigatórios")
    private Set<TipoRisco> riscosConfirmados;

    @NotBlank(message = "O local de armazenamento temporário é obrigatório")
    @Schema(example = "Abrigo de resíduos - setor químico A")
    private String localArmazenamentoTemporario;

    @NotBlank(message = "O destino final previsto é obrigatório")
    @Schema(example = "Empresa licenciada para tratamento de resíduos químicos")
    private String destinoFinalPrevisto;

    @FutureOrPresent(message = "A data prevista de despacho não pode estar no passado")
    private LocalDate dataPrevistaDespacho;

    private String observacaoGestor;
}
