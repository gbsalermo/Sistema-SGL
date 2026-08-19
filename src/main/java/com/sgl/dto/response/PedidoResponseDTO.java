package com.sgl.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.sgl.model.Pedido;
import com.sgl.model.enums.StatusPedido;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PedidoResponseDTO {

    private UUID id;
    private UUID usuarioId;
    private String usuarioNome;
    private UUID laboratorioId;
    private String laboratorioNome;
    private UUID projetoId;
    private String projetoNome;
    private LocalDateTime dataSolicitacao;
    private StatusPedido status;
    private String observacao;
    private String arquivoDocumento;
    private List<ItemPedidoResponseDTO> itens;

    public PedidoResponseDTO(Pedido entity) {
        this.id = entity.getPublicId();
        this.usuarioId = entity.getUsuario().getPublicId();
        this.usuarioNome = entity.getUsuario().getNome();
        this.laboratorioId = entity.getLaboratorio().getPublicId();
        this.laboratorioNome = entity.getLaboratorio().getNome();
        this.projetoId = entity.getProjeto() != null ? entity.getProjeto().getPublicId() : null;
        this.projetoNome = entity.getProjeto() != null ? entity.getProjeto().getNome() : null;
        this.dataSolicitacao = entity.getDataSolicitacao();
        this.status = entity.getStatus();
        this.observacao = entity.getObservacao();
        this.arquivoDocumento = entity.getArquivoDocumento();
        this.itens = entity.getItens().stream()
                .map(ItemPedidoResponseDTO::new)
                .toList();
    }
}
