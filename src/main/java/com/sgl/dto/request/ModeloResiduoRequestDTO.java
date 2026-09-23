package com.sgl.dto.request;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.sgl.model.enums.EstadoFisicoResiduo;
import com.sgl.model.enums.MedidaSeguranca;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModeloResiduoRequestDTO {

    @NotNull(message = "A unidade é obrigatória")
    private UUID unidadeId;

    @NotBlank(message = "O nome do modelo é obrigatório")
    @Size(
            max = 150,
            message = "O nome do modelo deve possuir no máximo 150 caracteres"
    )
    private String nome;

    @NotBlank(message = "A descrição do modelo é obrigatória")
    @Size(
            max = 1000,
            message = "A descrição deve possuir no máximo 1000 caracteres"
    )
    private String descricao;

    @NotBlank(message = "O processo de origem é obrigatório")
    @Size(
            max = 1000,
            message = "O processo de origem deve possuir no máximo 1000 caracteres"
    )
    private String processoOrigem;

    @NotNull(message = "O estado físico é obrigatório")
    private EstadoFisicoResiduo estadoFisico;

    @NotNull(message = "Informe se existe tratamento padrão")
    private Boolean tratamentoRealizado;

    @Size(
            max = 1000,
            message = "A descrição do tratamento deve possuir no máximo 1000 caracteres"
    )
    private String descricaoTratamento;

    @NotBlank(message = "O recipiente é obrigatório")
    @Size(
            max = 255,
            message = "O recipiente deve possuir no máximo 255 caracteres"
    )
    private String recipiente;

    @NotNull(message = "A unidade de medida é obrigatória")
    private UnidadeMedida unidadeMedida;

    @NotNull(message = "O nível de risco é obrigatório")
    private NivelRisco nivelRisco;

    @NotEmpty(message = "Informe pelo menos um risco")
    private Set<TipoRisco> riscos;

    @NotEmpty(message = "Informe pelo menos uma classe de resíduo")
    private Set<UUID> classesIds;

    @NotEmpty(message = "Informe pelo menos uma medida de segurança")
    private Set<MedidaSeguranca> medidasSeguranca;

    @Size(
            max = 1000,
            message = "A observação de segurança deve possuir no máximo 1000 caracteres"
    )
    private String observacaoSeguranca;

    @Valid
    @NotEmpty(message = "O modelo deve possuir pelo menos um componente")
    private List<ComponenteModeloResiduoRequestDTO> componentes;

    private Boolean ativo;
}