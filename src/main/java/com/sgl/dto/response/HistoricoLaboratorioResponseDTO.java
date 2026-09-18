package com.sgl.dto.response;

import java.time.LocalDate;
import java.util.UUID;

import com.sgl.model.HistoricoLaboratorio;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Representação de um registro do histórico de recebimento de materiais por laboratório.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HistoricoLaboratorioResponseDTO {

    @Schema(description = "Identificador público UUID do registro histórico.", example = "550e8400-e29b-41d4-a716-446655440015")
    private UUID id;
    @Schema(description = "Identificador público UUID do laboratório.", example = "550e8400-e29b-41d4-a716-446655440003")
    private UUID laboratorioId;
    @Schema(description = "Nome do laboratório.", example = "Laboratório de Química Orgânica")
    private String laboratorioNome;
    @Schema(description = "Identificador público UUID do produto recebido.", example = "550e8400-e29b-41d4-a716-446655440004")
    private UUID produtoId;
    @Schema(description = "Nome do produto recebido.", example = "Extrato de DNA Plant Wizard")
    private String produtoNome;
    @Schema(description = "Unidade de armazenamento do produto.", example = "kit com 50 reações")
    private String produtoUnidadeArmazenamento;
    @Schema(description = "Quantidade efetivamente recebida pelo laboratório.", example = "8")
    private Integer quantidade;
    @Schema(description = "Data em que o material foi recebido.", example = "2026-08-20")
    private LocalDate dataRecebimento;
    @Schema(description = "Identificador público UUID do pedido de origem, quando houver.", example = "550e8400-e29b-41d4-a716-446655440010")
    private UUID pedidoId;
    @Schema(description = "Indica se o registro histórico está ativo.", example = "true")
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
