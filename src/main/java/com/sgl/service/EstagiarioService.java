package com.sgl.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.EstagiarioRequestDTO;
import com.sgl.dto.request.VinculoEstagioAtividadeRequestDTO;
import com.sgl.dto.response.EstagiarioResponseDTO;
import com.sgl.dto.response.VinculoEstagioResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Estagiario;
import com.sgl.model.Laboratorio;
import com.sgl.model.Usuario;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.VinculoEstagioAtividade;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.repository.EstagiarioRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.repository.VinculoEstagioAtividadeRepository;
import com.sgl.repository.VinculoEstagioRepository;
import com.sgl.tenant.TenantContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstagiarioService {

	private final EstagiarioRepository estagiarioRepository;
	private final UsuarioRepository usuarioRepository;
	private final LaboratorioRepository laboratorioRepository;
	private final VinculoEstagioRepository vinculoEstagioRepository;
	private final VinculoEstagioAtividadeRepository vinculoEstagioAtividadeRepository;
	private final VinculoEstagioAtividadeService vinculoEstagioAtividadeService;

	@PersistenceContext
	private EntityManager entityManager;

	@Transactional
	public EstagiarioResponseDTO criar(EstagiarioRequestDTO dto) {

		Usuario usuario = buscarUsuario(dto.getUsuarioId());
		Laboratorio laboratorio = buscarLaboratorio(dto.getLaboratorioId());
		Usuario orientador = buscarEValidarOrientador(dto.getOrientadorId());

		validarTenantUnidade(usuario.getUnidade() != null ? usuario.getUnidade().getPublicId() : null);

		validarTenantUnidade(laboratorio.getUnidade() != null ? laboratorio.getUnidade().getPublicId() : null);

		if (estagiarioRepository.existsById(usuario.getId())) {
			throw new BusinessRuleException("Usuário já possui cadastro de estagiário. "
					+ "Novos períodos devem ser registrados como novo vínculo de estágio.");
		}

		usuario.validateInternProfile();
		usuario.validateActive();

		validarDatas(dto.getDataInicioEstagio(), dto.getDataFimEstagio());

		validarUnidadeCompativel(usuario, laboratorio);
		validarMesmaUnidade(usuario, orientador);

		usuario.setLaboratorio(laboratorio);
		usuarioRepository.save(usuario);

		/*
		 * Compatibilidade temporária: enquanto os campos legados ainda existirem em
		 * estagiarios, mantemos o primeiro vínculo também nessa tabela.
		 */
		entityManager.createNativeQuery("""
				INSERT INTO estagiarios (
				    id,
				    data_inicio_estagio,
				    data_fim_estagio,
				    tipo_bolsa,
				    observacao,
				    situacao_estagio,
				    orientador_id
				)
				VALUES (
				    :id,
				    :dataInicio,
				    :dataFim,
				    :tipoBolsa,
				    :observacao,
				    :situacao,
				    :orientadorId
				)
				""").setParameter("id", usuario.getId()).setParameter("dataInicio", dto.getDataInicioEstagio())
				.setParameter("dataFim", dto.getDataFimEstagio()).setParameter("tipoBolsa", dto.getTipoBolsa().name())
				.setParameter("observacao", dto.getObservacao())
				.setParameter("situacao", SituacaoEstagio.EM_ANDAMENTO.name())
				.setParameter("orientadorId", orientador.getId()).executeUpdate();

		entityManager.clear();

		Estagiario estagiario = estagiarioRepository.findById(usuario.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Estagiário recém-criado", usuario.getId()));

		VinculoEstagio vinculo = new VinculoEstagio();

		vinculo.setEstagiario(estagiario);
		vinculo.setOrientador(orientador);

		vinculo.setDataInicio(dto.getDataInicioEstagio());
		vinculo.setDataFimPrevista(dto.getDataFimEstagio());
		vinculo.setDataFimEfetiva(null);

		vinculo.setTipoBolsa(dto.getTipoBolsa());
		vinculo.setSituacao(SituacaoEstagio.EM_ANDAMENTO);
		vinculo.setObservacao(dto.getObservacao());

		vinculo = vinculoEstagioRepository.save(vinculo);

		VinculoEstagioAtividadeRequestDTO participacaoDto = new VinculoEstagioAtividadeRequestDTO();

		participacaoDto.setAtividadeId(dto.getAtividadeId());

		participacaoDto.setDataInicioParticipacao(dto.getDataInicioEstagio());

		participacaoDto.setObservacao(dto.getObservacao());

		vinculoEstagioAtividadeService.adicionar(vinculo.getPublicId(), participacaoDto);

		return montarResponse(estagiario);
	}

	@Transactional(readOnly = true)
	public List<EstagiarioResponseDTO> listarTodos() {
		// Correção de segurança: sem tenant definido, este método caía num
		// "findAll" que devolvia estagiários de todas as unidades. Agora
		// exigimos o header X-SGL-Unidade-Id também para listar.
		exigirTenantAtivo();

		List<Estagiario> estagiarios = estagiarioRepository
				.findByUnidadePublicId(TenantContext.unidadeAtual().orElseThrow());

		return estagiarios.stream().map(this::montarResponse).toList();
	}

	@Transactional(readOnly = true)
	public EstagiarioResponseDTO buscarPorId(UUID id) {
		return montarResponse(buscarEstagiarioNoTenant(id));
	}

	@Transactional(readOnly = true)
	public List<EstagiarioResponseDTO> listarPorLaboratorio(UUID id) {
		Laboratorio laboratorio = buscarLaboratorio(id);
		validarTenantUnidade(laboratorio.getUnidade() != null ? laboratorio.getUnidade().getPublicId() : null);

		return estagiarioRepository.findByLaboratorioId(laboratorio.getId()).stream().map(this::montarResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<EstagiarioResponseDTO> listarAtivos() {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		List<Estagiario> estagiarios = estagiarioRepository.findEstagiariosComVinculoAtivo(unidadeId);

		return estagiarios.stream().map(this::montarResponse).toList();
	}

	@Transactional
	public EstagiarioResponseDTO atualizar(UUID id, EstagiarioRequestDTO dto) {

		buscarEstagiarioNoTenant(id);

		throw new BusinessRuleException(
				"A atualização direta do estágio foi substituída " + "pelo gerenciamento de vínculos institucionais.");
	}

	@Transactional
	public void deletar(UUID id) {
		buscarEstagiarioNoTenant(id);

		throw new BusinessRuleException(
				"Estagiários não podem ser excluídos diretamente. " + "O histórico institucional deve ser preservado.");
	}

	private Estagiario buscarEstagiarioNoTenant(UUID id) {
		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();
		return estagiarioRepository.findByPublicIdAndUnidadePublicId(id, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Estagiário", id));
	}

	private Usuario buscarUsuario(UUID uuid) {
		// Mesma correção de segurança aplicada à busca de usuário.
		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();
		return usuarioRepository.findByPublicIdAndUnidadePublicId(uuid, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Usuário", uuid));
	}

	private Laboratorio buscarLaboratorio(UUID uuid) {
		return laboratorioRepository.findByPublicId(uuid)
				.orElseThrow(() -> new ResourceNotFoundException("Laboratório", uuid));
	}

	private void validarTenantUnidade(UUID unidadeId) {
		if (!TenantContext.pertence(unidadeId)) {
			throw new BusinessRuleException("A operação não pode acessar dados de outra unidade.");
		}
	}

	/**
	 * Garante que existe uma unidade (tenant) definida para a requisição atual. Ver
	 * o mesmo método em EstoqueCentralService para a explicação completa do porquê
	 * essa checagem existe (correção do "modo sem tenant" que vazava dados entre
	 * unidades).
	 */
	private void exigirTenantAtivo() {
		if (!TenantContext.ativo()) {
			throw new BusinessRuleException("Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.");
		}
	}

	private void validarUnidadeCompativel(Usuario usuario, Laboratorio laboratorio) {
		if (usuario.getUnidade() == null || laboratorio.getUnidade() == null
				|| !usuario.getUnidade().getId().equals(laboratorio.getUnidade().getId())) {
			throw new BusinessRuleException("O estagiário e o laboratório devem pertencer à mesma unidade.");
		}
	}

	private void preencherEstagiario(Estagiario estagiario, EstagiarioRequestDTO dto) {
		validarDatas(dto.getDataInicioEstagio(), dto.getDataFimEstagio());

		estagiario.setDataInicioEstagio(dto.getDataInicioEstagio());
		estagiario.setDataFimEstagio(dto.getDataFimEstagio());
		estagiario.setTipoBolsa(dto.getTipoBolsa());
		estagiario.setObservacao(dto.getObservacao());
	}

	private void validarDatas(LocalDate dataInicio, LocalDate dataFim) {
		if (dataFim != null && dataFim.isBefore(dataInicio)) {
			throw new BusinessRuleException("Data de fim do estágio não pode ser menor que data de início.");
		}
	}

	@Transactional
	public EstagiarioResponseDTO encerrarEstagio(UUID id) {

		buscarEstagiarioNoTenant(id);

		throw new BusinessRuleException("O encerramento direto do estágio foi substituído "
				+ "pelo fluxo de encerramento do vínculo institucional.");
	}

	private Usuario buscarEValidarOrientador(UUID orientadorId) {

		Usuario orientador = buscarUsuario(orientadorId);

		orientador.validateActive();

		if (orientador.getPerfil() != Perfil.ANALISTA && orientador.getPerfil() != Perfil.PESQUISADOR) {

			throw new BusinessRuleException("Orientador deve possuir perfil ANALISTA ou PESQUISADOR.");
		}

		return orientador;
	}

	private void validarMesmaUnidade(Usuario estagiario, Usuario orientador) {

		if (estagiario.getUnidade() == null || orientador.getUnidade() == null
				|| !estagiario.getUnidade().getId().equals(orientador.getUnidade().getId())) {

			throw new BusinessRuleException("Estagiário e orientador devem pertencer à mesma unidade.");
		}
	}

	private EstagiarioResponseDTO montarResponse(Estagiario estagiario) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		List<VinculoEstagio> vinculos = vinculoEstagioRepository
				.findByEstagiarioPublicIdAndEstagiarioUnidadePublicIdOrderByDataInicioDesc(estagiario.getPublicId(),
						unidadeId);

		EstagiarioResponseDTO response = new EstagiarioResponseDTO(estagiario, vinculos);

		List<VinculoEstagioResponseDTO> vinculosResponse = vinculos.stream().map(vinculo -> {

			List<VinculoEstagioAtividade> participacoes = vinculoEstagioAtividadeRepository
					.findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataInicioParticipacaoDesc(
							vinculo.getPublicId(), unidadeId);

			return new VinculoEstagioResponseDTO(vinculo, participacoes);
		}).toList();

		response.setVinculos(vinculosResponse);

		boolean possuiVinculoOperacional = vinculosResponse.stream()
				.anyMatch(vinculo -> vinculo.getSituacao() != SituacaoEstagio.FINALIZADO
						&& vinculo.getParticipacoesAtividade() != null
						&& vinculo.getParticipacoesAtividade().stream()
								.anyMatch(participacao -> Boolean.TRUE.equals(participacao.getAtiva())));

		response.setAtivo(Boolean.TRUE.equals(estagiario.getAtivo()) && possuiVinculoOperacional);

		return response;
	}

}
