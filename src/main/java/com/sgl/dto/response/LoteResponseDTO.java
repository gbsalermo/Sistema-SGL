package com.sgl.dto.response;

import java.time.LocalDate;
import java.util.UUID;

import com.sgl.model.Lote;
import com.sgl.model.enums.TipoEmbalagem;
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

    @Schema(description = "Código interno imutável gerado pelo SGL.", example = "LOT-EXT-DNA-PL-001")
    private String codigoInterno;
    @Schema(description = "Número/lote informado pelo fornecedor ou responsável.", example = "FAB-2026-8841")
    private String numeroLote;

    @Schema(description = "Tipo principal da embalagem.", example = "KIT")
    private TipoEmbalagem tipoEmbalagem;
    @Schema(description = "Especificação livre da embalagem.", example = "kit com 50 unidades")
    private String apresentacao;
    @Schema(description = "Quantidade de embalagens/unidades físicas recebidas.", example = "2")
    private Integer quantidadeApresentacoes;
    @Schema(description = "Multiplicador de unidades individuais por embalagem.", example = "50")
    private Integer conteudoPorApresentacao;
    @Schema(description = "Indica se a embalagem permite saída parcial.", example = "true")
    private Boolean fracionavel;
    @Schema(description = "Observação cadastral do lote.", example = "Material recebido lacrado.")
    private String observacao;
    @Schema(description = "Unidade interna de controle do produto.", example = "UNIDADE")
    private UnidadeMedida unidadeBase;

    @Schema(description = "Quantidade inicial convertida para unidades individuais do produto.", example = "100")
    private Integer quantidadeInicial;
    @Schema(description = "Quantidade disponível convertida para unidades individuais do produto.", example = "80")
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
        this.codigoInterno = entity.getCodigoInterno();
        this.numeroLote = entity.getNumeroLote();
        this.tipoEmbalagem = entity.getTipoEmbalagem();
        this.apresentacao = entity.getApresentacao();
        this.quantidadeApresentacoes = entity.getQuantidadeApresentacoes();
        this.conteudoPorApresentacao = entity.getConteudoPorApresentacao();
        this.fracionavel = entity.getFracionavel();
        this.observacao = entity.getObservacao();
        this.unidadeBase = entity.getEstoqueCentral().getProduto().getUnidadeMedida();
        this.quantidadeInicial = entity.getQuantidadeInicial();
        this.quantidadeDisponivel = entity.getQuantidadeDisponivel();
        this.dataEntrada = entity.getDataEntrada();
        this.dataValidade = entity.getDataValidade();
        this.ativo = entity.getAtivo();
    }
}
