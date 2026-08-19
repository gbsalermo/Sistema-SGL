package com.sgl.dto.response;

import java.io.Serializable;
import java.util.UUID;

import com.sgl.model.Laboratorio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LaboratorioResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private UUID unidadeId;
    private String nome;
    private String descricao;
    private UUID responsavelId;
    private String responsavelNome;
    private Boolean ativo;

    public LaboratorioResponseDTO(Laboratorio entity) {
        this.id = entity.getPublicId();
        this.unidadeId = entity.getUnidade() != null ? entity.getUnidade().getPublicId() : null;
        this.nome = entity.getNome();
        this.descricao = entity.getDescricao();
        this.responsavelId = entity.getResponsavel() != null ? entity.getResponsavel().getPublicId() : null;
        this.responsavelNome = entity.getResponsavel() != null ? entity.getResponsavel().getNome() : null;
        this.ativo = entity.getAtivo();
    }
}
