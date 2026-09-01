package com.sgl.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Informações fornecidas pelo laboratório ao comunicar a geração e entrega de um resíduo.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CriarResiduoRequestDTO {

    @NotNull(message = "O usuário gerador é obrigatório")
    private UUID usuarioGeradorId;

    @NotNull(message = "O laboratório gerador é obrigatório")
    private UUID laboratorioId;

    @Schema(description = "Projeto relacionado à geração do resíduo, quando aplicável.")
    private UUID projetoId;

    @NotBlank(message = "A descrição do resíduo é obrigatória")
    @Schema(example = "Resíduo líquido do processo de extração de DNA")
    private String descricao;

    @NotBlank(message = "O processo de origem é obrigatório")
    @Schema(example = "Extração de DNA vegetal")
    private String processoOrigem;

    @NotBlank(message = "O recipiente é obrigatório")
    @Schema(example = "Bombona plástica de 5 L")
    private String recipiente;

    @NotNull(message = "A quantidade do resíduo é obrigatória")
    @DecimalMin(value = "0.001", message = "A quantidade do resíduo deve ser maior que zero")
    private BigDecimal quantidade;

    @NotNull(message = "A unidade de medida é obrigatória")
    private UnidadeMedida unidadeMedida;

    @NotNull(message = "O nível de risco informado é obrigatório")
    private NivelRisco nivelRiscoInformado;

    @NotNull(message = "Os riscos informados são obrigatórios")
    private Set<TipoRisco> riscosInformados;

    @Schema(description = "Observação do usuário que gerou ou entregou o resíduo.")
    private String observacaoGerador;

    @Valid
    @NotEmpty(message = "O resíduo deve possuir pelo menos um componente informado")
    private List<ComponenteResiduoRequestDTO> componentes;
}
