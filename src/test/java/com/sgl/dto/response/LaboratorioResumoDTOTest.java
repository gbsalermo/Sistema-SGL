package com.sgl.dto.response;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sgl.model.Laboratorio;

class LaboratorioResumoDTOTest {

    private Laboratorio laboratorio(Boolean ativo) {
        return Laboratorio.builder().publicId(UUID.randomUUID()).nome("Lab").ativo(ativo).build();
    }

    @Test
    void exposeLaboratorioAtivo() {
        assertTrue(LaboratorioResumoDTO.de(laboratorio(true)).ativo());
    }

    @Test
    void exposeLaboratorioInativo() {
        assertFalse(LaboratorioResumoDTO.de(laboratorio(false)).ativo());
    }

    @Test
    void tratarAtivoNuloComoInativo() {
        assertFalse(LaboratorioResumoDTO.de(laboratorio(null)).ativo());
    }
}
