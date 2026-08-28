package com.sgl.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import com.sgl.exception.BusinessRuleException;
import com.sgl.model.enums.TipoEmbalagem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "lote",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_lote_estoque_numero",
                        columnNames = {"estoque_central_id", "numero_lote"}
                ),
                @UniqueConstraint(
                        name = "uk_lote_codigo_interno",
                        columnNames = {"codigo_interno"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lote implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estoque_central_id", nullable = false)
    private EstoqueCentral estoqueCentral;

    /**
     * Identificador interno gerado pelo SGL. Nunca deve ser alterado depois
     * da criação do lote. Ex.: LOT-EXT-DNA-PL-001.
     */
    @Column(name = "codigo_interno", nullable = false, unique = true, updatable = false, length = 160)
    @Setter(lombok.AccessLevel.NONE)
    private String codigoInterno;

    /** Sequência do lote dentro do produto, usada para gerar codigoInterno. */
    @Column(name = "sequencial_interno", nullable = false, updatable = false)
    @Setter(lombok.AccessLevel.NONE)
    private Integer sequencialInterno;

    /**
     * Identificação externa informada pelo fornecedor/responsável.
     * É separada do código interno imutável do SGL.
     */
    @Column(name = "numero_lote", nullable = false, length = 100)
    private String numeroLote;

    /** Categoria física principal usada na interface: KIT, CAIXA, GARRAFA etc. */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_embalagem", nullable = false, length = 30)
    private TipoEmbalagem tipoEmbalagem = TipoEmbalagem.UNITARIO;

    /**
     * Especificação livre da embalagem. Ex.: "kit com 50 unidades" ou
     * "garrafa de 1 L".
     */
    @Column(length = 120)
    private String apresentacao;

    private Integer quantidadeApresentacoes;

    /**
     * Multiplicador interno usado para converter uma embalagem em unidades
     * individuais do produto. Ex.: kit de 50 -> multiplicador 50.
     */
    private Integer conteudoPorApresentacao;

    private Boolean fracionavel;

    @Column(length = 500)
    private String observacao;

    @Column(nullable = false)
    private Integer quantidadeInicial;

    @Column(nullable = false)
    @Setter(lombok.AccessLevel.NONE)
    private Integer quantidadeDisponivel;

    @Column(nullable = false)
    private LocalDate dataEntrada;

    private LocalDate dataValidade;

    @Column(nullable = false)
    private Boolean ativo = true;

    public void definirCodigoInterno(String codigoInterno, Integer sequencialInterno) {
        if (this.codigoInterno != null || this.sequencialInterno != null) {
            throw new BusinessRuleException("O código interno do lote é imutável.");
        }
        if (codigoInterno == null || codigoInterno.isBlank() || sequencialInterno == null || sequencialInterno <= 0) {
            throw new BusinessRuleException("Código interno e sequência do lote são obrigatórios.");
        }
        this.codigoInterno = codigoInterno;
        this.sequencialInterno = sequencialInterno;
    }

    public int fatorApresentacao() {
        return conteudoPorApresentacao == null || conteudoPorApresentacao <= 0
                ? 1
                : conteudoPorApresentacao;
    }

    public boolean permiteFracionamento() {
        return fracionavel == null || Boolean.TRUE.equals(fracionavel);
    }

    public void setQuantidadeDisponivel(Integer quantidadeDisponivel) {
        if (quantidadeDisponivel != null
                && quantidadeDisponivel >= 0
                && !permiteFracionamento()
                && quantidadeDisponivel % fatorApresentacao() != 0) {
            throw new BusinessRuleException(
                    "Este lote não permite fracionamento. A saída deve respeitar a embalagem completa de "
                            + fatorApresentacao() + " unidade(s)."
            );
        }
        this.quantidadeDisponivel = quantidadeDisponivel;
    }

    @PrePersist
    private void generatePublicId() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        if (tipoEmbalagem == null) {
            tipoEmbalagem = TipoEmbalagem.UNITARIO;
        }
    }
}
