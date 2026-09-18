package com.sgl.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.sgl.model.Residuo;
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
        this.processoOrigem = entity.getProcessoOrigem();
        this.recipiente = entity.getRecipiente();
        this.quantidade = entity.getQuantidade();
        this.unidadeMedida = entity.getUnidadeMedida();
        this.nivelRisco = entity.getNivelRiscoConfirmado() != null ? entity.getNivelRiscoConfirmado() : entity.getNivelRiscoInformado();
        this.riscos = !entity.getRiscosConfirmados().isEmpty()
                ? new LinkedHashSet<>(entity.getRiscosConfirmados())
                : new LinkedHashSet<>(entity.getRiscosInformados());
        this.componentes = entity.getComponentes().stream().map(ComponenteResiduoResponseDTO::new).toList();
        this.localArmazenamentoTemporario = entity.getLocalArmazenamentoTemporario();
        this.destinoFinalPrevisto = entity.getDestinoFinalPrevisto();
        this.dataPrevistaDespacho = entity.getDataPrevistaDespacho();
        this.dataRotulagem = entity.getDataLiberacao();
    }
}
