package com.sgl.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.EstagiarioDTO;
import com.sgl.model.Estagiario;
import com.sgl.model.Laboratorio;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.repository.EstagiarioRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UsuarioRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstagiarioService {

	private final EstagiarioRepository estagiarioRepository;
	private final UsuarioRepository usuarioRepository;
	private final LaboratorioRepository laboratorioRepository;

	@Transactional
	public EstagiarioDTO criar(EstagiarioDTO dto) {
		Usuario usuario = buscarUsuario(dto.getUsuarioId());
		Laboratorio laboratorio = buscarLaboratorio(dto.getLaboratorioId());

		if (estagiarioRepository.existsByUsuarioId(usuario.getId())) {
			throw new IllegalArgumentException("Usuário já possui cadastro de estagiário.");
		}

		validarPerfilEstagiario(usuario);

		Estagiario estagiario = Estagiario.builder()
				.usuario(usuario)
				.laboratorio(laboratorio)
				.build();

		preencherEstagiario(estagiario, dto);

		Estagiario salvo = estagiarioRepository.save(estagiario);
		return new EstagiarioDTO(salvo);
	}

	@Transactional(readOnly = true)
	public List<EstagiarioDTO> listarTodos() {
		return estagiarioRepository.findAll()
				.stream()
				.map(EstagiarioDTO::new)
				.toList();
	}

	@Transactional(readOnly = true)
	public EstagiarioDTO buscarPorId(Long id) {
		return estagiarioRepository.findById(id)
				.map(EstagiarioDTO::new)
				.orElseThrow(() -> new EntityNotFoundException("Estagiário não encontrado com id: " + id));
	}

	@Transactional(readOnly = true)
	public List<EstagiarioDTO> listarPorLaboratorio(Long laboratorioId) {
		return estagiarioRepository.findByLaboratorioId(laboratorioId)
				.stream()
				.map(EstagiarioDTO::new)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<EstagiarioDTO> listarAtivos() {
		return estagiarioRepository.findByAtivoTrue()
				.stream()
				.map(EstagiarioDTO::new)
				.toList();
	}

	@Transactional
	public EstagiarioDTO atualizar(Long id, EstagiarioDTO dto) {
		Estagiario estagiario = estagiarioRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Estagiário não encontrado com id: " + id));

		Usuario usuario = buscarUsuario(dto.getUsuarioId());
		Laboratorio laboratorio = buscarLaboratorio(dto.getLaboratorioId());

		if (estagiarioRepository.existsByUsuarioIdAndIdNot(usuario.getId(), id)) {
			throw new IllegalArgumentException("Usuário já está vinculado a outro cadastro de estagiário.");
		}

		validarPerfilEstagiario(usuario);

		estagiario.setUsuario(usuario);
		estagiario.setLaboratorio(laboratorio);
		preencherEstagiario(estagiario, dto);

		Estagiario atualizado = estagiarioRepository.save(estagiario);
		return new EstagiarioDTO(atualizado);
	}

	@Transactional
	public void deletar(Long id) {
		Estagiario estagiario = estagiarioRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Estagiário não encontrado com id: " + id));
		estagiario.setAtivo(false);
	}

	private Usuario buscarUsuario(Long usuarioId) {
		return usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com id: " + usuarioId));
	}

	private Laboratorio buscarLaboratorio(Long laboratorioId) {
		return laboratorioRepository.findById(laboratorioId)
				.orElseThrow(() -> new EntityNotFoundException("Laboratório não encontrado com id: " + laboratorioId));
	}

	private void validarPerfilEstagiario(Usuario usuario) {
		if (usuario.getPerfil() != Perfil.ESTAGIARIO) {
			throw new IllegalArgumentException("Usuário deve ter perfil ESTAGIARIO para cadastro de estagiário.");
		}
	}

	private void preencherEstagiario(Estagiario estagiario, EstagiarioDTO dto) {
		validarDatas(dto.getDataInicioEstagio(), dto.getDataFimEstagio());

		estagiario.setDataInicioEstagio(dto.getDataInicioEstagio());
		estagiario.setDataFimEstagio(dto.getDataFimEstagio());
		estagiario.setTipoBolsa(dto.getTipoBolsa());
		estagiario.setFuncao(dto.getFuncao());
		estagiario.setObservacao(dto.getObservacao());
		estagiario.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
	}

	private void validarDatas(LocalDate dataInicio, LocalDate dataFim) {
		if (dataFim != null && dataFim.isBefore(dataInicio)) {
			throw new IllegalArgumentException("Data de fim do estágio não pode ser menor que data de início.");
		}
	}
}
