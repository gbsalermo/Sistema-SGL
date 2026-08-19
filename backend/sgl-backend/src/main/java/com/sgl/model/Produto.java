package com.sgl.model;

import java.io.Serializable;
import java.util.UUID;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Catálogo global dos materiais controlados pelo SGL. */
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
    
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
	private UUID publicId;

    @Column(nullable = false)
    private String nome;

    private String descricao;

    @Column(unique = true)
    private String codigoReferencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnidadeMedida unidadeMedida;

    private String localizacaoFisica;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelRisco risco = NivelRisco.NENHUM;

    @Enumerated(EnumType.STRING)
    private TipoRisco tipoRisco;

    private String descricaoRisco;

    /** Define se os lotes deste produto exigem controle de validade. */
    @Column(nullable = false)
    private Boolean perecivel = false;

    @Enumerated(EnumType.STRING)
    private TipoPerecivel tipoPerecivel;

    private String condicoesArmazenamento;

    private String unidadeArmazenamento;

    @Column(nullable = false)
    private Boolean ativo = true;
    
    @PrePersist
	private void gerarPublicId() {
		if(publicId == null) {
			publicId = UUID.randomUUID();
		}
	}
}
