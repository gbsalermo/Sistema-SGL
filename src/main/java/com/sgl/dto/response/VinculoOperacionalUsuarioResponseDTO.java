package com.sgl.dto.response;

import java.time.LocalDate;
import java.util.UUID;
import com.sgl.model.VinculoOperacionalUsuario;
import lombok.Getter;

@Getter
public class VinculoOperacionalUsuarioResponseDTO {
    private final UUID id;
    private final UUID usuarioId;
    private final UUID laboratorioId;
    private final String laboratorioNome;
    private final UUID projetoId;
    private final String projetoNome;
    private final UUID sciId;
    private final String sciNome;
    private final UUID atividadeId;
    private final String atividadeNome;
    private final LocalDate dataInicio;
    private final LocalDate dataFim;
    private final String observacao;
    public VinculoOperacionalUsuarioResponseDTO(VinculoOperacionalUsuario v) {
        id=v.getPublicId(); usuarioId=v.getUsuario().getPublicId();
        laboratorioId=v.getLaboratorio().getPublicId();
        laboratorioNome=v.getLaboratorio().getNome();
        projetoId=v.getProjeto()==null?null:v.getProjeto().getPublicId();
        projetoNome=v.getProjeto()==null?null:v.getProjeto().getNome();
        sciId=v.getSci()==null?null:v.getSci().getPublicId();
        sciNome=v.getSci()==null?null:v.getSci().getNome();
        atividadeId=v.getAtividade()==null?null:v.getAtividade().getPublicId();
        atividadeNome=v.getAtividade()==null?null:v.getAtividade().getNome();
        dataInicio=v.getDataInicio(); dataFim=v.getDataFim();
        observacao=v.getObservacao();
    }
}
