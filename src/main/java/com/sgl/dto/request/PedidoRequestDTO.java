package com.sgl.dto.request;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import com.sgl.model.enums.TipoPedido;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Dados necessários para criar um pedido de materiais.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PedidoRequestDTO {

    @Schema(description = "Identificador público UUID do usuário solicitante.", example = "550e8400-e29b-41d4-a716-446655440001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Id do usuário é obrigatório")
    private UUID usuarioId;

    @Schema(description = "Identificador público UUID do laboratório solicitante.", example = "550e8400-e29b-41d4-a716-446655440002", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Id do laboratório é obrigatório")
    private UUID laboratorioId;

    @Schema(description = "Identificador público UUID do projeto vinculado ao pedido, quando houver.", example = "550e8400-e29b-41d4-a716-446655440003")
    private UUID projetoId;

    @Schema(description = "Indica se o solicitante marcou o pedido como urgente. A urgência é informativa e não altera o fluxo do pedido.", example = "true")
    private Boolean urgente;

    @Schema(description = "Justificativa opcional de urgência mantida por compatibilidade. Novos clientes podem registrar esse contexto no campo observação.", example = "Experimento agendado para amanhã.")
    @Size(max = 500, message = "Motivo da urgência deve ter no máximo 500 caracteres")
    private String motivoUrgencia;

    @Schema(description = "Observação ou descrição opcional sobre o pedido. Quando o pedido for urgente, este campo também pode informar o motivo ou contexto da urgência.", example = "Materiais destinados ao experimento da próxima semana.")
    private String observacao;

    @Schema(description = "Referência opcional ao documento associado ao pedido.", example = "solicitacao-2026-08.pdf")
    private String arquivoDocumento;

    @Schema(description = "Itens e quantidades solicitadas no pedido.", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    private List<ItemPedidoRequestDTO> itens;

    @Schema(description = "PRODUTOS (padrao) ou SOLUCAO; Solucao e uma unica preparacao.")
    private TipoPedido tipo;

    @Size(max = 150)
    private String nomeSolucao;

    @Schema(description = "Modelo de Solucao predefinido. Quando selecionado, itens nao sao enviados.")
    private UUID modeloSolucaoId;

    @JsonIgnore
    @AssertTrue(message = "Informe componentes ou selecione modelo de Solução")
    public boolean isComposicaoPresente() {
        return (tipo == TipoPedido.SOLUCAO && modeloSolucaoId != null)
            || (itens != null && !itens.isEmpty());
    }

    // Construtor legado para preservar os clientes e testes ja existentes.
    public PedidoRequestDTO(UUID usuarioId, UUID laboratorioId, UUID projetoId,
            Boolean urgente, String motivoUrgencia, String observacao,
            String arquivoDocumento, List<ItemPedidoRequestDTO> itens) {
        this.usuarioId = usuarioId;
        this.laboratorioId = laboratorioId;
        this.projetoId = projetoId;
        this.urgente = urgente;
        this.motivoUrgencia = motivoUrgencia;
        this.observacao = observacao;
        this.arquivoDocumento = arquivoDocumento;
        this.itens = itens;
    }
}
