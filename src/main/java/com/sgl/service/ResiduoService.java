package com.sgl.service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.AdministrarResiduoRequestDTO;
import com.sgl.dto.request.AnalisarResiduoRequestDTO;
import com.sgl.dto.request.ArmazenarResiduoRequestDTO;
import com.sgl.dto.request.ComponenteResiduoRequestDTO;
import com.sgl.dto.request.CriarResiduoRequestDTO;
import com.sgl.dto.request.DespacharResiduoRequestDTO;
import com.sgl.dto.request.ReceberResiduoRequestDTO;
import com.sgl.dto.response.HistoricoResiduoResponseDTO;
import com.sgl.dto.response.ResiduoResponseDTO;
import com.sgl.dto.response.RotuloResiduoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.ClasseResiduo;
import com.sgl.model.ComponenteResiduo;
import com.sgl.model.HistoricoResiduo;
import com.sgl.model.Laboratorio;
import com.sgl.model.LocalArmazenamentoResiduo;
import com.sgl.model.Produto;
import com.sgl.model.Projeto;
import com.sgl.model.Residuo;
import com.sgl.model.Usuario;
import com.sgl.model.enums.AcaoAdministrativaResiduo;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.StatusResiduo;
import com.sgl.repository.ClasseResiduoRepository;
import com.sgl.repository.HistoricoResiduoRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.LocalArmazenamentoResiduoRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.ResiduoRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResiduoService {

	private final ResiduoRepository residuoRepository;
	private final HistoricoResiduoRepository historicoResiduoRepository;
	private final UsuarioRepository usuarioRepository;
	private final LaboratorioRepository laboratorioRepository;
	private final ProjetoRepository projetoRepository;
	private final ProdutoRepository produtoRepository;
	private final ClasseResiduoRepository classeResiduoRepository;
	private final LocalArmazenamentoResiduoRepository localArmazenamentoResiduoRepository;

	@Transactional
	public ResiduoResponseDTO criar(CriarResiduoRequestDTO dto) {
		Usuario gerador = buscarUsuario(dto.getUsuarioGeradorId());
		gerador.validateActive();

		Laboratorio laboratorio = buscarLaboratorio(dto.getLaboratorioId());
		laboratorio.validateActive();
		validarGeradorNoLaboratorio(gerador, laboratorio);

		Projeto projeto = buscarProjeto(dto.getProjetoId(), laboratorio);

		Residuo residuo = Residuo.builder().laboratorio(laboratorio).gerador(gerador).projeto(projeto)
				.unidadeIdSnapshot(laboratorio.getUnidade().getPublicId())
				.unidadeNomeSnapshot(laboratorio.getUnidade().getNome())
				.unidadeSiglaSnapshot(laboratorio.getUnidade().getSigla()).descricao(dto.getDescricao())
				.processoOrigem(dto.getProcessoOrigem()).estadoFisico(dto.getEstadoFisico())
				.recipiente(dto.getRecipiente()).quantidade(dto.getQuantidade()).unidadeMedida(dto.getUnidadeMedida())
				.nivelRiscoInformado(dto.getNivelRiscoInformado())
				.riscosInformados(new LinkedHashSet<>(dto.getRiscosInformados()))
				.observacaoGerador(dto.getObservacaoGerador()).status(StatusResiduo.INFORMADO)
				.dataInformacao(LocalDateTime.now()).build();

		residuo.definirTratamento(dto.getTratamentoRealizado(), dto.getDescricaoTratamento());
		residuo.definirClassesInformadas(
				buscarClassesAtivasDaUnidade(dto.getClassesInformadasIds(), laboratorio.getUnidade().getPublicId()));

		// Snapshot das medidas de segurança informadas pelo solicitante
		residuo.definirSegurancaInformada(dto.getMedidasSegurancaInformadas(), dto.getObservacaoSegurancaInformada());

		dto.getComponentes().forEach(item -> residuo.addComponente(criarComponente(item)));

		Residuo salvo = residuoRepository.save(residuo);
		assegurarIdentificacaoRotulo(salvo);
		salvo = residuoRepository.save(salvo);

		registrarHistorico(salvo, gerador, "RESIDUO_INFORMADO", dto.getObservacaoGerador());

		return new ResiduoResponseDTO(salvo);
	}

	@Transactional
	public ResiduoResponseDTO receber(UUID id, ReceberResiduoRequestDTO dto) {
		Residuo residuo = buscarEntidade(id);
		Usuario gestor = buscarUsuarioGestao(dto.getUsuarioGestorId());

		residuo.receber(gestor, dto.getObservacao());
		Residuo salvo = residuoRepository.save(residuo);
		registrarHistorico(salvo, gestor, "RECEBIDO_PELA_GESTAO", dto.getObservacao());

		return new ResiduoResponseDTO(salvo);
	}

	@Transactional
	public ResiduoResponseDTO analisarELiberar(UUID id, AnalisarResiduoRequestDTO dto) {
		Residuo residuo = buscarEntidade(id);
		Usuario gestor = buscarUsuarioGestao(dto.getUsuarioGestorId());

		// 1. Busca e valida as classes escolhidas pela Gestão
		List<ClasseResiduo> classesConfirmadas = buscarClassesAtivasDaUnidade(dto.getClassesConfirmadasIds(),
				residuo.getLaboratorio().getUnidade().getPublicId());

		LocalArmazenamentoResiduo localArmazenamento = buscarLocalArmazenamentoAtivo(
				dto.getLocalArmazenamentoResiduoId(), residuo.getLaboratorio().getUnidade().getPublicId());

		// 2. Executa a análise/liberação
		residuo.liberarParaArmazenamento(gestor, dto.getNivelRiscoConfirmado(), dto.getRiscosConfirmados(),

				localArmazenamento, dto.getComplementoLocalArmazenamento(), dto.getLocalArmazenamentoTemporario(),

				dto.getDestinoFinalPrevisto(), dto.getDataPrevistaDespacho(), dto.getObservacaoGestor());

		// 3. Registra o snapshot das classes confirmadas e a segurança
		residuo.definirClassesConfirmadas(classesConfirmadas);
		residuo.definirSegurancaConfirmada(dto.getMedidasSegurancaConfirmadas(),
				dto.getObservacaoSegurancaConfirmada());

		assegurarIdentificacaoRotulo(residuo);

		Residuo salvo = residuoRepository.save(residuo);
		registrarHistorico(salvo, gestor, "RISCO_CONFERIDO_E_RESIDUO_LIBERADO", dto.getObservacaoGestor());

		return new ResiduoResponseDTO(salvo);
	}

	@Transactional
	public ResiduoResponseDTO confirmarArmazenamento(UUID id, ArmazenarResiduoRequestDTO dto) {

		Residuo residuo = buscarEntidade(id);

		Usuario gestor = buscarUsuarioGestao(dto.getUsuarioGestorId());

		String localPlanejado = residuo.getLocalArmazenamentoTemporario();

		LocalArmazenamentoResiduo localArmazenamento = buscarLocalArmazenamentoAtivo(
				dto.getLocalArmazenamentoResiduoId(), residuo.getLaboratorio().getUnidade().getPublicId());

		residuo.confirmarArmazenamento(localArmazenamento, dto.getComplementoLocalArmazenamento(),
				dto.getLocalArmazenamentoTemporario());

		Residuo salvo = residuoRepository.save(residuo);

		String localConfirmado = salvo.getLocalArmazenamentoTemporario();

		boolean localCorrigido = !Objects.equals(localPlanejado, localConfirmado);

		String acao = localCorrigido ? "ARMAZENAMENTO_TEMPORARIO_CORRIGIDO" : "ARMAZENAMENTO_TEMPORARIO_CONFIRMADO";

		String observacaoHistorico;

		if (localCorrigido) {

			observacaoHistorico = "Local planejado: " + localPlanejado + " | Local confirmado: " + localConfirmado;

		} else {

			observacaoHistorico = "Local confirmado: " + localConfirmado;
		}

		registrarHistorico(salvo, gestor, acao, observacaoHistorico);

		return new ResiduoResponseDTO(salvo);
	}

	@Transactional
	public ResiduoResponseDTO despachar(UUID id, DespacharResiduoRequestDTO dto) {
		Residuo residuo = buscarEntidade(id);
		Usuario gestor = buscarUsuarioGestao(dto.getUsuarioGestorId());

		residuo.confirmarDespacho(dto.getDestinoFinalConfirmado(), dto.getObservacao());

		Residuo salvo = residuoRepository.save(residuo);
		registrarHistorico(salvo, gestor, "DESPACHO_CONFIRMADO", dto.getDestinoFinalConfirmado());

		return new ResiduoResponseDTO(salvo);
	}

	@Transactional
	public ResiduoResponseDTO administrarCiclo(UUID id, AdministrarResiduoRequestDTO dto) {

		if (dto == null || dto.getAcao() == null) {
			throw new BusinessRuleException("A ação administrativa é obrigatória.");
		}

		if (dto.getJustificativa() == null || dto.getJustificativa().isBlank()) {
			throw new BusinessRuleException("A justificativa da ação administrativa é obrigatória.");
		}

		Residuo residuo = buscarEntidade(id);
		Usuario administrador = buscarUsuarioAdministrador(dto.getUsuarioAdministradorId());
		String justificativa = dto.getJustificativa().trim();

		if (dto.getAcao() == AcaoAdministrativaResiduo.CANCELAR) {
			residuo.cancelarAdministrativamente();

			Residuo salvo = residuoRepository.save(residuo);
			registrarHistorico(
					salvo,
					administrador,
					"RESIDUO_CANCELADO_ADMINISTRATIVAMENTE",
					limitarObservacaoHistorico("Justificativa: " + justificativa)
			);

			return new ResiduoResponseDTO(salvo);
		}

		StatusResiduo etapaAnterior = residuo.getStatus();
		StatusResiduo novaEtapa = residuo.retornarEtapaAdministrativamente();

		Residuo salvo = residuoRepository.save(residuo);

		String observacao =
				"Retorno administrativo de "
				+ etapaAnterior
				+ " para "
				+ novaEtapa
				+ ". Justificativa: "
				+ justificativa;

		registrarHistorico(
				salvo,
				administrador,
				"RETORNO_ADMINISTRATIVO_DE_ETAPA",
				limitarObservacaoHistorico(observacao)
		);

		return new ResiduoResponseDTO(salvo);
	}

	@Transactional(readOnly = true)
	public ResiduoResponseDTO buscarPorId(UUID id) {
		return new ResiduoResponseDTO(buscarEntidade(id));
	}

	@Transactional(readOnly = true)
	public List<ResiduoResponseDTO> listarTodos() {
		// Correção de segurança: sem tenant ativo, caía num "findAll" que
		// devolvia resíduos de todas as unidades. Agora o header
		// X-SGL-Unidade-Id é exigido também para listar.
		exigirTenantAtivo();

		List<Residuo> residuos = residuoRepository
				.findByLaboratorioUnidadePublicIdOrderByDataInformacaoDesc(TenantContext.unidadeAtual().orElseThrow());

		return residuos.stream().map(ResiduoResponseDTO::new).toList();
	}

	@Transactional(readOnly = true)
	public List<ResiduoResponseDTO> listarPorStatus(StatusResiduo status) {
		exigirTenantAtivo();

		List<Residuo> residuos = residuoRepository.findByLaboratorioUnidadePublicIdAndStatusOrderByDataInformacaoDesc(
				TenantContext.unidadeAtual().orElseThrow(), status);

		return residuos.stream().map(ResiduoResponseDTO::new).toList();
	}

	@Transactional(readOnly = true)
	public List<ResiduoResponseDTO> listarPorLaboratorio(UUID laboratorioId) {
		Laboratorio laboratorio = buscarLaboratorio(laboratorioId);

		return residuoRepository.findByLaboratorioPublicIdOrderByDataInformacaoDesc(laboratorio.getPublicId()).stream()
				.map(ResiduoResponseDTO::new).toList();
	}

	@Transactional(readOnly = true)
	public List<ResiduoResponseDTO> listarPorGerador(UUID usuarioGeradorId) {
		Usuario gerador = buscarUsuario(usuarioGeradorId);
		gerador.validateActive();

		return residuoRepository.findByGeradorPublicIdOrderByDataInformacaoDesc(usuarioGeradorId).stream()
				.map(ResiduoResponseDTO::new).toList();
	}

	@Transactional(readOnly = true)
	public List<HistoricoResiduoResponseDTO> buscarHistorico(UUID id) {
		Residuo residuo = buscarEntidade(id);
		return historicoResiduoRepository.findByResiduoIdOrderByDataHoraAsc(residuo.getId()).stream()
				.map(HistoricoResiduoResponseDTO::new).toList();
	}

	@Transactional
	public RotuloResiduoResponseDTO gerarDadosRotulo(UUID id) {
		Residuo residuo = buscarEntidade(id);

		// Compatibilidade com Resíduos antigos criados antes da Etapa 3.3.
		if (assegurarIdentificacaoRotulo(residuo)) {
			residuo = residuoRepository.save(residuo);
		}

		return new RotuloResiduoResponseDTO(residuo);
	}

	private Residuo buscarEntidade(UUID id) {
		// Correção de segurança: antes, sem tenant ativo, buscava sem
		// filtro de unidade (findByPublicId), vazando o resíduo de outra
		// unidade para quem não enviasse o header.
		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();
		return residuoRepository.findByPublicIdAndLaboratorioUnidadePublicId(id, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Resíduo", id));
	}

	private Usuario buscarUsuario(UUID id) {
		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();
		return usuarioRepository.findByPublicIdAndUnidadePublicId(id, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
	}

	private Laboratorio buscarLaboratorio(UUID id) {
		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();
		return laboratorioRepository.findByPublicIdAndUnidadePublicId(id, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Laboratório", id));
	}

	private Usuario buscarUsuarioGestao(UUID id) {
		Usuario usuario = buscarUsuario(id);
		usuario.validateActive();

		if (usuario.getPerfil() != Perfil.GESTOR && usuario.getPerfil() != Perfil.ADMINISTRADOR) {
			throw new BusinessRuleException("A operação de gestão de resíduos exige perfil GESTOR ou ADMINISTRADOR.");
		}

		return usuario;
	}

	private Usuario buscarUsuarioAdministrador(UUID id) {
		Usuario usuario = buscarUsuario(id);
		usuario.validateActive();

		if (usuario.getPerfil() != Perfil.ADMINISTRADOR) {
			throw new BusinessRuleException(
					"A correção administrativa do ciclo de resíduos exige perfil ADMINISTRADOR.");
		}

		return usuario;
	}

	private Projeto buscarProjeto(UUID projetoId, Laboratorio laboratorio) {
		if (projetoId == null) {
			return null;
		}

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();
		Projeto projeto = projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(projetoId, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Projeto", projetoId));
		projeto.validateActive();

		if (!projeto.getLaboratorio().getId().equals(laboratorio.getId())) {
			throw new BusinessRuleException("O projeto informado não pertence ao laboratório gerador do resíduo.");
		}

		return projeto;
	}

	private void validarGeradorNoLaboratorio(Usuario gerador, Laboratorio laboratorio) {
		if (gerador.getLaboratorio() != null && !gerador.getLaboratorio().getId().equals(laboratorio.getId())) {
			throw new BusinessRuleException("O usuário gerador não pertence ao laboratório informado.");
		}
	}

	private List<ClasseResiduo> buscarClassesAtivasDaUnidade(Set<UUID> ids, UUID unidadeId) {

		if (ids == null || ids.isEmpty()) {
			throw new BusinessRuleException("Informe pelo menos uma classe de resíduo.");
		}

		List<ClasseResiduo> classes = classeResiduoRepository.findByPublicIdInAndUnidadePublicId(ids, unidadeId);

		if (classes.size() != ids.size()) {
			throw new BusinessRuleException("Uma ou mais classes de resíduo são inválidas para esta unidade.");
		}

		for (ClasseResiduo classe : classes) {
			classe.validateActive();
		}

		return classes;
	}

	private ComponenteResiduo criarComponente(ComponenteResiduoRequestDTO dto) {
		Produto produto = null;
		String nomeComponente = dto.getNomeComponente();

		if (dto.getProdutoId() != null) {
			produto = produtoRepository.findByPublicId(dto.getProdutoId())
					.orElseThrow(() -> new ResourceNotFoundException("Produto", dto.getProdutoId()));

			// Correção de bug: antes, sem tenant ativo, o ".orElse(false)"
			// fazia essa checagem ser simplesmente ignorada, permitindo
			// registrar um componente com produto de qualquer unidade.
			// Como a criação de resíduo sempre acontece dentro de um
			// laboratório (que já exige tenant via buscarLaboratorio acima),
			// aqui também exigimos o tenant explicitamente.
			exigirTenantAtivo();
			if (!produtoRepository.pertenceAUnidade(dto.getProdutoId(), TenantContext.unidadeAtual().orElseThrow())) {
				throw new ResourceNotFoundException("Produto", dto.getProdutoId());
			}

			if (nomeComponente == null || nomeComponente.isBlank()) {
				nomeComponente = produto.getNome();
			}
		}

		if (nomeComponente == null || nomeComponente.isBlank()) {
			throw new BusinessRuleException("O componente do resíduo deve possuir nome ou referência de produto.");
		}

		return ComponenteResiduo.builder().produto(produto).nomeComponente(nomeComponente)
				.principal(Boolean.TRUE.equals(dto.getPrincipal()))
				.concentracaoOuQuantidade(dto.getConcentracaoOuQuantidade()).observacao(dto.getObservacao()).build();
	}

	/**
	 * Garante a identificação necessária para a prévia do rótulo.
	 *
	 * Código e QR pertencem à identidade do Resíduo e não representam, por si só,
	 * autorização para impressão física.
	 */
	private boolean assegurarIdentificacaoRotulo(Residuo residuo) {
		boolean alterado = false;

		if (residuo.getCodigoRastreio() == null) {
			residuo.setCodigoRastreio(gerarCodigoRastreio(residuo));
			alterado = true;
		}

		if (residuo.getQrCodeConteudo() == null) {
			residuo.setQrCodeConteudo("SGL-RESIDUO:" + residuo.getPublicId());
			alterado = true;
		}

		return alterado;
	}

	/**
	 * Garante que existe uma unidade (tenant) definida para a requisição atual. Ver
	 * o mesmo método em EstoqueCentralService para a explicação completa do porquê
	 * essa checagem existe.
	 */
	private void exigirTenantAtivo() {
		if (!TenantContext.ativo()) {
			throw new BusinessRuleException("Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.");
		}
	}

	private String gerarCodigoRastreio(Residuo residuo) {
		int ano = residuo.getDataInformacao().getYear();
		return String.format("SGL-RES-%d-%06d", ano, residuo.getId());
	}

	private void registrarHistorico(Residuo residuo, Usuario usuario, String acao, String observacao) {

		historicoResiduoRepository.save(HistoricoResiduo.builder().residuo(residuo).usuario(usuario)
				.status(residuo.getStatus()).acao(acao).observacao(observacao).dataHora(LocalDateTime.now()).build());
	}

	private String limitarObservacaoHistorico(String observacao) {
		if (observacao == null || observacao.length() <= 1000) {
			return observacao;
		}

		return observacao.substring(0, 1000);
	}

	private LocalArmazenamentoResiduo buscarLocalArmazenamentoAtivo(UUID localId, UUID unidadeId) {

		if (localId == null) {
			return null;
		}

		exigirTenantAtivo();

		if (!TenantContext.pertence(unidadeId)) {
			throw new BusinessRuleException("A operação não pode acessar dados de outra unidade.");
		}

		LocalArmazenamentoResiduo local = localArmazenamentoResiduoRepository
				.findByPublicIdAndUnidadePublicId(localId, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Local de armazenamento de resíduo", localId));

		local.validateActive();

		return local;
	}

}
