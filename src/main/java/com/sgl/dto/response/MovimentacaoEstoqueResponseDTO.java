package com.sgl.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.TipoPedido;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.model.enums.UnidadeMedida;

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

    private UUID id;
    private UUID produtoId;
    private String produtoNome;
    private UUID laboratorioId;
    private String laboratorioNome;
    private UUID usuarioId;
    private String usuarioNome;
    private UUID estoqueCentralId;
    private UUID pedidoId;
    private TipoPedido pedidoTipo;
    private String pedidoSolicitanteNome;
    private UUID loteId;
    private String codigoInternoLote;
    private String numeroLote;
    private TipoMovimentacao tipoMovimentacao;
    private OrigemMovimentacao origem;
    private BigDecimal quantidadeMovimentada;
    private BigDecimal quantidadeAnterior;
    private BigDecimal quantidadeAtual;
    private LocalDateTime dataMovimentacao;
    private String observacao;
    private UnidadeMedida unidadeMedida;


    // Mantém compatibilidade com os usos anteriores do construtor completo nos testes.
    public MovimentacaoEstoqueResponseDTO(
            UUID id, UUID produtoId, String produtoNome, UUID laboratorioId,
            String laboratorioNome, UUID usuarioId, String usuarioNome,
            UUID estoqueCentralId, UUID pedidoId, TipoPedido pedidoTipo,
            String pedidoSolicitanteNome, UUID loteId, String codigoInternoLote,
            String numeroLote, TipoMovimentacao tipoMovimentacao,
            OrigemMovimentacao origem, BigDecimal quantidadeMovimentada,
            BigDecimal quantidadeAnterior, BigDecimal quantidadeAtual,
            LocalDateTime dataMovimentacao, String observacao) {
        this(id, produtoId, produtoNome, laboratorioId, laboratorioNome, usuarioId,
                usuarioNome, estoqueCentralId, pedidoId, pedidoTipo,
                pedidoSolicitanteNome, loteId, codigoInternoLote, numeroLote,
                tipoMovimentacao, origem, quantidadeMovimentada,
                quantidadeAnterior, quantidadeAtual, dataMovimentacao, observacao, null);
    }

    public MovimentacaoEstoqueResponseDTO(MovimentacaoEstoque entity) {
        this.id = entity.getPublicId();
        this.produtoId = entity.getProduto().getPublicId();
        this.produtoNome = entity.getProduto().getNome();
        this.unidadeMedida = entity.getProduto().getUnidadeMedida();
        this.laboratorioId = entity.getLaboratorio() != null ? entity.getLaboratorio().getPublicId() : null;
        this.laboratorioNome = entity.getLaboratorio() != null ? entity.getLaboratorio().getNome() : null;
        this.usuarioId = entity.getUsuario().getPublicId();
        this.usuarioNome = entity.getUsuario().getNome();
        this.estoqueCentralId = entity.getEstoqueCentral().getPublicId();
        this.pedidoId = entity.getPedido() != null ? entity.getPedido().getPublicId() : null;
        this.pedidoTipo = entity.getPedido() != null ? entity.getPedido().getTipo() : null;
        this.pedidoSolicitanteNome = entity.getPedido() != null && entity.getPedido().getUsuario() != null
                ? entity.getPedido().getUsuario().getNome()
                : null;
        this.loteId = entity.getLote() != null ? entity.getLote().getPublicId() : null;
        this.codigoInternoLote = entity.getLote() != null ? entity.getLote().getCodigoInterno() : null;
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
