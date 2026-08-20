package com.sgl.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.TipoMovimentacao;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Representação de uma movimentação de estoque retornada pela API.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovimentacaoEstoqueResponseDTO {

    @Schema(description = "Identificador público UUID da movimentação.", example = "550e8400-e29b-41d4-a716-446655440014")
    private UUID id;
    @Schema(description = "Identificador público UUID do produto movimentado.", example = "550e8400-e29b-41d4-a716-446655440004")
    private UUID produtoId;
    @Schema(description = "Nome do produto movimentado.", example = "Extrato de DNA Plant Wizard")
    private String produtoNome;
    @Schema(description = "Identificador público UUID do laboratório relacionado, quando houver.", example = "550e8400-e29b-41d4-a716-446655440003")
    private UUID laboratorioId;
    @Schema(description = "Nome do laboratório relacionado, quando houver.", example = "Laboratório de Química Orgânica")
    private String laboratorioNome;
    @Schema(description = "Identificador público UUID do usuário responsável pela operação.", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID usuarioId;
    @Schema(description = "Nome do usuário responsável pela operação.", example = "Maria Oliveira")
    private String usuarioNome;
    @Schema(description = "Identificador público UUID do estoque central.", example = "550e8400-e29b-41d4-a716-446655440012")
    private UUID estoqueCentralId;
    @Schema(description = "Identificador público UUID do pedido relacionado, quando houver.", example = "550e8400-e29b-41d4-a716-446655440010")
    private UUID pedidoId;
    @Schema(description = "Identificador público UUID do lote movimentado, quando houver.", example = "550e8400-e29b-41d4-a716-446655440013")
    private UUID loteId;
    @Schema(description = "Número do lote movimentado, quando houver.", example = "LOT-2026-001")
    private String numeroLote;
    @Schema(description = "Tipo da movimentação realizada.", example = "SAIDA")
    private TipoMovimentacao tipoMovimentacao;
    @Schema(description = "Origem que motivou a movimentação.", example = "PEDIDO")
    private OrigemMovimentacao origem;
    @Schema(description = "Quantidade movimentada.", example = "8")
    private Integer quantidadeMovimentada;
    @Schema(description = "Quantidade registrada antes da movimentação.", example = "20")
    private Integer quantidadeAnterior;
    @Schema(description = "Quantidade registrada após a movimentação.", example = "12")
    private Integer quantidadeAtual;
    @Schema(description = "Data e hora em que a movimentação foi registrada.", example = "2026-08-20T14:35:00")
    private LocalDateTime dataMovimentacao;
    @Schema(description = "Observação associada à movimentação.", example = "Saída registrada durante aprovação do pedido.")
    private String observacao;

    public MovimentacaoEstoqueResponseDTO(MovimentacaoEstoque entity) {
        this.id = entity.getPublicId();
        this.produtoId = entity.getProduto().getPublicId();
        this.produtoNome = entity.getProduto().getNome();
        this.laboratorioId = entity.getLaboratorio() != null
                ? entity.getLaboratorio().getPublicId()
                : null;
        this.laboratorioNome = entity.getLaboratorio() != null
                ? entity.getLaboratorio().getNome()
                : null;
        this.usuarioId = entity.getUsuario().getPublicId();
        this.usuarioNome = entity.getUsuario().getNome();
        this.estoqueCentralId = entity.getEstoqueCentral().getPublicId();
        this.pedidoId = entity.getPedido() != null
                ? entity.getPedido().getPublicId()
                : null;
        this.loteId = entity.getLote() != null ? entity.getLote().getPublicId() : null;
        this.numeroLote = entity.getLote() != null ? entity.getLote().getNumeroLote() : null;
        this.tipoMovimentacao = entity.getTipoMovimentacao();
        this.origem = entity.getOrigem();
        this.quantidadeMovimentada = entity.getQuantidadeMovimentada();
        this.quantidadeAnterior = entity.getQuantidadeAnterior();
        this.quantidadeAtual = entity.getQuantidadeAtual();
        this.dataMovimentacao = entity.getDataMovimentacao();
        this.observacao = entity.getObservacao();
    }
}
