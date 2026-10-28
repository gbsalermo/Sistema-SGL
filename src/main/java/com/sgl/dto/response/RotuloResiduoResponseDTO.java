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

@Schema(description = "Dados para pré-visualização do rótulo. A autorização de impressão é informada separadamente pelo campo impressaoPermitida.")
@Getter
public class RotuloResiduoResponseDTO {

    private final UUID residuoId;
    private final String codigoRastreio;
    private final String qrCodeConteudo;
    private final StatusResiduo status;
    private final boolean impressaoPermitida;
    private final boolean classificacaoConfirmada;
    private final String descricao;
    private final UUID unidadeId;
    private final String unidadeNome;
    private final String unidadeSigla;
    private final String laboratorioNome;
    private final String geradorNome;
    private final LocalDateTime dataInformacao;
    private final EstadoFisicoResiduo estadoFisico;
    private final Set<MedidaSeguranca> medidasSeguranca;
    private final String observacaoSeguranca;
    private final String observacoesRotulo;
    private final String processoOrigem;
    private final String recipiente;
    private final BigDecimal quantidade;
    private final UnidadeMedida unidadeMedida;
    private final NivelRisco nivelRisco;
    private final Set<TipoRisco> riscos;
    private final List<ComponenteResiduoResponseDTO> componentes;
    private final String localArmazenamentoTemporario;
    private final String destinoFinalPrevisto;
    private final LocalDate dataPrevistaDespacho;
    private final LocalDateTime dataRotulagem;

    public RotuloResiduoResponseDTO(Residuo entity) {
        this.residuoId = entity.getPublicId();
        this.codigoRastreio = entity.getCodigoRastreio();
        this.qrCodeConteudo = entity.getQrCodeConteudo();
        this.status = entity.getStatus();
        this.impressaoPermitida = entity.isImpressaoRotuloPermitida();
        this.classificacaoConfirmada = entity.getNivelRiscoConfirmado() != null;
        this.descricao = entity.getDescricao();
        // Correção de bug: antes lia sempre a unidade ATUAL do laboratório
        // (entity.getLaboratorio().getUnidade()...), que pode ter mudado
        // depois que o resíduo foi criado. Agora usa o snapshot guardado no
        // momento da criação (ver Residuo.java e ResiduoService.criar()).
        // O "fallback" para o vínculo ao vivo é só para resíduos antigos que
        // por algum motivo não tenham o snapshot preenchido (não deveria
        // acontecer após a migração V13, mas evita um NullPointerException
        // caso aconteça).
        this.unidadeId = entity.getUnidadeIdSnapshot() != null
                ? entity.getUnidadeIdSnapshot()
                : entity.getLaboratorio().getUnidade().getPublicId();
        this.unidadeNome = entity.getUnidadeNomeSnapshot() != null
                ? entity.getUnidadeNomeSnapshot()
                : entity.getLaboratorio().getUnidade().getNome();
        this.unidadeSigla = entity.getUnidadeSiglaSnapshot() != null
                ? entity.getUnidadeSiglaSnapshot()
                : entity.getLaboratorio().getUnidade().getSigla();
        this.laboratorioNome = entity.getLaboratorio().getNome();
        this.geradorNome = entity.getGerador().getNome();
        this.dataInformacao = entity.getDataInformacao();
        this.estadoFisico = entity.getEstadoFisico();

        // O rótulo distingue as informações preliminares daquelas confirmadas.
        // Uma confirmação vazia não deve voltar a exibir medidas preliminares.
        this.medidasSeguranca = this.classificacaoConfirmada
                ? new LinkedHashSet<>(entity.getMedidasSegurancaConfirmadas())
                : new LinkedHashSet<>(entity.getMedidasSegurancaInformadas());
        this.observacaoSeguranca = this.classificacaoConfirmada
                ? entity.getObservacaoSegurancaConfirmada()
                : entity.getObservacaoSegurancaInformada();

        String observacaoGerador = entity.getObservacaoGerador();
        String observacaoGestor = entity.getObservacaoGestor();
        if (observacaoGerador != null && !observacaoGerador.isBlank()
                && observacaoGestor != null && !observacaoGestor.isBlank()) {
            this.observacoesRotulo = "Gerador: " + observacaoGerador + " | Gestão: " + observacaoGestor;
        } else if (observacaoGestor != null && !observacaoGestor.isBlank()) {
            this.observacoesRotulo = "Gestão: " + observacaoGestor;
        } else if (observacaoGerador != null && !observacaoGerador.isBlank()) {
            this.observacoesRotulo = "Gerador: " + observacaoGerador;
        } else {
            this.observacoesRotulo = null;
        }
        this.processoOrigem = entity.getProcessoOrigem();
        this.recipiente = entity.getRecipiente();
        this.quantidade = entity.getQuantidade();
        this.unidadeMedida = entity.getUnidadeMedida();
        this.nivelRisco = entity.getNivelRiscoConfirmado() != null ? entity.getNivelRiscoConfirmado() : entity.getNivelRiscoInformado();
        this.riscos = this.classificacaoConfirmada
                ? new LinkedHashSet<>(entity.getRiscosConfirmados())
                : new LinkedHashSet<>(entity.getRiscosInformados());
        this.componentes = entity.getComponentes().stream().map(ComponenteResiduoResponseDTO::new).toList();
        this.localArmazenamentoTemporario = entity.getLocalArmazenamentoTemporario();
        this.destinoFinalPrevisto = entity.getDestinoFinalPrevisto();
        this.dataPrevistaDespacho = entity.getDataPrevistaDespacho();
        this.dataRotulagem = entity.getDataLiberacao();
    }
}
