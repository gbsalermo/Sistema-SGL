package com.sgl.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.SincronizacaoVinculoEstagioRequestDTO;
import com.sgl.dto.response.HistoricoSincronizacaoVinculoEstagioResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Curso;
import com.sgl.model.HistoricoSincronizacaoVinculoEstagio;
import com.sgl.model.Usuario;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.VinculoEstagioAtividade;
import com.sgl.model.enums.FormacaoEstagiario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.model.enums.TipoEventoSincronizacaoVinculoEstagio;
import com.sgl.repository.CursoRepository;
import com.sgl.repository.HistoricoSincronizacaoVinculoEstagioRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.repository.VinculoEstagioAtividadeRepository;
import com.sgl.repository.VinculoEstagioRepository;
import com.sgl.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

/**
 * Serviço responsável por sincronizar os dados institucionais de vínculos de
 * estágio, validando regras de negócio, atualizando o vínculo e registrando o
 * histórico da sincronização.
 */
@Service
@RequiredArgsConstructor
public class SincronizacaoVinculoEstagioService {

	private final VinculoEstagioRepository vinculoEstagioRepository;

	private final VinculoEstagioAtividadeRepository participacaoRepository;

	private final HistoricoSincronizacaoVinculoEstagioRepository historicoRepository;

	private final CursoRepository cursoRepository;

	private final UsuarioRepository usuarioRepository;

	@Transactional
	public HistoricoSincronizacaoVinculoEstagioResponseDTO sincronizar(UUID vinculoId,
			SincronizacaoVinculoEstagioRequestDTO dto) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		validarCamposObrigatorios(dto);

		String referenciaEvento = normalizarTexto(dto.getReferenciaEvento());

		String referenciaInstitucional = normalizarTexto(dto.getReferenciaInstitucional());

		VinculoEstagio vinculo = vinculoEstagioRepository.buscarPorPublicIdETenantComBloqueio(vinculoId, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Vínculo de estágio", vinculoId));

		validarReferenciaInstitucional(vinculo, referenciaInstitucional);

		Optional<HistoricoSincronizacaoVinculoEstagio> eventoJaProcessado = buscarEventoJaProcessado(dto,
				referenciaEvento);

		if (eventoJaProcessado.isPresent()) {

			HistoricoSincronizacaoVinculoEstagio historico = eventoJaProcessado.get();

			if (!historico.getVinculoEstagio().getPublicId().equals(vinculoId)) {

				throw new BusinessRuleException(
						"A referência do evento já foi utilizada " + "para outro vínculo de estágio.");
			}

			validarRepeticaoIdempotente(historico, dto, vinculo);

			return new HistoricoSincronizacaoVinculoEstagioResponseDTO(historico);
		}

		List<VinculoEstagioAtividade> participacoes = participacaoRepository
				.findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataInicioParticipacaoDesc(
						vinculoId, unidadeId);

		validarCamposOpcionaisInstitucionais(vinculo, dto, unidadeId);

		validarEstadoRecebido(vinculo, dto);

		validarCoerenciaComParticipacoes(vinculo, participacoes, dto);

		TipoEventoSincronizacaoVinculoEstagio tipoEvento = classificarEvento(vinculo, dto);

		SituacaoEstagio situacaoAnterior = vinculo.getSituacao();

		LocalDate dataFimPrevistaAnterior = vinculo.getDataFimPrevista();

		LocalDate dataFimEfetivaAnterior = vinculo.getDataFimEfetiva();

		aplicarReferenciaInstitucional(vinculo, referenciaInstitucional);

		aplicarEstadoInstitucional(vinculo, dto);

		if (dto.getSituacao() == SituacaoEstagio.FINALIZADO) {

			encerrarParticipacoesAbertas(participacoes, dto.getDataFimEfetiva());
		}

		vinculoEstagioRepository.save(vinculo);

		HistoricoSincronizacaoVinculoEstagio historico = HistoricoSincronizacaoVinculoEstagio.builder()
				.vinculoEstagio(vinculo).tipoEvento(tipoEvento).origem(dto.getOrigem())
				.referenciaEvento(referenciaEvento).situacaoAnterior(situacaoAnterior).situacaoNova(dto.getSituacao())
				.dataFimPrevistaAnterior(dataFimPrevistaAnterior).dataFimPrevistaNova(dto.getDataFimPrevista())
				.dataFimEfetivaAnterior(dataFimEfetivaAnterior).dataFimEfetivaNova(dto.getDataFimEfetiva())
				.dataHoraOrigem(dto.getDataHoraOrigem()).build();

		historico = historicoRepository.save(historico);

		return new HistoricoSincronizacaoVinculoEstagioResponseDTO(historico);
	}

	private Optional<HistoricoSincronizacaoVinculoEstagio> buscarEventoJaProcessado(
			SincronizacaoVinculoEstagioRequestDTO dto, String referenciaEvento) {

		if (referenciaEvento == null) {
			return Optional.empty();
		}

		return historicoRepository.findByOrigemAndReferenciaEvento(dto.getOrigem(), referenciaEvento);
	}

	private void validarCamposObrigatorios(SincronizacaoVinculoEstagioRequestDTO dto) {

		if (dto.getOrigem() == null) {

			throw new BusinessRuleException("A origem da sincronização é obrigatória.");
		}

		if (dto.getSituacao() == null) {

			throw new BusinessRuleException("A situação institucional é obrigatória.");
		}

		if (dto.getDataFimPrevista() == null) {

			throw new BusinessRuleException("A data final prevista é obrigatória.");
		}
	}

	private void validarReferenciaInstitucional(VinculoEstagio vinculo, String referenciaRecebida) {

		if (referenciaRecebida == null) {
			return;
		}

		String referenciaAtual = normalizarTexto(vinculo.getReferenciaInstitucional());

		if (referenciaAtual == null) {
			return;
		}

		if (!referenciaAtual.equals(referenciaRecebida)) {

			throw new BusinessRuleException("A referência institucional do vínculo " + "não pode ser substituída pela "
					+ "sincronização comum.");
		}
	}

	private void validarEstadoRecebido(VinculoEstagio vinculo, SincronizacaoVinculoEstagioRequestDTO dto) {

		LocalDate dataInicio = dto.getDataInicio() != null ? dto.getDataInicio() : vinculo.getDataInicio();

		LocalDate fimPrevista = dto.getDataFimPrevista();

		if (fimPrevista.isBefore(dataInicio)) {

			throw new BusinessRuleException(
					"A data final prevista institucional " + "não pode ser anterior ao início " + "do vínculo.");
		}

		if (dto.getSituacao() == SituacaoEstagio.FINALIZADO) {

			validarFinalizacao(vinculo, dto, dataInicio);

			return;
		}

		if (dto.getDataFimEfetiva() != null) {

			throw new BusinessRuleException("Um vínculo não finalizado não pode " + "possuir data final efetiva.");
		}

		if (vinculo.getSituacao() == SituacaoEstagio.FINALIZADO) {

			throw new BusinessRuleException(
					"Um vínculo finalizado não pode ser " + "reativado pela sincronização comum.");
		}
	}

	private void validarFinalizacao(
			VinculoEstagio vinculo,
			SincronizacaoVinculoEstagioRequestDTO dto,
			LocalDate dataInicioRecebida) {

		LocalDate fimEfetiva = dto.getDataFimEfetiva();

		if (fimEfetiva == null) {

			throw new BusinessRuleException("A data final efetiva é obrigatória " + "para um vínculo finalizado.");
		}

		if (fimEfetiva.isBefore(dataInicioRecebida)) {

			throw new BusinessRuleException("A data final efetiva não pode ser " + "anterior ao início do vínculo.");
		}

		if (fimEfetiva.isAfter(dto.getDataFimPrevista())) {

			throw new BusinessRuleException("A data final efetiva não pode ser " + "posterior à data final prevista "
					+ "informada pelo ambiente institucional.");
		}

		if (vinculo.getSituacao() == SituacaoEstagio.FINALIZADO) {

			boolean mesmoEstado = Objects.equals(vinculo.getDataFimPrevista(), dto.getDataFimPrevista())
					&& Objects.equals(vinculo.getDataFimEfetiva(), dto.getDataFimEfetiva());

			if (!mesmoEstado) {

				throw new BusinessRuleException("Um vínculo já finalizado não pode " + "ter seu encerramento reescrito "
						+ "pela sincronização comum.");
			}
		}
	}

	private void validarCoerenciaComParticipacoes(
			VinculoEstagio vinculo,
			List<VinculoEstagioAtividade> participacoes,
			SincronizacaoVinculoEstagioRequestDTO dto) {

		LocalDate inicioRecebido = dto.getDataInicio() != null
				? dto.getDataInicio()
				: vinculo.getDataInicio();

		for (VinculoEstagioAtividade participacao : participacoes) {

			if (participacao.getDataInicioParticipacao().isBefore(inicioRecebido)) {
				throw new BusinessRuleException(
						"A data de início institucional é posterior a uma participação já registrada.");
			}

			if (participacao.getDataInicioParticipacao().isAfter(dto.getDataFimPrevista())) {

				throw new BusinessRuleException("A data final prevista recebida " + "é incompatível com o histórico "
						+ "de participações do vínculo.");
			}

			if (participacao.getDataFimParticipacao() != null
					&& participacao.getDataFimParticipacao().isAfter(dto.getDataFimPrevista())) {

				throw new BusinessRuleException(
						"A data final prevista recebida " + "é anterior a uma participação " + "já registrada.");
			}

			if (dto.getSituacao() == SituacaoEstagio.FINALIZADO) {

				validarParticipacaoNaFinalizacao(participacao, dto.getDataFimEfetiva());
			}
		}
	}

	private void validarParticipacaoNaFinalizacao(VinculoEstagioAtividade participacao, LocalDate dataFimEfetiva) {

		if (participacao.getDataInicioParticipacao().isAfter(dataFimEfetiva)) {

			throw new BusinessRuleException(
					"A data efetiva de encerramento é " + "anterior ao início de uma participação.");
		}

		if (participacao.getDataFimParticipacao() != null
				&& participacao.getDataFimParticipacao().isAfter(dataFimEfetiva)) {

			throw new BusinessRuleException(
					"A data efetiva de encerramento é " + "incompatível com uma participação " + "já encerrada.");
		}

		LocalDate fimAtividade = participacao.getAtividade().getDataFim();

		if (participacao.getDataFimParticipacao() == null && fimAtividade != null
				&& dataFimEfetiva.isAfter(fimAtividade)) {

			throw new BusinessRuleException("A data efetiva de encerramento é incompatível "
					+ "com uma participação aberta cuja Atividade " + "já terminou anteriormente.");
		}
	}

	private TipoEventoSincronizacaoVinculoEstagio classificarEvento(VinculoEstagio vinculo,
			SincronizacaoVinculoEstagioRequestDTO dto) {

		if (dto.getSituacao() == SituacaoEstagio.FINALIZADO && vinculo.getSituacao() != SituacaoEstagio.FINALIZADO) {

			return TipoEventoSincronizacaoVinculoEstagio.FINALIZACAO;
		}

		LocalDate fimAtual = vinculo.getDataFimPrevista();

		LocalDate fimNovo = dto.getDataFimPrevista();

		if (fimAtual != null && fimNovo.isAfter(fimAtual)) {

			return TipoEventoSincronizacaoVinculoEstagio.PRORROGACAO;
		}

		return TipoEventoSincronizacaoVinculoEstagio.ATUALIZACAO;
	}

	private void aplicarReferenciaInstitucional(VinculoEstagio vinculo, String referenciaInstitucional) {

		if (referenciaInstitucional == null) {
			return;
		}

		if (normalizarTexto(vinculo.getReferenciaInstitucional()) == null) {

			vinculo.setReferenciaInstitucional(referenciaInstitucional);
		}
	}

	private void aplicarEstadoInstitucional(VinculoEstagio vinculo, SincronizacaoVinculoEstagioRequestDTO dto) {

		if (dto.getDataInicio() != null) {
			vinculo.setDataInicio(dto.getDataInicio());
		}

		vinculo.setDataFimPrevista(dto.getDataFimPrevista());

		if (dto.getTipoBolsa() != null) {
			vinculo.setTipoBolsa(dto.getTipoBolsa());
		}

		if (dto.getFormacao() != null) {
			vinculo.setFormacao(dto.getFormacao());
			vinculo.setFormacaoOutro(normalizarFormacaoOutro(dto.getFormacao(), dto.getFormacaoOutro()));
		}

		if (dto.getCursoId() != null) {
			UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();
			Curso curso = cursoRepository.findByPublicIdAndUnidadePublicId(dto.getCursoId(), unidadeId)
					.orElseThrow(() -> new ResourceNotFoundException("Curso", dto.getCursoId()));
			vinculo.setCurso(curso);
		}

		if (dto.getOrientadorId() != null) {
			UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();
			Usuario orientador = usuarioRepository.findByPublicIdAndUnidadePublicId(dto.getOrientadorId(), unidadeId)
					.orElseThrow(() -> new ResourceNotFoundException("Orientador", dto.getOrientadorId()));
			vinculo.setOrientador(orientador);
		}

		vinculo.setSituacao(dto.getSituacao());

		if (dto.getSituacao() == SituacaoEstagio.FINALIZADO) {

			vinculo.setDataFimEfetiva(dto.getDataFimEfetiva());

		} else {

			vinculo.setDataFimEfetiva(null);
		}
	}

	private void encerrarParticipacoesAbertas(List<VinculoEstagioAtividade> participacoes, LocalDate dataFimEfetiva) {

		List<VinculoEstagioAtividade> abertas = participacoes.stream().filter(p -> p.getDataFimParticipacao() == null)
				.toList();

		for (VinculoEstagioAtividade participacao : abertas) {

			participacao.setDataFimParticipacao(dataFimEfetiva);
		}

		if (!abertas.isEmpty()) {
			participacaoRepository.saveAll(abertas);
		}
	}

	private void validarCamposOpcionaisInstitucionais(
			VinculoEstagio vinculo,
			SincronizacaoVinculoEstagioRequestDTO dto,
			UUID unidadeId) {

		if (dto.getFormacao() != null) {
			validarFormacao(dto.getFormacao(), dto.getFormacaoOutro());
		}

		if (dto.getCursoId() != null) {
			Curso curso = cursoRepository.findByPublicIdAndUnidadePublicId(dto.getCursoId(), unidadeId)
					.orElseThrow(() -> new ResourceNotFoundException("Curso", dto.getCursoId()));
			curso.validateActive();
		}

		if (dto.getOrientadorId() != null) {
			Usuario orientador = usuarioRepository.findByPublicIdAndUnidadePublicId(dto.getOrientadorId(), unidadeId)
					.orElseThrow(() -> new ResourceNotFoundException("Orientador", dto.getOrientadorId()));

			orientador.validateActive();

			if (orientador.getPerfil() != Perfil.ANALISTA
					&& orientador.getPerfil() != Perfil.PESQUISADOR) {
				throw new BusinessRuleException(
						"Orientador deve possuir perfil ANALISTA ou PESQUISADOR.");
			}

			if (vinculo.getEstagiario().getUnidade() == null
					|| orientador.getUnidade() == null
					|| !vinculo.getEstagiario().getUnidade().getPublicId()
							.equals(orientador.getUnidade().getPublicId())) {
				throw new BusinessRuleException(
						"Estagiário e orientador devem pertencer à mesma unidade.");
			}
		}
	}

	private void validarFormacao(FormacaoEstagiario formacao, String formacaoOutro) {

		if (formacao == FormacaoEstagiario.OUTRO
				&& (formacaoOutro == null || formacaoOutro.isBlank())) {
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

	private String normalizarTexto(String valor) {

		if (valor == null || valor.isBlank()) {
			return null;
		}

		return valor.trim();
	}

	private void exigirTenantAtivo() {

		if (!TenantContext.ativo()) {

			throw new BusinessRuleException("Cabeçalho X-SGL-Unidade-Id " + "é obrigatório para esta operação.");
		}
	}

	private void validarRepeticaoIdempotente(
			HistoricoSincronizacaoVinculoEstagio historico,
			SincronizacaoVinculoEstagioRequestDTO dto,
			VinculoEstagio vinculo) {

		boolean mesmoEstado = Objects.equals(historico.getSituacaoNova(), dto.getSituacao())
				&& Objects.equals(historico.getDataFimPrevistaNova(), dto.getDataFimPrevista())
				&& Objects.equals(historico.getDataFimEfetivaNova(), dto.getDataFimEfetiva())
				&& (dto.getDataInicio() == null || Objects.equals(vinculo.getDataInicio(), dto.getDataInicio()))
				&& (dto.getTipoBolsa() == null || Objects.equals(vinculo.getTipoBolsa(), dto.getTipoBolsa()))
				&& (dto.getFormacao() == null || Objects.equals(vinculo.getFormacao(), dto.getFormacao()))
				&& (dto.getCursoId() == null
						|| (vinculo.getCurso() != null
								&& Objects.equals(vinculo.getCurso().getPublicId(), dto.getCursoId())))
				&& (dto.getOrientadorId() == null
						|| (vinculo.getOrientador() != null
								&& Objects.equals(vinculo.getOrientador().getPublicId(), dto.getOrientadorId())));

		if (!mesmoEstado) {
			throw new BusinessRuleException(
					"A referência do evento já foi processada com dados institucionais diferentes.");
		}
	}
}