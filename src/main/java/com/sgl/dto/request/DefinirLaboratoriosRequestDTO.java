package com.sgl.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Replaces the whole laboratory list of a person; an empty list is only accepted for Gestão profiles. */
@Getter @Setter
public class DefinirLaboratoriosRequestDTO {
    private List<@NotNull(message = "Laboratório inválido na lista") UUID> laboratorioIds;
}
