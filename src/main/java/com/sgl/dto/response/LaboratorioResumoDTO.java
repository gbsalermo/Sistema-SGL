package com.sgl.dto.response;

import java.util.List;
import java.util.UUID;

import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;

public record LaboratorioResumoDTO(UUID id, String nome) {

    public static LaboratorioResumoDTO de(Laboratorio laboratorio) {
        return new LaboratorioResumoDTO(laboratorio.getPublicId(), laboratorio.getNome());
    }

    public static List<LaboratorioResumoDTO> de(Projeto projeto) {
        return projeto.laboratoriosOrdenados().stream()
                .map(LaboratorioResumoDTO::de)
                .toList();
    }
}
