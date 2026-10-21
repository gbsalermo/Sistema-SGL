package com.sgl.dto.request;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** Edição administrativa somente de observação e término previsto.
 * Alterar escopo exige encerrar o vínculo e criar outro. */
@Getter @Setter
public class EditarVinculoOperacionalRequestDTO {
    private LocalDate dataFim;
    private String observacao;
}
