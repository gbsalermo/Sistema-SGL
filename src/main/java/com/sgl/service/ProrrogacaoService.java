package com.sgl.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.ProrrogacaoRequestDTO;
import com.sgl.dto.response.HistoricoProrrogacaoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Atividade;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;
import com.sgl.model.prorrogacao.HistoricoProrrogacaoAtividade;
import com.sgl.model.prorrogacao.HistoricoProrrogacaoProjeto;
import com.sgl.model.prorrogacao.HistoricoProrrogacaoSci;
import com.sgl.repository.AtividadeRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.SciRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.repository.prorrogacao.HistoricoProrrogacaoAtividadeRepository;
import com.sgl.repository.prorrogacao.HistoricoProrrogacaoProjetoRepository;
import com.sgl.repository.prorrogacao.HistoricoProrrogacaoSciRepository;
import com.sgl.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProrrogacaoService {

	private final ProjetoRepository projetoRepository;
	private final SciRepository sciRepository;
	private final AtividadeRepository atividadeRepository;
	private final UsuarioRepository usuarioRepository;

	private final HistoricoProrrogacaoProjetoRepository historicoProjetoRepository;
	private final HistoricoProrrogacaoSciRepository historicoSciRepository;
	private final HistoricoProrrogacaoAtividadeRepository historicoAtividadeRepository;

	@Transactional
	public HistoricoProrrogacaoResponseDTO prorrogarProjeto(UUID projetoId, ProrrogacaoRequestDTO dto) {

		Projeto projeto = buscarProjeto(projetoId);
		Usuario usuario = buscarUsuarioOperador(dto.getUsuarioId());

		validarAberto("O projeto", projeto.getAtivo(), projeto.getStatus(), projeto.getSituacaoExecucao());

		LocalDate dataAnterior = exigirDataFimExistente(projeto.getDataFim(), "O projeto");

		validarNovaData(dataAnterior, dto.getNovaDataFim());

		String justificativa = normalizarJustificativa(dto.getJustificativa());

		projeto.updateDates(projeto.getDataInicio(), dto.getNovaDataFim());

		projetoRepository.save(projeto);

		HistoricoProrrogacaoProjeto historico = HistoricoProrrogacaoProjeto.builder().projeto(projeto).usuario(usuario)
				.dataFimAnterior(dataAnterior).dataFimNova(dto.getNovaDataFim()).justificativa(justificativa).build();

		historico = historicoProjetoRepository.save(historico);

		return new HistoricoProrrogacaoResponseDTO(historico);
	}

	@Transactional
	public HistoricoProrrogacaoResponseDTO prorrogarSci(UUID sciId, ProrrogacaoRequestDTO dto) {

		Sci sci = buscarSci(sciId);
		Projeto projeto = sci.getProjeto();

		Usuario usuario = buscarUsuarioOperador(dto.getUsuarioId());

		validarAberto("O SCI", sci.getAtivo(), sci.getStatus(), sci.getSituacaoExecucao());

		validarAberto("O projeto pai", projeto.getAtivo(), projeto.getStatus(), projeto.getSituacaoExecucao());

		LocalDate dataAnterior = exigirDataFimExistente(sci.getDataFim(), "O SCI");

		validarNovaData(dataAnterior, dto.getNovaDataFim());

		validarLimiteSuperior(dto.getNovaDataFim(), projeto.getDataFim(),
				"A nova data de fim do SCI não pode ultrapassar a data de fim do projeto.");

		String justificativa = normalizarJustificativa(dto.getJustificativa());

		sci.updateDates(sci.getDataInicio(), dto.getNovaDataFim());

		sciRepository.save(sci);

		HistoricoProrrogacaoSci historico = HistoricoProrrogacaoSci.builder().sci(sci).usuario(usuario)
				.dataFimAnterior(dataAnterior).dataFimNova(dto.getNovaDataFim()).justificativa(justificativa).build();

		historico = historicoSciRepository.save(historico);

		return new HistoricoProrrogacaoResponseDTO(historico);
	}

	@Transactional
	public HistoricoProrrogacaoResponseDTO prorrogarAtividade(UUID atividadeId, ProrrogacaoRequestDTO dto) {

		Atividade atividade = buscarAtividade(atividadeId);

		Sci sci = atividade.getSci();
		Projeto projeto = sci.getProjeto();

		Usuario usuario = buscarUsuarioOperador(dto.getUsuarioId());

		validarAberto("A Atividade", atividade.getAtivo(), atividade.getStatus(), atividade.getSituacaoExecucao());

		validarAberto("O SCI pai", sci.getAtivo(), sci.getStatus(), sci.getSituacaoExecucao());

		validarAberto("O projeto ancestral", projeto.getAtivo(), projeto.getStatus(), projeto.getSituacaoExecucao());

		LocalDate dataAnterior = exigirDataFimExistente(atividade.getDataFim(), "A Atividade");

		validarNovaData(dataAnterior, dto.getNovaDataFim());

		validarLimiteSuperior(dto.getNovaDataFim(), sci.getDataFim(),
				"A nova data de fim da Atividade não pode ultrapassar a data de fim do SCI.");

		/*
		 * Validação defensiva do ancestral.
		 *
		 * Um SCI pode atualmente possuir dataFim nula mesmo quando seu Projeto possui
		 * limite final. Por isso a Atividade também respeita diretamente o teto
		 * conhecido do Projeto.
		 */
		validarLimiteSuperior(dto.getNovaDataFim(), projeto.getDataFim(),
				"A nova data de fim da Atividade não pode ultrapassar a data de fim do projeto.");

		String justificativa = normalizarJustificativa(dto.getJustificativa());

		atividade.updateDates(atividade.getDataInicio(), dto.getNovaDataFim());

		atividadeRepository.save(atividade);

		HistoricoProrrogacaoAtividade historico = HistoricoProrrogacaoAtividade.builder().atividade(atividade)
				.usuario(usuario).dataFimAnterior(dataAnterior).dataFimNova(dto.getNovaDataFim())
				.justificativa(justificativa).build();

		historico = historicoAtividadeRepository.save(historico);

		return new HistoricoProrrogacaoResponseDTO(historico);
	}

	@Transactional(readOnly = true)
	public List<HistoricoProrrogacaoResponseDTO> listarHistoricoProjeto(UUID projetoId) {

		Projeto projeto = buscarProjeto(projetoId);

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return historicoProjetoRepository
				.findByProjetoPublicIdAndProjetoLaboratorioUnidadePublicIdOrderByDataHoraAsc(projeto.getPublicId(),
						unidadeId)
				.stream().map(HistoricoProrrogacaoResponseDTO::new).toList();
	}

	@Transactional(readOnly = true)
	public List<HistoricoProrrogacaoResponseDTO> listarHistoricoSci(UUID sciId) {

		Sci sci = buscarSci(sciId);

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return historicoSciRepository
				.findBySciPublicIdAndSciProjetoLaboratorioUnidadePublicIdOrderByDataHoraAsc(sci.getPublicId(),
						unidadeId)
				.stream().map(HistoricoProrrogacaoResponseDTO::new).toList();
	}

	@Transactional(readOnly = true)
	public List<HistoricoProrrogacaoResponseDTO> listarHistoricoAtividade(UUID atividadeId) {

		Atividade atividade = buscarAtividade(atividadeId);

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return historicoAtividadeRepository
				.findByAtividadePublicIdAndAtividadeSciProjetoLaboratorioUnidadePublicIdOrderByDataHoraAsc(
						atividade.getPublicId(), unidadeId)
				.stream().map(HistoricoProrrogacaoResponseDTO::new).toList();
	}

	private Projeto buscarProjeto(UUID projetoId) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(projetoId, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Projeto", projetoId));
	}

	private Sci buscarSci(UUID sciId) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return sciRepository.findByPublicIdAndProjetoLaboratorioUnidadePublicId(sciId, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("SCI", sciId));
	}

	private Atividade buscarAtividade(UUID atividadeId) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return atividadeRepository.findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(atividadeId, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Atividade", atividadeId));
	}

	private Usuario buscarUsuarioOperador(UUID usuarioId) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		Usuario usuario = usuarioRepository.findByPublicIdAndUnidadePublicId(usuarioId, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Usuário", usuarioId));

		usuario.validateActive();

		validarPerfilProrrogacao(usuario);

		return usuario;
	}

	private void validarPerfilProrrogacao(Usuario usuario) {

		if (usuario.getPerfil() != Perfil.GESTOR && usuario.getPerfil() != Perfil.ADMINISTRADOR) {

			throw new BusinessRuleException("A prorrogação exige perfil GESTOR ou ADMINISTRADOR.");
		}
	}

	private void validarAberto(String entidade, Boolean ativo, StatusProjeto status,
			SituacaoExecucaoProjeto situacaoExecucao) {

		if (!Boolean.TRUE.equals(ativo)) {
			throw new BusinessRuleException(entidade + " está inativo e não pode ser prorrogado.");
		}

		if (status != StatusProjeto.ATIVO) {
			throw new BusinessRuleException(entidade + " está encerrado e não pode ser prorrogado.");
		}

		if (situacaoExecucao == SituacaoExecucaoProjeto.EXECUCAO_CANCELADA) {

			throw new BusinessRuleException(entidade + " possui execução cancelada e não pode ser prorrogado.");
		}
	}

	private LocalDate exigirDataFimExistente(LocalDate dataFim, String entidade) {

		if (dataFim == null) {

			throw new BusinessRuleException(entidade + " ainda não possui data de fim definida. "
					+ "A primeira definição deve ser realizada pelo fluxo comum de atualização.");
		}

		return dataFim;
	}

	private void validarNovaData(LocalDate dataAtual, LocalDate novaData) {

		if (novaData == null) {
			throw new BusinessRuleException("A nova data de fim é obrigatória.");
		}

		if (!novaData.isAfter(dataAtual)) {
			throw new BusinessRuleException("A nova data de fim deve ser posterior à data de fim atual.");
		}
	}

	private void validarLimiteSuperior(LocalDate novaData, LocalDate limite, String mensagem) {

		if (limite != null && novaData.isAfter(limite)) {

			throw new BusinessRuleException(mensagem);
		}
	}

	private String normalizarJustificativa(String justificativa) {

		if (justificativa == null || justificativa.isBlank()) {

			throw new BusinessRuleException("A justificativa da prorrogação é obrigatória.");
		}

		String normalizada = justificativa.trim();

		if (normalizada.length() > 1000) {

			throw new BusinessRuleException("A justificativa deve possuir no máximo 1000 caracteres.");
		}

		return normalizada;
	}

	private void exigirTenantAtivo() {

		if (!TenantContext.ativo()) {

			throw new BusinessRuleException("Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.");
		}
	}
}