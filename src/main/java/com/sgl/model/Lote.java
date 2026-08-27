package com.sgl.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import com.sgl.exception.BusinessRuleException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(name = "numero_lote", nullable = false, length = 100)
    private String numeroLote;

    @Column(length = 120)
    private String apresentacao;

    private Integer quantidadeApresentacoes;

    private Integer conteudoPorApresentacao;

    private Boolean fracionavel;

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

    public int fatorApresentacao() {
        return conteudoPorApresentacao == null || conteudoPorApresentacao <= 0
                ? 1
                : conteudoPorApresentacao;
    }

    public boolean permiteFracionamento() {
        return fracionavel == null || Boolean.TRUE.equals(fracionavel);
    }

    /**
     * O saldo do lote é sempre armazenado na unidade-base do produto.
     * Para apresentação não fracionável, o saldo só pode variar em múltiplos
     * inteiros do conteúdo por apresentação.
     */
    public void setQuantidadeDisponivel(Integer quantidadeDisponivel) {
        if (quantidadeDisponivel != null
                && quantidadeDisponivel >= 0
                && !permiteFracionamento()
                && quantidadeDisponivel % fatorApresentacao() != 0) {
            throw new BusinessRuleException(
                    "Este lote não permite fracionamento. A saída deve respeitar a apresentação completa de "
                            + fatorApresentacao() + " unidade(s)-base."
            );
        }
        this.quantidadeDisponivel = quantidadeDisponivel;
    }

    @PrePersist
    private void generatePublicId() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }
}
