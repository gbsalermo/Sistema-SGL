package com.sgl.dto.response;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.sgl.model.ModeloResiduo;
import com.sgl.model.enums.EstadoFisicoResiduo;
import com.sgl.model.enums.MedidaSeguranca;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;

import lombok.Getter;

@Getter
public class ModeloResiduoResponseDTO {

	private final UUID id;

	private final UUID unidadeId;
	private final String unidadeNome;

	private final String nome;
	private final String descricao;
	private final String processoOrigem;

	private final EstadoFisicoResiduo estadoFisico;

	private final Boolean tratamentoRealizado;
	private final String descricaoTratamento;

	private final String recipiente;

	private final UnidadeMedida unidadeMedida;

	private final NivelRisco nivelRisco;
	private final Set<TipoRisco> riscos;

	private final List<ClasseResiduoResponseDTO> classes;

	private final Set<MedidaSeguranca> medidasSeguranca;
	private final String observacaoSeguranca;

	private final List<ComponenteModeloResiduoResponseDTO> componentes;

	private final Boolean ativo;

	public ModeloResiduoResponseDTO(ModeloResiduo entity) {

		this.id = entity.getPublicId();

		this.unidadeId = entity.getUnidade().getPublicId();

		this.unidadeNome = entity.getUnidade().getNome();

		this.nome = entity.getNome();
		this.descricao = entity.getDescricao();
		this.processoOrigem = entity.getProcessoOrigem();

		this.estadoFisico = entity.getEstadoFisico();

		this.tratamentoRealizado = entity.getTratamentoRealizado();

		this.descricaoTratamento = entity.getDescricaoTratamento();

		this.recipiente = entity.getRecipiente();

		this.unidadeMedida = entity.getUnidadeMedida();

		this.nivelRisco = entity.getNivelRisco();

		this.riscos = Set.copyOf(entity.getRiscos());

		this.classes = entity.getClasses().stream().map(ClasseResiduoResponseDTO::new).toList();

		this.medidasSeguranca = Set.copyOf(entity.getMedidasSeguranca());

		this.observacaoSeguranca = entity.getObservacaoSeguranca();

		this.componentes = entity.getComponentes().stream().map(ComponenteModeloResiduoResponseDTO::new).toList();

		this.ativo = entity.getAtivo();
	}
}