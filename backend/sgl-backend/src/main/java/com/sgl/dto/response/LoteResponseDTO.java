package com.sgl.dto.response;

import java.time.LocalDate;
import java.util.UUID;

import com.sgl.model.Lote;
import com.sgl.model.enums.UnidadeMedida;

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

    private UUID id;
    private UUID estoqueCentralId;
    private UUID produtoId;
    private String produtoNome;
    private UUID unidadeId;
    private String unidadeNome;
    private String numeroLote;

    @Schema(description = "Apresentação física registrada para o lote.", example = "kit")
    private String apresentacao;
    @Schema(description = "Quantidade de apresentações físicas recebidas.", example = "2")
    private Integer quantidadeApresentacoes;
    @Schema(description = "Conteúdo em unidade-base por apresentação.", example = "50")
    private Integer conteudoPorApresentacao;
    @Schema(description = "Indica se a apresentação permite saída parcial.", example = "true")
    private Boolean fracionavel;
    @Schema(description = "Unidade-base de controle do produto.", example = "UNIDADE")
    private UnidadeMedida unidadeBase;

    @Schema(description = "Quantidade inicial do lote, sempre na unidade-base do produto.", example = "100")
    private Integer quantidadeInicial;
    @Schema(description = "Quantidade disponível no lote, sempre na unidade-base do produto.", example = "80")
    private Integer quantidadeDisponivel;
    private LocalDate dataEntrada;
    private LocalDate dataValidade;
    private Boolean ativo;

    public LoteResponseDTO(Lote entity) {
        this.id = entity.getPublicId();
        this.estoqueCentralId = entity.getEstoqueCentral().getPublicId();
        this.produtoId = entity.getEstoqueCentral().getProduto().getPublicId();
        this.produtoNome = entity.getEstoqueCentral().getProduto().getNome();
        this.unidadeId = entity.getEstoqueCentral().getUnidade().getPublicId();
        this.unidadeNome = entity.getEstoqueCentral().getUnidade().getNome();
        this.numeroLote = entity.getNumeroLote();
        this.apresentacao = entity.getApresentacao();
        this.quantidadeApresentacoes = entity.getQuantidadeApresentacoes();
        this.conteudoPorApresentacao = entity.getConteudoPorApresentacao();
        this.fracionavel = entity.getFracionavel();
        this.unidadeBase = entity.getEstoqueCentral().getProduto().getUnidadeMedida();
        this.quantidadeInicial = entity.getQuantidadeInicial();
        this.quantidadeDisponivel = entity.getQuantidadeDisponivel();
        this.dataEntrada = entity.getDataEntrada();
        this.dataValidade = entity.getDataValidade();
        this.ativo = entity.getAtivo();
    }
}
