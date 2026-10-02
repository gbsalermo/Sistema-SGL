package com.sgl.service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.AtualizarVinculoEstagioRequestDTO;
import com.sgl.dto.request.NovaBolsaVinculoEstagioRequestDTO;
import com.sgl.dto.request.ProrrogarBolsaVinculoEstagioRequestDTO;
import com.sgl.dto.request.NovoVinculoEstagioRequestDTO;
import com.sgl.dto.request.NovoVinculoInstitucionalRequestDTO;
import com.sgl.dto.request.VinculoEstagioAtividadeRequestDTO;
import com.sgl.dto.response.VinculoEstagioResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Atividade;
import com.sgl.model.Curso;
import com.sgl.model.Estagiario;
import com.sgl.model.HistoricoSincronizacaoVinculoEstagio;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.Usuario;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.VinculoEstagioAtividade;
import com.sgl.model.VinculoEstagioAtividadeCultura;
import com.sgl.model.enums.FormacaoEstagiario;
import com.sgl.model.enums.OrigemSincronizacaoVinculoEstagio;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.model.enums.TipoEventoSincronizacaoVinculoEstagio;
import com.sgl.repository.AtividadeRepository;
import com.sgl.repository.CursoRepository;
import com.sgl.repository.EstagiarioRepository;
import com.sgl.repository.HistoricoSincronizacaoVinculoEstagioRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.repository.VinculoEstagioAtividadeCulturaRepository;
import com.sgl.repository.VinculoEstagioAtividadeRepository;
import com.sgl.repository.VinculoEstagioRepository;
import com.sgl.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VinculoEstagioService {

	private final VinculoEstagioRepository vinculoEstagioRepository;
	private final VinculoEstagioAtividadeRepository participacaoRepository;
	private final VinculoEstagioAtividadeCulturaRepository participacaoCulturaRepository;
	private final VinculoEstagioAtividadeService vinculoEstagioAtividadeService;
	private final HistoricoSincronizacaoVinculoEstagioRepository historicoSincronizacaoRepository;

	private final EstagiarioRepository estagiarioRepository;
	private final UsuarioRepository usuarioRepository;
	private final AtividadeRepository atividadeRepository;

	private final CursoRepository cursoRepository;

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

		validarFormacao(dto.getFormacao(), dto.getFormacaoOutro());

		Curso curso = null;

		if (dto.getCursoId() != null) {
			curso = buscarCursoNoTenant(dto.getCursoId());

			curso.validateActive();
		}

		VinculoEstagio vinculo = new VinculoEstagio();

		vinculo.setEstagiario(estagiario);
		vinculo.setOrientador(orientador);

		vinculo.setDataInicio(dto.getDataInicio());

		vinculo.setDataFimPrevista(dto.getDataFimPrevista());

		vinculo.setDataFimEfetiva(null);

		vinculo.setTipoBolsa(dto.getTipoBolsa());

		vinculo.setFormacao(dto.getFormacao());

		vinculo.setFormacaoOutro(normalizarFormacaoOutro(dto.getFormacao(), dto.getFormacaoOutro()));

		vinculo.setCurso(curso);

		vinculo.setTreinamentoSegurancaConcluido(false);

		vinculo.setSituacao(SituacaoEstagio.EM_ANDAMENTO);

		vinculo.setObservacao(normalizarTexto(dto.getObservacao()));

		vinculo = vinculoEstagioRepository.save(vinculo);
		/*
		 * O novo vínculo institucional não pode nascer sem atividade. A primeira
		 * participação usa o mesmo fluxo das demais participações para manter
		 * validações e Culturas centralizadas.
		 */
		VinculoEstagioAtividadeRequestDTO participacaoDto = new VinculoEstagioAtividadeRequestDTO();

		participacaoDto.setAtividadeId(dto.getAtividadeId());
		participacaoDto.setDataInicioParticipacao(dto.getDataInicio());
		participacaoDto.setObservacao(normalizarTexto(dto.getObservacaoParticipacao()));
		participacaoDto.setCulturaIds(dto.getCulturaIds());

		vinculoEstagioAtividadeService.adicionar(vinculo.getPublicId(), participacaoDto);

		List<VinculoEstagioAtividade> participacoes = participacaoRepository
				.findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataInicioParticipacaoDesc(
						vinculo.getPublicId(), TenantContext.unidadeAtual().orElseThrow());

		return montarResponse(vinculo, participacoes);
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

		if (fimPrevista == null) {
			throw new BusinessRuleException("Data final prevista é obrigatória.");
		}

		if (fimPrevista.isBefore(inicio)) {
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

	private Curso buscarCursoNoTenant(UUID cursoId) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return cursoRepository.findByPublicIdAndUnidadePublicId(cursoId, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Curso", cursoId));
	}

	private void validarFormacao(FormacaoEstagiario formacao, String formacaoOutro) {

		if (formacao == null) {
			throw new BusinessRuleException("Formação é obrigatória.");
		}

		if (formacao == FormacaoEstagiario.OUTRO && (formacaoOutro == null || formacaoOutro.isBlank())) {

			throw new BusinessRuleException(
					"A descrição da formação é obrigatória quando a opção OUTRO for selecionada.");
		}
	}

	private String normalizarFormacaoOutro(FormacaoEstagiario formacao, String valor) {

		if (formacao != FormacaoEstagiario.OUTRO) {
			return null;
		}

		return valor.trim();
	}

	@Transactional
	public VinculoEstagioResponseDTO atualizarLocal(UUID vinculoId, AtualizarVinculoEstagioRequestDTO dto) {

		VinculoEstagio vinculo = buscarVinculoNoTenant(vinculoId);

		if (vinculo.getSituacao() == SituacaoEstagio.FINALIZADO) {
			throw new BusinessRuleException("Vínculos finalizados não podem ser alterados pelo fluxo operacional.");
		}

		if (dto.getTipoBolsa() != vinculo.getTipoBolsa()) {
			throw new BusinessRuleException(
					"O tipo de bolsa não pode ser alterado em Editar vínculo. Use o fluxo de nova bolsa.");
		}

		validarPeriodoVinculo(dto.getDataInicio(), dto.getDataFimPrevista());
		validarFormacao(dto.getFormacao(), dto.getFormacaoOutro());

		Usuario orientador = buscarOrientadorNoTenant(dto.getOrientadorId());
		validarOrientador(orientador);
		validarMesmaUnidade(vinculo.getEstagiario(), orientador);

		Curso curso = null;
		if (dto.getCursoId() != null) {
			curso = buscarCursoNoTenant(dto.getCursoId());
			curso.validateActive();
		}

		List<VinculoEstagioAtividade> participacoes = participacaoRepository
				.findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataInicioParticipacaoDesc(
						vinculoId, TenantContext.unidadeAtual().orElseThrow());

		validarPeriodoComParticipacoes(dto.getDataInicio(), dto.getDataFimPrevista(), participacoes);

		LocalDate fimAnterior = vinculo.getDataFimPrevista();
		SituacaoEstagio situacaoAnterior = vinculo.getSituacao();

		vinculo.setOrientador(orientador);
		vinculo.setDataInicio(dto.getDataInicio());
		vinculo.setDataFimPrevista(dto.getDataFimPrevista());
		vinculo.setTipoBolsa(dto.getTipoBolsa());
		vinculo.setFormacao(dto.getFormacao());
		vinculo.setFormacaoOutro(normalizarFormacaoOutro(dto.getFormacao(), dto.getFormacaoOutro()));
		vinculo.setCurso(curso);
		vinculo.setObservacao(normalizarTexto(dto.getObservacao()));

		boolean houveProrrogacao = fimAnterior != null && dto.getDataFimPrevista().isAfter(fimAnterior);

		if (houveProrrogacao) {
			vinculo.setSituacao(SituacaoEstagio.PRORROGADO);
		}

		vinculo = vinculoEstagioRepository.save(vinculo);

		if (!Objects.equals(fimAnterior, dto.getDataFimPrevista())) {
			HistoricoSincronizacaoVinculoEstagio historico = HistoricoSincronizacaoVinculoEstagio.builder()
					.vinculoEstagio(vinculo)
					.tipoEvento(houveProrrogacao
							? TipoEventoSincronizacaoVinculoEstagio.PRORROGACAO
							: TipoEventoSincronizacaoVinculoEstagio.ATUALIZACAO)
					.origem(OrigemSincronizacaoVinculoEstagio.DEV)
					.situacaoAnterior(situacaoAnterior)
					.situacaoNova(vinculo.getSituacao())
					.dataFimPrevistaAnterior(fimAnterior)
					.dataFimPrevistaNova(dto.getDataFimPrevista())
					.build();

			historicoSincronizacaoRepository.save(historico);
		}

		return montarResponse(vinculo, participacoes);
	}

	private void validarPeriodoComParticipacoes(
			LocalDate dataInicio,
			LocalDate dataFimPrevista,
			List<VinculoEstagioAtividade> participacoes) {

		for (VinculoEstagioAtividade participacao : participacoes) {
			if (participacao.getDataInicioParticipacao().isBefore(dataInicio)) {
				throw new BusinessRuleException(
						"A nova data de início é posterior a uma participação já registrada.");
			}

			if (participacao.getDataInicioParticipacao().isAfter(dataFimPrevista)) {
				throw new BusinessRuleException(
						"A nova data final prevista é anterior ao início de uma participação já registrada.");
			}

			if (participacao.getDataFimParticipacao() != null
					&& participacao.getDataFimParticipacao().isAfter(dataFimPrevista)) {
				throw new BusinessRuleException(
						"A nova data final prevista é anterior ao fim de uma participação já registrada.");
			}
		}
	}

	@Transactional
	public VinculoEstagioResponseDTO prorrogarBolsaLocal(UUID vinculoId,
			ProrrogarBolsaVinculoEstagioRequestDTO dto) {

		VinculoEstagio vinculo = buscarVinculoNoTenant(vinculoId);

		if (vinculo.getSituacao() == SituacaoEstagio.FINALIZADO) {
			throw new BusinessRuleException("A bolsa atual já está encerrada.");
		}

		LocalDate fimAnterior = vinculo.getDataFimPrevista();
		LocalDate novoFim = dto.getNovaDataFimPrevista();

		if (fimAnterior == null) {
			throw new BusinessRuleException("A bolsa atual não possui data final prevista para ser prorrogada.");
		}

		if (!novoFim.isAfter(fimAnterior)) {
			throw new BusinessRuleException(
					"A nova data final prevista deve ser posterior ao término atual da bolsa.");
		}

		SituacaoEstagio situacaoAnterior = vinculo.getSituacao();

		vinculo.setDataFimPrevista(novoFim);
		vinculo.setSituacao(SituacaoEstagio.PRORROGADO);
		vinculo = vinculoEstagioRepository.save(vinculo);

		historicoSincronizacaoRepository.save(HistoricoSincronizacaoVinculoEstagio.builder()
				.vinculoEstagio(vinculo)
				.tipoEvento(TipoEventoSincronizacaoVinculoEstagio.PRORROGACAO)
				.origem(OrigemSincronizacaoVinculoEstagio.DEV)
				.situacaoAnterior(situacaoAnterior)
				.situacaoNova(SituacaoEstagio.PRORROGADO)
				.dataFimPrevistaAnterior(fimAnterior)
				.dataFimPrevistaNova(novoFim)
				.dataFimEfetivaAnterior(vinculo.getDataFimEfetiva())
				.dataFimEfetivaNova(vinculo.getDataFimEfetiva())
				.build());

		List<VinculoEstagioAtividade> participacoes = participacaoRepository
				.findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataInicioParticipacaoDesc(
						vinculo.getPublicId(), TenantContext.unidadeAtual().orElseThrow());

		return montarResponse(vinculo, participacoes);
	}

	@Transactional
	public VinculoEstagioResponseDTO registrarNovaBolsaLocal(UUID vinculoId,
			NovaBolsaVinculoEstagioRequestDTO dto) {

		VinculoEstagio atual = buscarVinculoNoTenant(vinculoId);

		if (atual.getSituacao() == SituacaoEstagio.FINALIZADO) {
			throw new BusinessRuleException("A bolsa atual já está encerrada.");
		}

		validarPeriodoVinculo(dto.getDataInicio(), dto.getDataFimPrevista());

		if (!dto.getDataInicio().isAfter(atual.getDataInicio())) {
			throw new BusinessRuleException("A nova bolsa deve iniciar após a data de início da bolsa atual.");
		}

		if (dto.getDataInicio().isAfter(LocalDate.now())) {
			throw new BusinessRuleException(
					"A nova bolsa local só pode entrar em vigor hoje ou em uma data passada.");
		}

		LocalDate inicioNovaBolsa = dto.getDataInicio();
		LocalDate fimBolsaAtual = inicioNovaBolsa.minusDays(1);
		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		List<VinculoEstagioAtividade> participacoes = participacaoRepository
				.findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataInicioParticipacaoDesc(
						atual.getPublicId(), unidadeId);

		SituacaoEstagio situacaoAnterior = atual.getSituacao();
		LocalDate fimEfetivoAnterior = atual.getDataFimEfetiva();

		atual.setSituacao(SituacaoEstagio.FINALIZADO);
		atual.setDataFimEfetiva(fimBolsaAtual);
		vinculoEstagioRepository.save(atual);

		historicoSincronizacaoRepository.save(HistoricoSincronizacaoVinculoEstagio.builder()
				.vinculoEstagio(atual)
				.tipoEvento(TipoEventoSincronizacaoVinculoEstagio.FINALIZACAO)
				.origem(OrigemSincronizacaoVinculoEstagio.DEV)
				.situacaoAnterior(situacaoAnterior)
				.situacaoNova(SituacaoEstagio.FINALIZADO)
				.dataFimPrevistaAnterior(atual.getDataFimPrevista())
				.dataFimPrevistaNova(atual.getDataFimPrevista())
				.dataFimEfetivaAnterior(fimEfetivoAnterior)
				.dataFimEfetivaNova(fimBolsaAtual)
				.build());

		VinculoEstagio novo = new VinculoEstagio();
		novo.setEstagiario(atual.getEstagiario());
		novo.setOrientador(atual.getOrientador());
		novo.setDataInicio(inicioNovaBolsa);
		novo.setDataFimPrevista(dto.getDataFimPrevista());
		novo.setDataFimEfetiva(null);
		novo.setTipoBolsa(dto.getTipoBolsa());
		novo.setFormacao(atual.getFormacao());
		novo.setFormacaoOutro(atual.getFormacaoOutro());
		novo.setCurso(atual.getCurso());
		novo.setTreinamentoSegurancaConcluido(atual.getTreinamentoSegurancaConcluido());
		novo.setSituacao(SituacaoEstagio.EM_ANDAMENTO);
		novo.setObservacao(atual.getObservacao());
		novo.setReferenciaInstitucional(null);
		novo = vinculoEstagioRepository.save(novo);

		historicoSincronizacaoRepository.save(HistoricoSincronizacaoVinculoEstagio.builder()
				.vinculoEstagio(novo)
				.tipoEvento(TipoEventoSincronizacaoVinculoEstagio.CRIACAO)
				.origem(OrigemSincronizacaoVinculoEstagio.DEV)
				.situacaoNova(SituacaoEstagio.EM_ANDAMENTO)
				.dataFimPrevistaNova(dto.getDataFimPrevista())
				.build());

		for (VinculoEstagioAtividade participacao : participacoes) {

			LocalDate inicioParticipacao = participacao.getDataInicioParticipacao();
			LocalDate fimParticipacaoOriginal = participacao.getDataFimParticipacao();

			/*
			 * Participações que terminaram antes da nova bolsa permanecem integralmente
			 * no vínculo anterior.
			 */
			if (fimParticipacaoOriginal != null && fimParticipacaoOriginal.isBefore(inicioNovaBolsa)) {
				continue;
			}

			/*
			 * Se a participação começou na data da nova bolsa ou depois dela, o registro
			 * inteiro pertence à nova ocorrência. A associação de Culturas acompanha a
			 * própria participação, portanto não precisa ser recriada.
			 */
			if (!inicioParticipacao.isBefore(inicioNovaBolsa)) {
				participacao.setVinculoEstagio(novo);
				participacaoRepository.save(participacao);
				continue;
			}

			/*
			 * Participação que atravessa a troca de bolsa é dividida: o trecho anterior
			 * permanece no vínculo antigo e o trecho seguinte continua no novo vínculo.
			 */
			participacao.setDataFimParticipacao(fimBolsaAtual);
			participacaoRepository.save(participacao);

			VinculoEstagioAtividade continuacao = new VinculoEstagioAtividade();
			continuacao.setVinculoEstagio(novo);
			continuacao.setAtividade(participacao.getAtividade());
			continuacao.setDataInicioParticipacao(inicioNovaBolsa);
			continuacao.setDataFimParticipacao(fimParticipacaoOriginal);
			continuacao.setObservacao(participacao.getObservacao());
			continuacao = participacaoRepository.save(continuacao);

			List<VinculoEstagioAtividadeCultura> culturas = participacaoCulturaRepository
					.findByParticipacaoPublicIdAndParticipacaoVinculoEstagioEstagiarioUnidadePublicIdOrderByCulturaNomeAsc(
							participacao.getPublicId(), unidadeId);

			for (VinculoEstagioAtividadeCultura associacao : culturas) {
				VinculoEstagioAtividadeCultura copia = new VinculoEstagioAtividadeCultura();
				copia.setParticipacao(continuacao);
				copia.setCultura(associacao.getCultura());
				participacaoCulturaRepository.save(copia);
			}
		}

		List<VinculoEstagioAtividade> novas = participacaoRepository
				.findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataInicioParticipacaoDesc(
						novo.getPublicId(), unidadeId);

		return montarResponse(novo, novas);
	}

	@Transactional
	public VinculoEstagioResponseDTO concluirTreinamentoSeguranca(UUID vinculoId) {

		VinculoEstagio vinculo = buscarVinculoNoTenant(vinculoId);

		if (vinculo.getSituacao() == SituacaoEstagio.FINALIZADO) {

			throw new BusinessRuleException(
					"Não é possível registrar treinamento em um vínculo de estágio finalizado.");
		}

		if (Boolean.TRUE.equals(vinculo.getTreinamentoSegurancaConcluido())) {

			throw new BusinessRuleException("O treinamento de segurança já foi registrado como concluído.");
		}

		vinculo.setTreinamentoSegurancaConcluido(true);

		vinculo = vinculoEstagioRepository.save(vinculo);

		List<VinculoEstagioAtividade> participacoes = participacaoRepository
				.findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataInicioParticipacaoDesc(
						vinculo.getPublicId(), TenantContext.unidadeAtual().orElseThrow());

		return montarResponse(vinculo, participacoes);
	}

	private VinculoEstagioResponseDTO montarResponse(VinculoEstagio vinculo,
			List<VinculoEstagioAtividade> participacoes) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		Map<UUID, List<VinculoEstagioAtividadeCultura>> culturasPorParticipacao = new LinkedHashMap<>();

		for (VinculoEstagioAtividade participacao : participacoes) {

			List<VinculoEstagioAtividadeCultura> culturas = participacaoCulturaRepository
					.findByParticipacaoPublicIdAndParticipacaoVinculoEstagioEstagiarioUnidadePublicIdOrderByCulturaNomeAsc(
							participacao.getPublicId(), unidadeId);

			culturasPorParticipacao.put(participacao.getPublicId(), culturas);
		}

		return new VinculoEstagioResponseDTO(vinculo, participacoes, culturasPorParticipacao);
	}

	private VinculoEstagio buscarVinculoNoTenant(UUID vinculoId) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return vinculoEstagioRepository.findByPublicIdAndEstagiarioUnidadePublicId(vinculoId, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Vínculo de estágio", vinculoId));
	}

	@Transactional
	public VinculoEstagioResponseDTO criarInstitucional(UUID estagiarioId, NovoVinculoInstitucionalRequestDTO dto) {

		Estagiario estagiario = buscarEstagiarioNoTenant(estagiarioId);

		String referenciaEvento = normalizarTexto(dto.getReferenciaEvento());

		/*
		 * Idempotência: se o ambiente reenviar o mesmo evento de criação, não criamos
		 * outro vínculo.
		 */
		if (referenciaEvento != null) {

			var eventoExistente = historicoSincronizacaoRepository.findByOrigemAndReferenciaEvento(dto.getOrigem(),
					referenciaEvento);

			if (eventoExistente.isPresent()) {

				HistoricoSincronizacaoVinculoEstagio historico = eventoExistente.get();

				VinculoEstagio vinculoExistente = historico.getVinculoEstagio();

				if (historico.getTipoEvento() != TipoEventoSincronizacaoVinculoEstagio.CRIACAO) {

					throw new BusinessRuleException(
							"A referência do evento já foi utilizada " + "por outro tipo de sincronização.");
				}

				if (!vinculoExistente.getEstagiario().getPublicId().equals(estagiarioId)) {

					throw new BusinessRuleException(
							"A referência do evento já foi utilizada " + "para outro Estagiário.");
				}

				return montarResponse(vinculoExistente, List.of());
			}
		}

		estagiario.validateActive();

		/*
		 * Uma nova bolsa só pode gerar um novo vínculo quando não existe outro vínculo
		 * não finalizado.
		 */
		validarAusenciaDeVinculoAtivo(estagiario);

		Usuario orientador = buscarOrientadorNoTenant(dto.getOrientadorId());

		validarOrientador(orientador);

		validarMesmaUnidade(estagiario, orientador);

		validarPeriodoVinculo(dto.getDataInicio(), dto.getDataFimPrevista());

		validarFormacao(dto.getFormacao(), dto.getFormacaoOutro());

		Curso curso = null;

		if (dto.getCursoId() != null) {

			curso = buscarCursoNoTenant(dto.getCursoId());

			curso.validateActive();
		}

		VinculoEstagio vinculo = new VinculoEstagio();

		vinculo.setEstagiario(estagiario);

		vinculo.setOrientador(orientador);

		vinculo.setDataInicio(dto.getDataInicio());

		vinculo.setDataFimPrevista(dto.getDataFimPrevista());

		vinculo.setDataFimEfetiva(null);

		vinculo.setTipoBolsa(dto.getTipoBolsa());

		vinculo.setFormacao(dto.getFormacao());

		vinculo.setFormacaoOutro(normalizarFormacaoOutro(dto.getFormacao(), dto.getFormacaoOutro()));

		vinculo.setCurso(curso);

		vinculo.setTreinamentoSegurancaConcluido(false);

		/*
		 * Institucionalmente o vínculo existe, mas ainda não é operacionalmente ativo
		 * até possuir uma participação aberta em Atividade.
		 */
		vinculo.setSituacao(SituacaoEstagio.EM_ANDAMENTO);

		vinculo.setObservacao(normalizarTexto(dto.getObservacao()));

		vinculo.setReferenciaInstitucional(normalizarTexto(dto.getReferenciaInstitucional()));

		vinculo = vinculoEstagioRepository.save(vinculo);

		/*
		 * A criação institucional também entra na trilha de sincronização.
		 */
		HistoricoSincronizacaoVinculoEstagio historico = HistoricoSincronizacaoVinculoEstagio.builder()
				.vinculoEstagio(vinculo).tipoEvento(TipoEventoSincronizacaoVinculoEstagio.CRIACAO)
				.origem(dto.getOrigem()).referenciaEvento(referenciaEvento).situacaoAnterior(null)
				.situacaoNova(SituacaoEstagio.EM_ANDAMENTO).dataFimPrevistaAnterior(null)
				.dataFimPrevistaNova(dto.getDataFimPrevista()).dataFimEfetivaAnterior(null).dataFimEfetivaNova(null)
				.dataHoraOrigem(dto.getDataHoraOrigem()).build();

		historicoSincronizacaoRepository.save(historico);

		/*
		 * Diferente do fluxo local/DEV antigo, o vínculo institucional nasce sem
		 * participação obrigatória em Atividade.
		 */
		return montarResponse(vinculo, List.of());
	}
}