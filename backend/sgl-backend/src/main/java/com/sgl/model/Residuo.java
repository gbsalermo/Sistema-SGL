package com.sgl.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.sgl.exception.BusinessRuleException;
import com.sgl.model.enums.EstadoFisicoResiduo;
import com.sgl.model.enums.EtapaClassificacaoResiduo;
import com.sgl.model.enums.MedidaSeguranca;
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
    @JoinColumn(name = "gestor_recebedor_inicial_id")
    @ToString.Exclude
    private Usuario gestorRecebedorInicial;

    @OneToMany(
            mappedBy = "residuo",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<ResiduoClasse> classificacoes = new ArrayList<>();
    
    
    @Column(nullable = false, length = 1000)
    private String descricao;

    @Column(name = "processo_origem", nullable = false, length = 1000)
    private String processoOrigem;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_fisico")
    private EstadoFisicoResiduo estadoFisico;
    
    @Column(name = "tratamento_realizado")
    private Boolean tratamentoRealizado;
    
    @Column(name = "descricao_tratamento", length = 1000)
    private String descricaoTratamento;
    
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
    
    /**
     * Snapshot das medidas de segurança declaradas no momento
     * em que o Resíduo foi informado.
     *
     * Esses dados pertencem à ocorrência real do Resíduo e não
     * devem ser recalculados quando o cadastro dos Produtos mudar.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "residuo_medidas_seguranca_informadas",
            joinColumns = @JoinColumn(name = "residuo_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "medida", nullable = false)
    @Builder.Default
    private Set<MedidaSeguranca> medidasSegurancaInformadas =
            new LinkedHashSet<>();

    @Column(name = "observacao_seguranca_informada", length = 1000)
    private String observacaoSegurancaInformada;


    /**
     * Snapshot das medidas de segurança confirmadas pela Gestão.
     *
     * Pode diferir da informação original do Solicitante sem
     * alterar o snapshot informado.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "residuo_medidas_seguranca_confirmadas",
            joinColumns = @JoinColumn(name = "residuo_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "medida", nullable = false)
    @Builder.Default
    private Set<MedidaSeguranca> medidasSegurancaConfirmadas =
            new LinkedHashSet<>();

    @Column(name = "observacao_seguranca_confirmada", length = 1000)
    private String observacaoSegurancaConfirmada;
    
    public void definirTratamento(Boolean tratamentoRealizado, String descricaoTratamento) {
    	
    	if(tratamentoRealizado == null) {
    		throw new BusinessRuleException(
    				"informe se o resíduo recebeu tratamento");
    	}
    	
    	this.tratamentoRealizado = tratamentoRealizado;
    	
    	if(!tratamentoRealizado) {
    		this.descricaoTratamento = null; //descrição é descartada se o tratamento for false
    		return;
    	}
    	
    	//caso seja o tratamento true, evita que o usuario ignore o tratamento
    	if (descricaoTratamento == null || descricaoTratamento.isBlank()) {
    		throw new BusinessRuleException(
    				"A descrição do tratamento é obrigatória quando o resíduo já foi tratado"
    				);
    	}
    	
    	this.descricaoTratamento = descricaoTratamento.trim();
    }

    public void addComponente(ComponenteResiduo componente) {
        componente.setResiduo(this);
        componentes.add(componente);
    }

    public void receber(Usuario gestor, String observacao) {
        requireStatus(StatusResiduo.INFORMADO, "recebido para análise");
        this.gestorRecebedorInicial = gestor;
        this.dataRecebimento = LocalDateTime.now();
        this.status = StatusResiduo.EM_ANALISE;

        if (observacao != null && !observacao.isBlank()) {
            this.observacaoGestor = observacao;
        }
    }

    private void validarGestorRecebedorInicial(Usuario gestor) {
    	
    	if(gestorRecebedorInicial == null) {
    		
    	throw new BusinessRuleException(
    			"O resíduo não possui gestor de recebimento inicial"
    			);
    }
    	
    if (gestor == null || !Objects.equals(gestorRecebedorInicial.getId(), gestor.getId())){
    	throw new BusinessRuleException(
    			"A análise deve ser realizada pelo gestor que recebeu inicialmente o resíduo"
    			);
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
        
        validarGestorRecebedorInicial(gestor);

        if (nivelConfirmado == null) {
            throw new BusinessRuleException("O nível de risco confirmado é obrigatório.");
        }

        if (localArmazenamento == null || localArmazenamento.isBlank()) {
            throw new BusinessRuleException("O local de armazenamento temporário é obrigatório.");
        }

        if (destinoPrevisto == null || destinoPrevisto.isBlank()) {
            throw new BusinessRuleException("O destino final previsto é obrigatório.");
        }

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

    public void confirmarArmazenamento(String localArmazenamento) {
        requireStatus(
                StatusResiduo.LIBERADO_PARA_ARMAZENAMENTO,
                "armazenado temporariamente"
        );

        if (localArmazenamento != null && !localArmazenamento.isBlank()) {
            this.localArmazenamentoTemporario = localArmazenamento;
        }
        this.dataArmazenamentoTemporario = LocalDateTime.now();
        this.status = StatusResiduo.ARMAZENADO_TEMPORARIAMENTE;
    }

    public void confirmarDespacho(String destinoFinal, String observacao) {
        requireStatus(StatusResiduo.ARMAZENADO_TEMPORARIAMENTE, "despachado");

        if (destinoFinal == null || destinoFinal.isBlank()) {
            throw new BusinessRuleException("O destino final confirmado é obrigatório.");
        }

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
    
    //Metodos para a classificação dos residuos
    public void definirClassesInformadas(
            List<ClasseResiduo> classes) {

        substituirClasses(
                EtapaClassificacaoResiduo.INFORMADA,
                classes
        );
    }

    public void definirClassesConfirmadas(
            List<ClasseResiduo> classes) {

        substituirClasses(
                EtapaClassificacaoResiduo.CONFIRMADA,
                classes
        );
    }

    private void substituirClasses(
            EtapaClassificacaoResiduo etapa,
            List<ClasseResiduo> classes) {

        if (classes == null || classes.isEmpty()) {
            throw new BusinessRuleException(
                    "Informe pelo menos uma classe de resíduo."
            );
        }

        classificacoes.removeIf(
                item -> item.getEtapa() == etapa
        );

        for (ClasseResiduo classe : classes) {
            classe.validateActive();

            classificacoes.add(
                    ResiduoClasse.criar(
                            this,
                            classe,
                            etapa
                    )
            );
        }
    }

    public List<ResiduoClasse> getClassesInformadas() {
        return classificacoes.stream()
                .filter(item ->
                        item.getEtapa()
                                == EtapaClassificacaoResiduo.INFORMADA
                )
                .toList();
    }

    public List<ResiduoClasse> getClassesConfirmadas() {
        return classificacoes.stream()
                .filter(item ->
                        item.getEtapa()
                                == EtapaClassificacaoResiduo.CONFIRMADA
                )
                .toList();
    }
    
    public void definirSegurancaInformada(
            Set<MedidaSeguranca> medidas,
            String observacao) {

        validarSeguranca(medidas, observacao);

        this.medidasSegurancaInformadas.clear();

        if (medidas != null) {
            this.medidasSegurancaInformadas.addAll(medidas);
        }

        this.observacaoSegurancaInformada =
                normalizarObservacao(observacao);
    }
    
    public void definirSegurancaConfirmada(
            Set<MedidaSeguranca> medidas,
            String observacao) {

        validarSeguranca(medidas, observacao);

        this.medidasSegurancaConfirmadas.clear();

        if (medidas != null) {
            this.medidasSegurancaConfirmadas.addAll(medidas);
        }

        this.observacaoSegurancaConfirmada =
                normalizarObservacao(observacao);
    }
    
    private void validarSeguranca(
            Set<MedidaSeguranca> medidas,
            String observacao) {

        if (medidas == null) {
            throw new BusinessRuleException(
                    "Informe as medidas de segurança do resíduo."
            );
        }

        if (medidas.contains(MedidaSeguranca.OUTRO)
                && (observacao == null || observacao.isBlank())) {

            throw new BusinessRuleException(
                    "Descreva a medida de segurança marcada como OUTRO."
            );
        }
    }
    
    private String normalizarObservacao(String observacao) {

        if (observacao == null || observacao.isBlank()) {
            return null;
        }

        return observacao.trim();
    }
}
