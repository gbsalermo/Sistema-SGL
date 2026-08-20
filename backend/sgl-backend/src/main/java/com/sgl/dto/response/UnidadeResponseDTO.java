package com.sgl.dto.response;

import java.io.Serializable;
import java.util.UUID;

import com.sgl.model.Unidade;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Representação de uma unidade institucional retornada pela API.")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnidadeResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Identificador público UUID da unidade.", example = "550e8400-e29b-41d4-a716-446655440002")
    private UUID id;
    @Schema(description = "Nome completo da unidade.", example = "Instituto de Química")
    private String nome;
    @Schema(description = "Sigla da unidade.", example = "IQ")
    private String sigla;

    public UnidadeResponseDTO(Unidade entity) {
        this.id = entity.getPublicId();
        this.nome = entity.getNome();
        this.sigla = entity.getSigla();
    }
}
