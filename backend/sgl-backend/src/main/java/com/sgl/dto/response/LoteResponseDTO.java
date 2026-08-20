package com.sgl.dto.response;

import java.time.LocalDate;
import java.util.UUID;

import com.sgl.model.Lote;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Representação de um lote retornado pela API.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoteResponseDTO {

    @Schema(description = "Identificador público UUID do lote.", example = "550e8400-e29b-41d4-a716-446655440013")
    private UUID id;
    @Schema(description = "Identificador público UUID do estoque central.", example = "550e8400-e29b-41d4-a716-446655440012")
    private UUID estoqueCentralId;
    @Schema(description = "Identificador público UUID do produto.", example = "550e8400-e29b-41d4-a716-446655440004")
    private UUID produtoId;
    @Schema(description = "Nome do produto.", example = "Extrato de DNA Plant Wizard")
    private String produtoNome;
    @Schema(description = "Identificador público UUID da unidade.", example = "550e8400-e29b-41d4-a716-446655440002")
    private UUID unidadeId;
    @Schema(description = "Nome da unidade.", example = "Instituto de Química")
    private String unidadeNome;
    @Schema(description = "Número de identificação do lote.", example = "LOT-2026-001")
    private String numeroLote;
    @Schema(description = "Quantidade registrada na entrada original do lote.", example = "20")
    private Integer quantidadeInicial;
    @Schema(description = "Quantidade ainda disponível no lote.", example = "8")
    private Integer quantidadeDisponivel;
    @Schema(description = "Data de entrada do lote no estoque.", example = "2026-08-20")
    private LocalDate dataEntrada;
    @Schema(description = "Data de validade do lote, quando aplicável.", example = "2027-08-31")
    private LocalDate dataValidade;
    @Schema(description = "Indica se o lote está ativo.", example = "true")
    private Boolean ativo;

    public LoteResponseDTO(Lote entity) {
        this.id = entity.getPublicId();
        this.estoqueCentralId = entity.getEstoqueCentral().getPublicId();
        this.produtoId = entity.getEstoqueCentral().getProduto().getPublicId();
        this.produtoNome = entity.getEstoqueCentral().getProduto().getNome();
        this.unidadeId = entity.getEstoqueCentral().getUnidade().getPublicId();
        this.unidadeNome = entity.getEstoqueCentral().getUnidade().getNome();
        this.numeroLote = entity.getNumeroLote();
        this.quantidadeInicial = entity.getQuantidadeInicial();
        this.quantidadeDisponivel = entity.getQuantidadeDisponivel();
        this.dataEntrada = entity.getDataEntrada();
        this.dataValidade = entity.getDataValidade();
        this.ativo = entity.getAtivo();
    }
}
