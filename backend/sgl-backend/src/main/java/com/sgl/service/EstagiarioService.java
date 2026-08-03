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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstagiarioService {

	private final EstagiarioRepository estagiarioRepository;
	private final UsuarioRepository usuarioRepository;
	private final LaboratorioRepository laboratorioRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Transactional
	public EstagiarioDTO criar(EstagiarioDTO dto) {
	    Usuario usuario = buscarUsuario(dto.getUsuarioId());
	    Laboratorio laboratorio = buscarLaboratorio(dto.getLaboratorioId());

	    if (estagiarioRepository.existsById(usuario.getId())) {
	        throw new IllegalArgumentException("Usuário já possui cadastro de estagiário.");
	    }

	    validarPerfilEstagiario(usuario);
	    validarDatas(dto.getDataInicioEstagio(), dto.getDataFimEstagio());

	    usuario.setLaboratorio(laboratorio); // se aplicável
	    usuarioRepository.save(usuario);

	    // INSERT direto na subtabela — evita o merge problemático
	    entityManager.createNativeQuery(
	        "INSERT INTO estagiario (id, data_inicio_estagio, data_fim_estagio, tipo_bolsa, observacao) " +
	        "VALUES (:id, :dataInicio, :dataFim, :tipoBolsa, :observacao)")
	        .setParameter("id", usuario.getId())
	        .setParameter("dataInicio", dto.getDataInicioEstagio())
	        .setParameter("dataFim", dto.getDataFimEstagio())
	        .setParameter("tipoBolsa", dto.getTipoBolsa().name())
	        .setParameter("observacao", dto.getObservacao())
	        .executeUpdate();

	    entityManager.clear(); // limpa o contexto pra recarregar como Estagiario "de verdade"

	    Estagiario salvo = estagiarioRepository.findById(usuario.getId())
	        .orElseThrow(() -> new EntityNotFoundException("Erro ao criar estagiário"));
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

		if (!id.equals(dto.getUsuarioId())) {
			throw new IllegalArgumentException("Não é permitido trocar o usuário vinculado do estagiário.");
		}

		Laboratorio laboratorio = buscarLaboratorio(dto.getLaboratorioId());
		validarPerfilEstagiario(estagiario);

		estagiario.setPerfil(Perfil.ESTAGIARIO);
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
		
		//Caso não seja informada a data de encessamento o sistema atualiza automaticamente
		if (estagiario.getDataFimEstagio() == null) {
		    estagiario.setDataFimEstagio(LocalDate.now());
		}
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
		estagiario.setObservacao(dto.getObservacao());
		estagiario.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
	}

	private void validarDatas(LocalDate dataInicio, LocalDate dataFim) {
		if (dataFim != null && dataFim.isBefore(dataInicio)) {
			throw new IllegalArgumentException("Data de fim do estágio não pode ser menor que data de início.");
		}
	}
	
	//Encerrar Estagio
	@Transactional
	public EstagiarioDTO encerrarEstagio(Long id) {

	    Estagiario estagiario = estagiarioRepository.findById(id)
	            .orElseThrow(() ->
	                    new EntityNotFoundException(
	                            "Estagiário não encontrado com id: " + id));

	    if (!estagiario.getAtivo()) {
	        throw new IllegalArgumentException(
	                "O estágio já está encerrado.");
	    }

	    estagiario.setAtivo(false);

	    if (estagiario.getDataFimEstagio() == null) {
	        estagiario.setDataFimEstagio(LocalDate.now());
	    }

	    Estagiario atualizado = estagiarioRepository.save(estagiario);

	    return new EstagiarioDTO(atualizado);
	}

}
