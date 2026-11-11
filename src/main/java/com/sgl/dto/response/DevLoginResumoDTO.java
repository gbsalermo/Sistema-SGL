package com.sgl.dto.response;

import java.util.List;
import java.util.Map;

import com.sgl.model.enums.Perfil;

public record DevLoginResumoDTO(Map<Perfil, Long> contagemPorPerfil, List<UnidadeOpcaoDTO> unidades) {
}
