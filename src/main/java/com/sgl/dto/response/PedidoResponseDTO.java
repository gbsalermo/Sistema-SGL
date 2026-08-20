package com.sgl.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.sgl.model.Pedido;
import com.sgl.model.enums.StatusPedido;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Representação de um pedido retornado pela API.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PedidoResponseDTO {

    @Schema(description = "Identificador público UUID do pedido.", example = "550e8400-e29b-41d4-a716-446655440010")
    private UUID id;
    @Schema(description = "Identificador público UUID do usuário solicitante.", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID usuarioId;
    @Schema(description = "Nome do usuário solicitante.", example = "Maria Oliveira")
    private String usuarioNome;
    @Schema(description = "Identificador público UUID do laboratório.", example = "550e8400-e29b-41d4-a716-446655440002")
    private UUID laboratorioId;
    @Schema(description = "Nome do laboratório.", example = "Laboratório de Química Orgânica")
    private String laboratorioNome;
    @Schema(description = "Identificador público UUID do projeto, quando houver.", example = "550e8400-e29b-41d4-a716-446655440003")
    private UUID projetoId;
    @Schema(description = "Nome do projeto vinculado ao pedido, quando houver.", example = "Síntese de Novos Compostos")
    private String projetoNome;
    @Schema(description = "Data e hora em que o pedido foi criado.", example = "2026-08-20T14:30:00")
    private LocalDateTime dataSolicitacao;
    @Schema(description = "Status atual do pedido.", example = "APROVADO")
    private StatusPedido status;
    @Schema(description = "Observação registrada no pedido.", example = "Aprovado para atendimento.")
    private String observacao;
    @Schema(description = "Referência ao documento associado ao pedido, quando houver.", example = "solicitacao-2026-08.pdf")
    private String arquivoDocumento;
    @Schema(description = "Itens que compõem o pedido.")
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
