package com.sgl.dto.response;

import java.time.LocalDate;
import java.util.UUID;

import com.sgl.model.HistoricoLaboratorio;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HistoricoLaboratorioResponseDTO {

    private UUID id;
    private UUID laboratorioId;
    private String laboratorioNome;
    private UUID produtoId;
    private String produtoNome;
    private String produtoUnidadeArmazenamento;
    private Integer quantidade;
    private LocalDate dataRecebimento;
    private UUID pedidoId;
    private Boolean ativo;

    public HistoricoLaboratorioResponseDTO(HistoricoLaboratorio entity) {
        this.id = entity.getPublicId();
        this.laboratorioId = entity.getLaboratorio().getPublicId();
        this.laboratorioNome = entity.getLaboratorio().getNome();
        this.produtoId = entity.getProduto().getPublicId();
        this.produtoNome = entity.getProduto().getNome();
        this.produtoUnidadeArmazenamento = entity.getProduto().getUnidadeArmazenamento();
        this.quantidade = entity.getQuantidade();
        this.dataRecebimento = entity.getDataRecebimento();
        this.pedidoId = entity.getPedido() != null ? entity.getPedido().getPublicId() : null;
        this.ativo = entity.getAtivo();
    }
}
