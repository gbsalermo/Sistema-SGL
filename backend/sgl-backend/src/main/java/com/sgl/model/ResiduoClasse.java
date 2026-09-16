package com.sgl.model;

import java.io.Serializable;

import com.sgl.model.enums.EtapaClassificacaoResiduo;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


//Classe responsavel por persistir o historico da classificação dos residuos
/*
 * EX:
 * 2026
Classe A = "Solventes sem halogênios"

Resíduo R001
→ Classe A

*SUPONDO QUE ALGUEM ALTERE A CLASSE A, O RESIDUO R001 CONTINUARA MOSTRANDO A MESMA CLASSIFICAÇÃO
 */
@Entity
@Table(
        name = "residuo_classes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_residuo_classe_etapa",
                        columnNames = {
                                "residuo_id",
                                "classe_residuo_id",
                                "etapa"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResiduoClasse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "residuo_id", nullable = false)
    private Residuo residuo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classe_residuo_id", nullable = false)
    private ClasseResiduo classeResiduo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EtapaClassificacaoResiduo etapa;

    @Column(name = "codigo_snapshot", nullable = false, length = 30)
    private String codigoSnapshot;

    @Column(name = "descricao_snapshot", nullable = false, length = 500)
    private String descricaoSnapshot;

    public static ResiduoClasse criar(
            Residuo residuo,
            ClasseResiduo classe,
            EtapaClassificacaoResiduo etapa) {

        return ResiduoClasse.builder()
                .residuo(residuo)
                .classeResiduo(classe)
                .etapa(etapa)
                .codigoSnapshot(classe.getCodigo())
                .descricaoSnapshot(classe.getDescricao())
                .build();
    }
}