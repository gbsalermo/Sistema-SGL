package com.sgl.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.CorrecaoCodigoSegRequestDTO;
import com.sgl.dto.response.HistoricoCorrecaoCodigoSegResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Atividade;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.Usuario;
import com.sgl.model.codigoseg.HistoricoCorrecaoCodigoSeg;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.TipoAlvoCodigoSeg;
import com.sgl.repository.AtividadeRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.SciRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.repository.codigoseg.HistoricoCorrecaoCodigoSegRepository;
import com.sgl.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CorrecaoCodigoSegService {

	private final ProjetoRepository projetoRepository;
	private final SciRepository sciRepository;
	private final AtividadeRepository atividadeRepository;
	private final UsuarioRepository usuarioRepository;
	private final HistoricoCorrecaoCodigoSegRepository historicoRepository;
	private final CodigoSegValidator codigoSegValidator;

	@Transactional
	public List<HistoricoCorrecaoCodigoSegResponseDTO> corrigirProjeto(
			UUID projetoId,
			CorrecaoCodigoSegRequestDTO dto) {

		Projeto projeto = buscarProjeto(projetoId);
		Usuario usuario = buscarUsuarioOperador(dto.getUsuarioId());
		String justificativa = normalizarJustificativa(dto.getJustificativa());

		String codigoAnterior = exigirCodigoExistente(
				projeto.getCodigoSeg(),
				"O Projeto ainda não possui Código SEG. A primeira definição deve ser realizada pelo fluxo comum de atualização."
		);

		String novoCodigoProjeto =
				codigoSegValidator.validarProjeto(dto.getNovoCodigoSeg());

		validarCodigoDiferente(codigoAnterior, novoCodigoProjeto);
		validarProjetoUnico(novoCodigoProjeto, projeto.getPublicId());

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		List<Sci> scis =
				sciRepository.findByProjetoPublicIdAndProjetoLaboratorioUnidadePublicId(
						projeto.getPublicId(),
						unidadeId
				);

		List<Atividade> atividades =
				atividadeRepository.findBySciProjetoPublicIdAndSciProjetoLaboratorioUnidadePublicId(
						projeto.getPublicId(),
						unidadeId
				);

		Map<UUID, String> novosCodigosSci = new HashMap<>();
		Map<UUID, String> novosCodigosAtividade = new HashMap<>();

		String novaRaizProjeto = raizSemUltimoSegmento(novoCodigoProjeto);

		for (Sci sci : scis) {

			String novoCodigoSci =
					novaRaizProjeto + "." + ultimoSegmento(sci.getCodigoSeg());

			novoCodigoSci = codigoSegValidator.validarSci(
					novoCodigoSci,
					novoCodigoProjeto
			);

			validarSciUnico(novoCodigoSci, sci.getPublicId());
			novosCodigosSci.put(sci.getPublicId(), novoCodigoSci);
		}

		for (Atividade atividade : atividades) {

			String novoCodigoSci =
					novosCodigosSci.get(atividade.getSci().getPublicId());

			if (novoCodigoSci == null) {
				throw new BusinessRuleException(
						"Não foi possível determinar o novo Código SEG do SCI pai da Atividade."
				);
			}

			String novoCodigoAtividade =
					novoCodigoSci + "." + ultimoSegmento(atividade.getCodigoSeg());

			novoCodigoAtividade =
					codigoSegValidator.validarAtividade(
							novoCodigoAtividade,
							novoCodigoSci
					);

			validarAtividadeUnica(
					novoCodigoAtividade,
					atividade.getPublicId()
			);

			novosCodigosAtividade.put(
					atividade.getPublicId(),
					novoCodigoAtividade
			);
		}

		List<HistoricoCorrecaoCodigoSeg> historicos = new ArrayList<>();

		historicos.add(
				criarHistoricoProjeto(
						projeto,
						usuario,
						codigoAnterior,
						novoCodigoProjeto,
						justificativa
				)
		);

		for (Sci sci : scis) {

			String novoCodigo = novosCodigosSci.get(sci.getPublicId());

			historicos.add(
					criarHistoricoSci(
							sci,
							usuario,
							sci.getCodigoSeg(),
							novoCodigo,
							justificativa
					)
			);

			sci.setCodigoSeg(novoCodigo);
		}

		for (Atividade atividade : atividades) {

			String novoCodigo =
					novosCodigosAtividade.get(atividade.getPublicId());

			historicos.add(
					criarHistoricoAtividade(
							atividade,
							usuario,
							atividade.getCodigoSeg(),
							novoCodigo,
							justificativa
					)
			);

			atividade.setCodigoSeg(novoCodigo);
		}

		projeto.setCodigoSeg(novoCodigoProjeto);

		projetoRepository.save(projeto);
		sciRepository.saveAll(scis);
		atividadeRepository.saveAll(atividades);

		return historicoRepository.saveAll(historicos)
				.stream()
				.map(HistoricoCorrecaoCodigoSegResponseDTO::new)
				.toList();
	}

	@Transactional
	public List<HistoricoCorrecaoCodigoSegResponseDTO> corrigirSci(
			UUID sciId,
			CorrecaoCodigoSegRequestDTO dto) {

		Sci sci = buscarSci(sciId);
		Projeto projeto = sci.getProjeto();

		Usuario usuario = buscarUsuarioOperador(dto.getUsuarioId());
		String justificativa = normalizarJustificativa(dto.getJustificativa());

		String codigoAnterior = exigirCodigoExistente(
				sci.getCodigoSeg(),
				"O SCI ainda não possui Código SEG."
		);

		String novoCodigoSci = codigoSegValidator.validarSci(
				dto.getNovoCodigoSeg(),
				projeto
		);

		validarCodigoDiferente(codigoAnterior, novoCodigoSci);
		validarSciUnico(novoCodigoSci, sci.getPublicId());

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		List<Atividade> atividades =
				atividadeRepository.findBySciPublicIdAndSciProjetoLaboratorioUnidadePublicId(
						sci.getPublicId(),
						unidadeId
				);

		Map<UUID, String> novosCodigosAtividade = new HashMap<>();

		for (Atividade atividade : atividades) {

			String novoCodigoAtividade =
					novoCodigoSci + "." + ultimoSegmento(atividade.getCodigoSeg());

			novoCodigoAtividade =
					codigoSegValidator.validarAtividade(
							novoCodigoAtividade,
							novoCodigoSci
					);

			validarAtividadeUnica(
					novoCodigoAtividade,
					atividade.getPublicId()
			);

			novosCodigosAtividade.put(
					atividade.getPublicId(),
					novoCodigoAtividade
			);
		}

		List<HistoricoCorrecaoCodigoSeg> historicos = new ArrayList<>();

		historicos.add(
				criarHistoricoSci(
						sci,
						usuario,
						codigoAnterior,
						novoCodigoSci,
						justificativa
				)
		);

		for (Atividade atividade : atividades) {

			String novoCodigo =
					novosCodigosAtividade.get(atividade.getPublicId());

			historicos.add(
					criarHistoricoAtividade(
							atividade,
							usuario,
							atividade.getCodigoSeg(),
							novoCodigo,
							justificativa
					)
			);

			atividade.setCodigoSeg(novoCodigo);
		}

		sci.setCodigoSeg(novoCodigoSci);

		sciRepository.save(sci);
		atividadeRepository.saveAll(atividades);

		return historicoRepository.saveAll(historicos)
				.stream()
				.map(HistoricoCorrecaoCodigoSegResponseDTO::new)
				.toList();
	}

	@Transactional
	public List<HistoricoCorrecaoCodigoSegResponseDTO> corrigirAtividade(
			UUID atividadeId,
			CorrecaoCodigoSegRequestDTO dto) {

		Atividade atividade = buscarAtividade(atividadeId);
		Usuario usuario = buscarUsuarioOperador(dto.getUsuarioId());

		String codigoAnterior = exigirCodigoExistente(
				atividade.getCodigoSeg(),
				"A Atividade ainda não possui Código SEG."
		);

		String novoCodigo =
				codigoSegValidator.validarAtividade(
						dto.getNovoCodigoSeg(),
						atividade.getSci()
				);

		validarCodigoDiferente(codigoAnterior, novoCodigo);
		validarAtividadeUnica(novoCodigo, atividade.getPublicId());

		String justificativa = normalizarJustificativa(dto.getJustificativa());

		HistoricoCorrecaoCodigoSeg historico =
				criarHistoricoAtividade(
						atividade,
						usuario,
						codigoAnterior,
						novoCodigo,
						justificativa
				);

		atividade.setCodigoSeg(novoCodigo);
		atividadeRepository.save(atividade);

		HistoricoCorrecaoCodigoSeg salvo =
				historicoRepository.save(historico);

		return List.of(
				new HistoricoCorrecaoCodigoSegResponseDTO(salvo)
		);
	}

	@Transactional(readOnly = true)
	public List<HistoricoCorrecaoCodigoSegResponseDTO> listarHistoricoProjeto(
			UUID projetoId) {

		Projeto projeto = buscarProjeto(projetoId);
		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return historicoRepository
				.findByProjetoPublicIdAndProjetoLaboratorioUnidadePublicIdOrderByDataHoraAsc(
						projeto.getPublicId(),
						unidadeId
				)
				.stream()
				.map(HistoricoCorrecaoCodigoSegResponseDTO::new)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<HistoricoCorrecaoCodigoSegResponseDTO> listarHistoricoSci(
			UUID sciId) {

		Sci sci = buscarSci(sciId);
		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return historicoRepository
				.findBySciPublicIdAndSciProjetoLaboratorioUnidadePublicIdOrderByDataHoraAsc(
						sci.getPublicId(),
						unidadeId
				)
				.stream()
				.map(HistoricoCorrecaoCodigoSegResponseDTO::new)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<HistoricoCorrecaoCodigoSegResponseDTO> listarHistoricoAtividade(
			UUID atividadeId) {

		Atividade atividade = buscarAtividade(atividadeId);
		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return historicoRepository
				.findByAtividadePublicIdAndAtividadeSciProjetoLaboratorioUnidadePublicIdOrderByDataHoraAsc(
						atividade.getPublicId(),
						unidadeId
				)
				.stream()
				.map(HistoricoCorrecaoCodigoSegResponseDTO::new)
				.toList();
	}

	private Projeto buscarProjeto(UUID projetoId) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return projetoRepository
				.findByPublicIdAndLaboratorioUnidadePublicId(
						projetoId,
						unidadeId
				)
				.orElseThrow(
						() -> new ResourceNotFoundException(
								"Projeto",
								projetoId
						)
				);
	}

	private Sci buscarSci(UUID sciId) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return sciRepository
				.findByPublicIdAndProjetoLaboratorioUnidadePublicId(
						sciId,
						unidadeId
				)
				.orElseThrow(
						() -> new ResourceNotFoundException(
								"SCI",
								sciId
						)
				);
	}

	private Atividade buscarAtividade(UUID atividadeId) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return atividadeRepository
				.findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(
						atividadeId,
						unidadeId
				)
				.orElseThrow(
						() -> new ResourceNotFoundException(
								"Atividade",
								atividadeId
						)
				);
	}

	private Usuario buscarUsuarioOperador(UUID usuarioId) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		Usuario usuario = usuarioRepository
				.findByPublicIdAndUnidadePublicId(
						usuarioId,
						unidadeId
				)
				.orElseThrow(
						() -> new ResourceNotFoundException(
								"Usuário",
								usuarioId
						)
				);

		usuario.validateActive();

		if (usuario.getPerfil() != Perfil.GESTOR
				&& usuario.getPerfil() != Perfil.ADMINISTRADOR) {

			throw new BusinessRuleException(
					"A correção de Código SEG exige perfil GESTOR ou ADMINISTRADOR."
			);
		}

		return usuario;
	}

	private void validarProjetoUnico(String codigoSeg, UUID projetoAtualId) {

		if (projetoRepository.existsByCodigoSegAndPublicIdNot(
				codigoSeg,
				projetoAtualId
		)) {
			throw new BusinessRuleException(
					"Já existe um Projeto com este Código SEG."
			);
		}
	}

	private void validarSciUnico(String codigoSeg, UUID sciAtualId) {

		if (sciRepository.existsByCodigoSegAndPublicIdNot(
				codigoSeg,
				sciAtualId
		)) {
			throw new BusinessRuleException(
					"Já existe um SCI com este Código SEG."
			);
		}
	}

	private void validarAtividadeUnica(
			String codigoSeg,
			UUID atividadeAtualId) {

		if (atividadeRepository.existsByCodigoSegAndPublicIdNot(
				codigoSeg,
				atividadeAtualId
		)) {
			throw new BusinessRuleException(
					"Já existe uma Atividade com este Código SEG."
			);
		}
	}

	private String exigirCodigoExistente(
			String codigoSeg,
			String mensagem) {

		if (codigoSeg == null || codigoSeg.isBlank()) {
			throw new BusinessRuleException(mensagem);
		}

		return codigoSeg.trim();
	}

	private void validarCodigoDiferente(
			String codigoAtual,
			String novoCodigo) {

		if (codigoAtual.equals(novoCodigo)) {
			throw new BusinessRuleException(
					"O novo Código SEG deve ser diferente do código atual."
			);
		}
	}

	private String normalizarJustificativa(String justificativa) {

		if (justificativa == null || justificativa.isBlank()) {
			throw new BusinessRuleException(
					"A justificativa da correção é obrigatória."
			);
		}

		String normalizada = justificativa.trim();

		if (normalizada.length() > 1000) {
			throw new BusinessRuleException(
					"A justificativa deve possuir no máximo 1000 caracteres."
			);
		}

		return normalizada;
	}

	private String raizSemUltimoSegmento(String codigoSeg) {

		int ultimoPonto = codigoSeg.lastIndexOf('.');

		if (ultimoPonto <= 0) {
			throw new BusinessRuleException(
					"Código SEG inválido para correção hierárquica."
			);
		}

		return codigoSeg.substring(0, ultimoPonto);
	}

	private String ultimoSegmento(String codigoSeg) {

		if (codigoSeg == null || codigoSeg.isBlank()) {
			throw new BusinessRuleException(
					"Foi encontrado um descendente sem Código SEG durante a correção hierárquica."
			);
		}

		String normalizado = codigoSeg.trim();
		int ultimoPonto = normalizado.lastIndexOf('.');

		if (ultimoPonto < 0 || ultimoPonto == normalizado.length() - 1) {
			throw new BusinessRuleException(
					"Foi encontrado um Código SEG descendente inválido durante a correção hierárquica."
			);
		}

		return normalizado.substring(ultimoPonto + 1);
	}

	private HistoricoCorrecaoCodigoSeg criarHistoricoProjeto(
			Projeto projeto,
			Usuario usuario,
			String codigoAnterior,
			String codigoNovo,
			String justificativa) {

		return HistoricoCorrecaoCodigoSeg.builder()
				.tipoAlvo(TipoAlvoCodigoSeg.PROJETO)
				.projeto(projeto)
				.usuario(usuario)
				.codigoAnterior(codigoAnterior)
				.codigoNovo(codigoNovo)
				.justificativa(justificativa)
				.build();
	}

	private HistoricoCorrecaoCodigoSeg criarHistoricoSci(
			Sci sci,
			Usuario usuario,
			String codigoAnterior,
			String codigoNovo,
			String justificativa) {

		return HistoricoCorrecaoCodigoSeg.builder()
				.tipoAlvo(TipoAlvoCodigoSeg.SCI)
				.sci(sci)
				.usuario(usuario)
				.codigoAnterior(codigoAnterior)
				.codigoNovo(codigoNovo)
				.justificativa(justificativa)
				.build();
	}

	private HistoricoCorrecaoCodigoSeg criarHistoricoAtividade(
			Atividade atividade,
			Usuario usuario,
			String codigoAnterior,
			String codigoNovo,
			String justificativa) {

		return HistoricoCorrecaoCodigoSeg.builder()
				.tipoAlvo(TipoAlvoCodigoSeg.ATIVIDADE)
				.atividade(atividade)
				.usuario(usuario)
				.codigoAnterior(codigoAnterior)
				.codigoNovo(codigoNovo)
				.justificativa(justificativa)
				.build();
	}

	private void exigirTenantAtivo() {

		if (!TenantContext.ativo()) {
			throw new BusinessRuleException(
					"Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação."
			);
		}
	}
}
