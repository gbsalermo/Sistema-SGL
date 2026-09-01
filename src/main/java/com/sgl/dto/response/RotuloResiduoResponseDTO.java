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
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "Dados consolidados para montar e imprimir o rótulo físico do resíduo.")
@Getter
public class RotuloResiduoResponseDTO {

    private final UUID residuoId;
    private final String codigoRastreio;
    private final String qrCodeConteudo;
    private final String descricao;
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
        this.descricao = entity.getDescricao();
        this.laboratorioNome = entity.getLaboratorio().getNome();
        this.geradorNome = entity.getGerador().getNome();
        this.processoOrigem = entity.getProcessoOrigem();
        this.recipiente = entity.getRecipiente();
        this.quantidade = entity.getQuantidade();
        this.unidadeMedida = entity.getUnidadeMedida();
        this.nivelRisco = entity.getNivelRiscoConfirmado() != null
                ? entity.getNivelRiscoConfirmado()
                : entity.getNivelRiscoInformado();
        this.riscos = !entity.getRiscosConfirmados().isEmpty()
                ? new LinkedHashSet<>(entity.getRiscosConfirmados())
                : new LinkedHashSet<>(entity.getRiscosInformados());
        this.componentes = entity.getComponentes().stream()
                .map(ComponenteResiduoResponseDTO::new)
                .toList();
        this.localArmazenamentoTemporario = entity.getLocalArmazenamentoTemporario();
        this.destinoFinalPrevisto = entity.getDestinoFinalPrevisto();
        this.dataPrevistaDespacho = entity.getDataPrevistaDespacho();
        this.dataRotulagem = entity.getDataLiberacao();
    }
}
