package com.sgl.dto.response;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import com.sgl.model.Estagiario;
import com.sgl.model.enums.TipoBolsa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstagiarioResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private UUID usuarioId;
    private String usuarioNome;
    private UUID laboratorioId;
    private String laboratorioNome;
    private LocalDate dataInicioEstagio;
    private LocalDate dataFimEstagio;
    private TipoBolsa tipoBolsa;
    private String observacao;
    private Boolean ativo;

    public EstagiarioResponseDTO(Estagiario entity) {
        this.id = entity.getPublicId();
        this.usuarioId = entity.getPublicId();
        this.usuarioNome = entity.getNome();
        this.laboratorioId = entity.getLaboratorio() != null ? entity.getLaboratorio().getPublicId() : null;
        this.laboratorioNome = entity.getLaboratorio() != null ? entity.getLaboratorio().getNome() : null;
        this.dataInicioEstagio = entity.getDataInicioEstagio();
        this.dataFimEstagio = entity.getDataFimEstagio();
        this.tipoBolsa = entity.getTipoBolsa();
        this.observacao = entity.getObservacao();
        this.ativo = entity.getAtivo();
    }
}
