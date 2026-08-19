package com.sgl.dto.response;

import java.io.Serializable;
import java.util.UUID;

import com.sgl.model.Unidade;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnidadeResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private String nome;
    private String sigla;

    public UnidadeResponseDTO(Unidade entity) {
        this.id = entity.getPublicId();
        this.nome = entity.getNome();
        this.sigla = entity.getSigla();
    }
}
