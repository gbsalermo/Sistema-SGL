package com.sgl.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.sgl.model.Residuo;
import com.sgl.model.enums.EstadoFisicoResiduo;
import com.sgl.model.enums.MedidaSeguranca;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.StatusResiduo;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "Representação completa do resíduo e do seu fluxo de rastreabilidade.")
@Getter
public class ResiduoResponseDTO {

    private final UUID id;
    private final String codigoRastreio;
    private final StatusResiduo status;

    private final UUID laboratorioId;
    private final String laboratorioNome;
    private final UUID usuarioGeradorId;
    private final String usuarioGeradorNome;
    private final UUID projetoId;
    private final String projetoNome;


    private final String descricao;
    private final String processoOrigem;
    private final String recipiente;
    private final BigDecimal quantidade;
    private final UnidadeMedida unidadeMedida;

    private final NivelRisco nivelRiscoInformado;
    private final Set<TipoRisco> riscosInformados;
    private final NivelRisco nivelRiscoConfirmado;
    private final Set<TipoRisco> riscosConfirmados;

    private final String observacaoGerador;
    private final String observacaoGestor;
    private final String localArmazenamentoTemporario;
    private final String destinoFinalPrevisto;
    private final String destinoFinalConfirmado;
    private final String qrCodeConteudo;

    private final LocalDateTime dataInformacao;
    private final LocalDateTime dataRecebimento;
    private final LocalDateTime dataLiberacao;
    private final LocalDateTime dataArmazenamentoTemporario;
    private final LocalDate dataPrevistaDespacho;
    private final LocalDateTime dataDespacho;
    
    private final UUID gestorRecebedorInicialId;
    private final String gestorRecebedorInicialNome;

    private final EstadoFisicoResiduo estadoFisico;
    private final Boolean tratamentoRealizado;
    private final String descricaoTratamento;

    private final List<ComponenteResiduoResponseDTO> componentes;
    
    private final List<ClasseResiduoSnapshotResponseDTO>
    classesInformadas;

    private final List<ClasseResiduoSnapshotResponseDTO>
    classesConfirmadas;

    private final Set<MedidaSeguranca> medidasSegurancaInformadas;
    private final String observacaoSegurancaInformada;

    private final Set<MedidaSeguranca> medidasSegurancaConfirmadas;
    private final String observacaoSegurancaConfirmada;
    
    private final UUID localArmazenamentoResiduoId;
    private final String complementoLocalArmazenamento;
    
    
    public ResiduoResponseDTO(Residuo entity) {
        this.id = entity.getPublicId();
        this.codigoRastreio = entity.getCodigoRastreio();
        this.status = entity.getStatus();

        this.laboratorioId = entity.getLaboratorio().getPublicId();
        this.laboratorioNome = entity.getLaboratorio().getNome();

        this.usuarioGeradorId = entity.getGerador().getPublicId();
        this.usuarioGeradorNome = entity.getGerador().getNome();

        this.projetoId = entity.getProjeto() != null
                ? entity.getProjeto().getPublicId()
                : null;

        this.projetoNome = entity.getProjeto() != null
                ? entity.getProjeto().getNome()
                : null;

        // Gestor que recebeu inicialmente o resíduo.
        this.gestorRecebedorInicialId = entity.getGestorRecebedorInicial() != null
                ? entity.getGestorRecebedorInicial().getPublicId()
                : null;

        this.gestorRecebedorInicialNome = entity.getGestorRecebedorInicial() != null
                ? entity.getGestorRecebedorInicial().getNome()
                : null;

        this.descricao = entity.getDescricao();
        this.processoOrigem = entity.getProcessoOrigem();

        this.estadoFisico = entity.getEstadoFisico();
        this.tratamentoRealizado = entity.getTratamentoRealizado();
        this.descricaoTratamento = entity.getDescricaoTratamento();

        this.recipiente = entity.getRecipiente();
        this.quantidade = entity.getQuantidade();
        this.unidadeMedida = entity.getUnidadeMedida();

        this.nivelRiscoInformado = entity.getNivelRiscoInformado();
        this.riscosInformados = new LinkedHashSet<>(
                entity.getRiscosInformados()
        );

        this.nivelRiscoConfirmado = entity.getNivelRiscoConfirmado();
        this.riscosConfirmados = new LinkedHashSet<>(
                entity.getRiscosConfirmados()
        );

        this.observacaoGerador = entity.getObservacaoGerador();
        this.observacaoGestor = entity.getObservacaoGestor();

        this.destinoFinalPrevisto =
                entity.getDestinoFinalPrevisto();

        this.destinoFinalConfirmado =
                entity.getDestinoFinalConfirmado();

        this.qrCodeConteudo = entity.getQrCodeConteudo();

        this.dataInformacao = entity.getDataInformacao();
        this.dataRecebimento = entity.getDataRecebimento();
        this.dataLiberacao = entity.getDataLiberacao();

        this.dataArmazenamentoTemporario =
                entity.getDataArmazenamentoTemporario();

        this.dataPrevistaDespacho =
                entity.getDataPrevistaDespacho();

        this.dataDespacho = entity.getDataDespacho();

        this.componentes = entity.getComponentes()
                .stream()
                .map(ComponenteResiduoResponseDTO::new)
                .toList();
        
        this.classesInformadas =
                entity.getClassesInformadas()
                        .stream()
                        .map(ClasseResiduoSnapshotResponseDTO::new)
                        .toList();

        this.classesConfirmadas =
                entity.getClassesConfirmadas()
                        .stream()
                        .map(ClasseResiduoSnapshotResponseDTO::new)
                        .toList();
        
        this.medidasSegurancaInformadas =
                new LinkedHashSet<>(
                        entity.getMedidasSegurancaInformadas()
                );

        this.observacaoSegurancaInformada =
                entity.getObservacaoSegurancaInformada();

        this.medidasSegurancaConfirmadas =
                new LinkedHashSet<>(
                        entity.getMedidasSegurancaConfirmadas()
                );

        this.observacaoSegurancaConfirmada =
                entity.getObservacaoSegurancaConfirmada();
        
        this.localArmazenamentoResiduoId =
                entity.getLocalArmazenamentoResiduo() != null
                        ? entity.getLocalArmazenamentoResiduo()
                                .getPublicId()
                        : null;

        this.complementoLocalArmazenamento =
                entity.getComplementoLocalArmazenamento();

        this.localArmazenamentoTemporario =
                entity.getLocalArmazenamentoTemporario();
    }
}
