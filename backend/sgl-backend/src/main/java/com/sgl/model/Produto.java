package com.sgl.model;

import java.io.Serializable;
import java.time.LocalDate;

import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.TipoPerecivel;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catálogo global dos materiais controlados pelo SGL.
 *
 * <p>Esta entidade descreve o produto, mas não armazena saldo. As quantidades
 * disponíveis são mantidas em {@link EstoqueCentral}, separadas por Unidade.</p>
 */
@Entity
@Table(name = "produtos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String descricao;

    /** Código único usado para identificação interna ou referência externa. */
    @Column(unique = true)
    private String codigoReferencia;

    /** Unidade utilizada para expressar as quantidades movimentadas. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnidadeMedida unidadeMedida;

    /** Local físico sugerido, como sala, armário ou prateleira. */
    private String localizacaoFisica;

    /** Severidade geral do risco associado ao produto. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelRisco risco = NivelRisco.NENHUM;

    /** Natureza do risco quando o nível informado não é NENHUM. */
    @Enumerated(EnumType.STRING)
    private TipoRisco tipoRisco;

    /** Orientação complementar de risco não representada pelos enums. */
    private String descricaoRisco;

    @Column(nullable = false)
    private Boolean perecivel = false;

    /**
     * Validade atualmente associada ao cadastro do produto.
     * Futuramente pode ser movida para uma entidade de lote.
     */
    private LocalDate dataValidade;

    @Enumerated(EnumType.STRING)
    private TipoPerecivel tipoPerecivel;

    private String condicoesArmazenamento;

    /**
     * Apresentação física do item, por exemplo: frasco de 1 L, caixa com 100
     * unidades ou saco de 5 kg. Contextualiza o valor armazenado no estoque.
     */
    private String unidadeArmazenamento;

    /** Permite inativar o catálogo sem remover vínculos históricos. */
    @Column(nullable = false)
    private Boolean ativo = true;
}
