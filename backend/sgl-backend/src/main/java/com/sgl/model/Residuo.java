package com.sgl.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.sgl.exception.BusinessRuleException;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.StatusResiduo;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "residuos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Residuo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laboratorio_id", nullable = false)
    @ToString.Exclude
    private Laboratorio laboratorio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gerador_id", nullable = false)
    @ToString.Exclude
    private Usuario gerador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id")
    @ToString.Exclude
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gestor_responsavel_id")
    @ToString.Exclude
    private Usuario gestorResponsavel;

    @Column(nullable = false, length = 1000)
    private String descricao;

    @Column(name = "processo_origem", nullable = false, length = 1000)
    private String processoOrigem;

    @Column(nullable = false)
    private String recipiente;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_medida", nullable = false)
    private UnidadeMedida unidadeMedida;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_risco_informado", nullable = false)
    private NivelRisco nivelRiscoInformado;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "residuo_riscos_informados",
            joinColumns = @JoinColumn(name = "residuo_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "risco", nullable = false)
    @Builder.Default
    private Set<TipoRisco> riscosInformados = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_risco_confirmado")
    private NivelRisco nivelRiscoConfirmado;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "residuo_riscos_confirmados",
            joinColumns = @JoinColumn(name = "residuo_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "risco", nullable = false)
    @Builder.Default
    private Set<TipoRisco> riscosConfirmados = new LinkedHashSet<>();

    @Column(name = "observacao_gerador", length = 1000)
    private String observacaoGerador;

    @Column(name = "observacao_gestor", length = 1000)
    private String observacaoGestor;

    @Column(name = "local_armazenamento_temporario")
    private String localArmazenamentoTemporario;

    @Column(name = "destino_final_previsto", length = 500)
    private String destinoFinalPrevisto;

    @Column(name = "destino_final_confirmado", length = 500)
    private String destinoFinalConfirmado;

    @Column(name = "codigo_rastreio", unique = true, length = 80)
    private String codigoRastreio;

    @Column(name = "qr_code_conteudo", length = 500)
    private String qrCodeConteudo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusResiduo status;

    @Column(name = "data_informacao", nullable = false)
    private LocalDateTime dataInformacao;

    @Column(name = "data_recebimento")
    private LocalDateTime dataRecebimento;

    @Column(name = "data_liberacao")
    private LocalDateTime dataLiberacao;

    @Column(name = "data_armazenamento_temporario")
    private LocalDateTime dataArmazenamentoTemporario;

    @Column(name = "data_prevista_despacho")
    private LocalDate dataPrevistaDespacho;

    @Column(name = "data_despacho")
    private LocalDateTime dataDespacho;

    @OneToMany(mappedBy = "residuo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ComponenteResiduo> componentes = new ArrayList<>();

    public void addComponente(ComponenteResiduo componente) {
        componente.setResiduo(this);
        componentes.add(componente);
    }

    public void receber(Usuario gestor, String observacao) {
        requireStatus(StatusResiduo.INFORMADO, "recebido para análise");
        this.gestorResponsavel = gestor;
        this.dataRecebimento = LocalDateTime.now();
        this.status = StatusResiduo.EM_ANALISE;

        if (observacao != null && !observacao.isBlank()) {
            this.observacaoGestor = observacao;
        }
    }

    public void liberarParaArmazenamento(
            Usuario gestor,
            NivelRisco nivelConfirmado,
            Set<TipoRisco> riscosConfirmados,
            String localArmazenamento,
            String destinoPrevisto,
            LocalDate dataPrevistaDespacho,
            String observacao) {

        requireStatus(StatusResiduo.EM_ANALISE, "liberado para armazenamento");

        if (nivelConfirmado == null) {
            throw new BusinessRuleException("O nível de risco confirmado é obrigatório.");
        }

        if (localArmazenamento == null || localArmazenamento.isBlank()) {
            throw new BusinessRuleException("O local de armazenamento temporário é obrigatório.");
        }

        if (destinoPrevisto == null || destinoPrevisto.isBlank()) {
            throw new BusinessRuleException("O destino final previsto é obrigatório.");
        }

        this.gestorResponsavel = gestor;
        this.nivelRiscoConfirmado = nivelConfirmado;
        this.riscosConfirmados.clear();
        if (riscosConfirmados != null) {
            this.riscosConfirmados.addAll(riscosConfirmados);
        }
        this.localArmazenamentoTemporario = localArmazenamento;
        this.destinoFinalPrevisto = destinoPrevisto;
        this.dataPrevistaDespacho = dataPrevistaDespacho;
        this.observacaoGestor = observacao;
        this.dataLiberacao = LocalDateTime.now();
        this.status = StatusResiduo.LIBERADO_PARA_ARMAZENAMENTO;
    }

    public void confirmarArmazenamento(Usuario gestor, String localArmazenamento) {
        requireStatus(
                StatusResiduo.LIBERADO_PARA_ARMAZENAMENTO,
                "armazenado temporariamente"
        );

        this.gestorResponsavel = gestor;
        if (localArmazenamento != null && !localArmazenamento.isBlank()) {
            this.localArmazenamentoTemporario = localArmazenamento;
        }
        this.dataArmazenamentoTemporario = LocalDateTime.now();
        this.status = StatusResiduo.ARMAZENADO_TEMPORARIAMENTE;
    }

    public void confirmarDespacho(Usuario gestor, String destinoFinal, String observacao) {
        requireStatus(StatusResiduo.ARMAZENADO_TEMPORARIAMENTE, "despachado");

        if (destinoFinal == null || destinoFinal.isBlank()) {
            throw new BusinessRuleException("O destino final confirmado é obrigatório.");
        }

        this.gestorResponsavel = gestor;
        this.destinoFinalConfirmado = destinoFinal;
        if (observacao != null && !observacao.isBlank()) {
            this.observacaoGestor = observacao;
        }
        this.dataDespacho = LocalDateTime.now();
        this.status = StatusResiduo.DESPACHADO;
    }

    public void validateLabelAvailable() {
        if (codigoRastreio == null || qrCodeConteudo == null) {
            throw new BusinessRuleException(
                    "O rótulo só fica disponível após a análise e liberação do resíduo."
            );
        }
    }

    private void requireStatus(StatusResiduo expected, String action) {
        if (status != expected) {
            throw new BusinessRuleException(
                    "O resíduo só pode ser " + action + " quando estiver em " + expected
                            + ". Status atual: " + status
            );
        }
    }

    @PrePersist
    private void generateDefaults() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        if (dataInformacao == null) {
            dataInformacao = LocalDateTime.now();
        }
        if (status == null) {
            status = StatusResiduo.INFORMADO;
        }
    }
}
