package com.sgl.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.NovoVinculoEstagioRequestDTO;
import com.sgl.dto.response.VinculoEstagioResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Atividade;
import com.sgl.model.Estagiario;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.Usuario;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.VinculoEstagioAtividade;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.repository.AtividadeRepository;
import com.sgl.repository.EstagiarioRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.repository.VinculoEstagioAtividadeRepository;
import com.sgl.repository.VinculoEstagioRepository;
import com.sgl.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VinculoEstagioService {

	private final VinculoEstagioRepository vinculoEstagioRepository;
	private final VinculoEstagioAtividadeRepository participacaoRepository;

	private final EstagiarioRepository estagiarioRepository;
	private final UsuarioRepository usuarioRepository;
	private final AtividadeRepository atividadeRepository;

	@Transactional
	public VinculoEstagioResponseDTO criar(UUID estagiarioId, NovoVinculoEstagioRequestDTO dto) {

		Estagiario estagiario = buscarEstagiarioNoTenant(estagiarioId);

		estagiario.validateActive();

		validarAusenciaDeVinculoAtivo(estagiario);

		Usuario orientador = buscarOrientadorNoTenant(dto.getOrientadorId());

		validarOrientador(orientador);

		validarMesmaUnidade(estagiario, orientador);

		Atividade atividade = buscarAtividadeNoTenant(dto.getAtividadeId());

		validarAtividadeOperacional(atividade);

		validarMesmaUnidade(estagiario, atividade);

		validarPeriodoVinculo(dto.getDataInicio(), dto.getDataFimPrevista());

		validarInicioComAtividade(dto.getDataInicio(), atividade);

		VinculoEstagio vinculo = new VinculoEstagio();

		vinculo.setEstagiario(estagiario);
		vinculo.setOrientador(orientador);

		vinculo.setDataInicio(dto.getDataInicio());

		vinculo.setDataFimPrevista(dto.getDataFimPrevista());

		vinculo.setDataFimEfetiva(null);

		vinculo.setTipoBolsa(dto.getTipoBolsa());

		vinculo.setSituacao(SituacaoEstagio.EM_ANDAMENTO);

		vinculo.setObservacao(normalizarTexto(dto.getObservacao()));

		vinculo = vinculoEstagioRepository.save(vinculo);

		/*
		 * O novo vínculo institucional não pode nascer sem atividade. A primeira
		 * participação é criada na mesma transação.
		 */
		VinculoEstagioAtividade participacao = new VinculoEstagioAtividade();

		participacao.setVinculoEstagio(vinculo);

		participacao.setAtividade(atividade);

		participacao.setDataInicioParticipacao(dto.getDataInicio());

		participacao.setDataFimParticipacao(null);

		participacao.setObservacao(normalizarTexto(dto.getObservacaoParticipacao()));

		participacao = participacaoRepository.save(participacao);

		return new VinculoEstagioResponseDTO(vinculo, List.of(participacao));
	}

	private Estagiario buscarEstagiarioNoTenant(UUID estagiarioId) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return estagiarioRepository.findByPublicIdAndUnidadePublicId(estagiarioId, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Estagiário", estagiarioId));
	}

	private Usuario buscarOrientadorNoTenant(UUID orientadorId) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return usuarioRepository.findByPublicIdAndUnidadePublicId(orientadorId, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Orientador", orientadorId));
	}

	private Atividade buscarAtividadeNoTenant(UUID atividadeId) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return atividadeRepository.findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(atividadeId, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Atividade", atividadeId));
	}

	private void validarAusenciaDeVinculoAtivo(Estagiario estagiario) {

		boolean possuiVinculoNaoFinalizado = vinculoEstagioRepository
				.existsByEstagiarioIdAndSituacaoNot(estagiario.getId(), SituacaoEstagio.FINALIZADO);

		if (possuiVinculoNaoFinalizado) {

			throw new BusinessRuleException("O Estagiário já possui um vínculo de estágio em andamento.");
		}
	}

	private void validarOrientador(Usuario orientador) {

		orientador.validateActive();

		if (orientador.getPerfil() != Perfil.ANALISTA && orientador.getPerfil() != Perfil.PESQUISADOR) {

			throw new BusinessRuleException("Orientador deve possuir perfil ANALISTA ou PESQUISADOR.");
		}
	}

	private void validarMesmaUnidade(Estagiario estagiario, Usuario orientador) {

		if (estagiario.getUnidade() == null || orientador.getUnidade() == null
				|| !estagiario.getUnidade().getPublicId().equals(orientador.getUnidade().getPublicId())) {

			throw new BusinessRuleException("Estagiário e orientador devem pertencer à mesma unidade.");
		}
	}

	private void validarMesmaUnidade(Estagiario estagiario, Atividade atividade) {

		UUID unidadeEstagiario = estagiario.getUnidade().getPublicId();

		UUID unidadeAtividade = atividade.getSci().getProjeto().getLaboratorio().getUnidade().getPublicId();

		if (!unidadeEstagiario.equals(unidadeAtividade)) {

			throw new BusinessRuleException("O Estagiário e a Atividade devem pertencer à mesma Unidade.");
		}
	}

	private void validarAtividadeOperacional(Atividade atividade) {

		atividade.validateActive();

		Sci sci = atividade.getSci();

		if (sci == null) {

			throw new BusinessRuleException("A Atividade informada não possui SCI válido.");
		}

		sci.validateActive();

		Projeto projeto = sci.getProjeto();

		if (projeto == null) {

			throw new BusinessRuleException("A Atividade informada não possui Projeto válido.");
		}

		projeto.validateActive();

		Laboratorio laboratorio = projeto.getLaboratorio();

		if (laboratorio == null) {

			throw new BusinessRuleException("A Atividade informada não possui Laboratório válido.");
		}

		laboratorio.validateActive();
	}

	private void validarPeriodoVinculo(LocalDate inicio, LocalDate fimPrevista) {

		if (inicio == null) {

			throw new BusinessRuleException("Data de início do vínculo é obrigatória.");
		}

		if (fimPrevista != null && fimPrevista.isBefore(inicio)) {

			throw new BusinessRuleException("Data final prevista não pode ser anterior à data de início do vínculo.");
		}
	}

	private void validarInicioComAtividade(LocalDate inicioVinculo, Atividade atividade) {

		if (atividade.getDataInicio() != null && inicioVinculo.isBefore(atividade.getDataInicio())) {

			throw new BusinessRuleException(
					"O vínculo não pode iniciar sua participação antes do início da Atividade.");
		}

		if (atividade.getDataFim() != null && inicioVinculo.isAfter(atividade.getDataFim())) {

			throw new BusinessRuleException("O vínculo não pode iniciar sua participação após o fim da Atividade.");
		}
	}

	private String normalizarTexto(String valor) {

		if (valor == null || valor.isBlank()) {

			return null;
		}

		return valor.trim();
	}

	private void exigirTenantAtivo() {

		if (!TenantContext.ativo()) {

			throw new BusinessRuleException("Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.");
		}
	}
}